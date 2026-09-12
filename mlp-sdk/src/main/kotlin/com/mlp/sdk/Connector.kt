package com.mlp.sdk

import com.mlp.gate.ServiceInfoProto
import com.mlp.gate.ServiceToGateProto
import com.mlp.sdk.State.Condition.ACTIVE
import java.time.Duration.ofMillis
import java.time.Instant.now
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class Connector(
    @Volatile
    var targetUrl: String,
    val pool: ConnectorsPool,
    val executor: TaskExecutor,
    val config: MlpServiceConfig,
    val scope: CoroutineScope,
    override val context: MlpExecutionContext,
) : WithExecutionContext, WithState(ACTIVE) {

    var serviceInfo: ServiceInfoProto? = null

    val connectorId = lastConnectorId.getAndIncrement()
    private var gatewayPermanentlyUnavailable = true

    private val keepConnectionJob: Job = launchKeepConnectionJob()

    private val grpcChannelRef = AtomicReference<GrpcChannel?>(null)

    val grpcChannel: GrpcChannel?
        get() = runCatching { grpcChannelRef.get() }.getOrNull()

    private val storage = ServiceToGateMessageStorage()
    private val processor = GateToServiceMessageProcessor(this, storage)
    private val buffer = OutgoingMessageBuffer.create(config)

    suspend fun gracefulShutdown() {
        if (state.isShutdownTypeState()) {
            return
        }

        state.shuttingDown()
        logger.debug("{}: graceful shutting down ...", this)

        runCatching { keepConnectionJob.cancelAndJoin() }
            .onFailure { logger.error("$this: error while keep connection job cancelling", it) }

        grpcChannel?.gracefulShutdown()
        buffer.clear()

        state.shutdown()
        logger.debug("{}: ... has been successfully shutdown", this)
    }

    internal suspend fun shutdown() {
        if (state.isShutdownTypeState()) {
            return
        }

        state.shuttingDown()
        logger.debug("{}: force shutting down ...", this)

        runCatching { keepConnectionJob.cancelAndJoin() }
            .onFailure { logger.error("$this: error while keep connection job cancelling", it) }

        grpcChannel?.shutdownNow()
        buffer.clear()

        state.shutdown()
        logger.debug("{}: ... has been successfully shutdown", this)
    }

    private fun launchKeepConnectionJob() = scope.launch {
        logger.debug("{}: keep connection job is started ...", this@Connector)

        var lastActiveTime = now()
        var progressiveDelay = 100L
        runCatching {
            while (isActive) {
                throttleIfUnknownConnectionToken()

                if (isGrpcChannelShutDownOrNull()) {
                    val connected = tryConnectOrShutdown()
                    if (connected) {
                        lastActiveTime = now()
                        if (progressiveDelay != 100L) {
                            logger.debug("{}: reset progressiveDelay to 100 because channel was connected", this@Connector)
                        }
                        progressiveDelay = 100L
                        gatewayPermanentlyUnavailable = false
                    } else {
                        progressiveDelay = min(progressiveDelay * 2, 10_000L)
                        if (progressiveDelay == 10_000L) gatewayPermanentlyUnavailable = true
                        logConnecting("{}: increase progressiveDelay to {}", this@Connector, progressiveDelay)
                    }
                }

                if (isGrpcChannelActive()) {
                    lastActiveTime = now()
                    progressiveDelay = 100L
                    gatewayPermanentlyUnavailable = false

                    if (!executor.canProcessNewJobs(connectorId)) {
                        logger.error("${this@Connector}: grpc channel is active, but can not process new jobs")
                    }
                }

                if (now() > lastActiveTime + ofMillis(config.grpcConnectTimeoutMs)) {
                    tryGrpcShutdown()
                }

                delay(progressiveDelay)
            }
            logger.debug("{}: ... keep connection job is stopped because scope is not active", this@Connector)
        }.onFailure {
            if (state.isShutdownTypeState() && it is CancellationException)
                return@onFailure
            else
                logger.error("${this@Connector}: error while keep connection job running", it)
        }

        logger.debug("{}: ... keep connection job has been stopped", this@Connector)
    }

    private suspend fun throttleIfUnknownConnectionToken() {
        if (grpcChannel?.state?.shutdownReason == "instance_by_token_not_found") {
            delay(1_000L)
        }
    }

    private suspend fun tryGrpcShutdown() {
        // Канал в состоянии SHUTTING_DOWN уже останавливается по своему сценарию
        // (stopServing -> дренаж -> half-close) и со своим бюджетом. Добивать его здесь
        // значит рвать активные запросы ровно в тот момент, ради которого дренаж и заведён.
        if (grpcChannel?.state?.shuttingDown == true) {
            logger.debug("{}: grpc channel is shutting down gracefully, not forcing shutdown", this@Connector)
            return
        }

        runCatching {
            grpcChannelRef.getAndSet(null)
                ?.shutdownNow()
        }.onFailure {
            logger.error("${this@Connector}: cannot shutdown grpc channel", it)
        }
    }

    private suspend fun tryConnectOrShutdown(): Boolean {
        val newGrpcChannel = GrpcChannel(this, storage, processor, context)

        return runCatching {
            newGrpcChannel.tryConnect()

            val oldChannel = grpcChannelRef.getAndSet(newGrpcChannel)
            if (oldChannel != null) {
                logger.info(
                    "$this: reconnected; tearing down previous grpc channel " +
                            "(old state=${oldChannel.state}). In-flight tasks are kept running."
                )
                oldChannel.shutdownNow()
            } else {
                logger.info("$this: connected to grpc channel for the first time")
            }

            drainBufferTo(newGrpcChannel)
            true
        }.onFailure {
            logger.trace("{}: cannot create new grpc channel", this@Connector, it)
            newGrpcChannel.shutdownNow()
        }.getOrDefault(false)
    }

    private suspend fun drainBufferTo(channel: GrpcChannel) {
        val pending = buffer.size()
        if (pending == 0) {
            logger.debug("$this: outgoing buffer is empty, nothing to drain to new channel")
            return
        }

        logger.info("$this: draining $pending buffered message(s) to the newly connected channel")
        var sent = 0
        runCatching {
            buffer.drainTo { msg ->
                channel.send(msg)
                sent++
            }
        }.onFailure {
            logger.error(
                "$this: failed to drain outgoing buffer to new channel " +
                        "(sent=$sent of $pending, remaining=${buffer.size()}); remaining messages " +
                        "stay in the buffer and will be retried on the next reconnect (until TTL).",
                it
            )
            return
        }

        logger.info(
            "$this: drained $sent message(s) to new channel; " +
                    "${buffer.size()} message(s) left in buffer (likely evicted by TTL)"
        )
    }

    /**
     * Единственная точка выхода сообщений коннектора — здесь же снимается учёт открытого стрима.
     * Снимаем после попытки отправки (в finally), а не до неё: иначе между «стрим закрыт» и
     * реальной отправкой финального кадра успел бы пройти дренаж и half-close.
     */
    suspend fun sendServiceToGate(grpcResponse: ServiceToGateProto.Builder) {
        try {
            deliverServiceToGate(grpcResponse)
        } finally {
            if (grpcResponse.isStreamTerminal()) {
                executor.streamFinished(connectorId, grpcResponse.requestId)
            } else if (grpcResponse.hasPartialPredict() && grpcResponse.requestId != 0L) {
                executor.streamTouched(connectorId, grpcResponse.requestId)
            }
        }
    }

    /**
     * Кадр закрывает стрим запроса: финальный partialPredict, обычный ответ predict или ошибка.
     * startPartialPredict (partialPredict со start=true и finish=false) стрим как раз открывает,
     * поэтому терминальным не считается.
     */
    private fun ServiceToGateProto.Builder.isStreamTerminal(): Boolean = when {
        requestId == 0L -> false
        hasPartialPredict() -> partialPredict.finish
        hasPredict() -> true
        hasError() -> true
        else -> false
    }

    private suspend fun deliverServiceToGate(grpcResponse: ServiceToGateProto.Builder) {
        val channel = grpcChannel
        // SHUTTING_DOWN — рабочее состояние для ответов: во время дренажа остановки канал ещё жив,
        // и ответ должен уйти в гейт, а не осесть в буфере реконнекта, которого уже не будет.
        if (channel != null && channel.isAvailableToSend()) {
            try {
                channel.send(grpcResponse)
                return
            } catch (e: Throwable) {
                logger.warn(
                    "$this: send through active grpc channel failed, falling back to outgoing buffer " +
                            "(buffer size before enqueue=${buffer.size()}, channel state=${channel.state}): ${e.message}"
                )
            }
        } else {
            logger.info(
                "$this: no active grpc channel (channel=${channel?.let { "state=${it.state}" } ?: "null"}), " +
                        "enqueueing message into outgoing buffer for delivery after reconnect " +
                        "(buffer size before enqueue=${buffer.size()})"
            )
        }

        try {
            buffer.enqueue(grpcResponse)
        } catch (e: OutgoingMessageBufferFullException) {
            // Buffer is full — caller gets back-pressure. The buffer logs overflow details itself.
            logger.error(
                "$this: outgoing buffer is full, dropping message and propagating the error to caller. " +
                        "This means the gate has been unreachable for too long or producer outpaces drain.",
                e
            )
            throw e
        }
    }

    internal fun logConnecting(message: String, vararg args: Any) {
        if (gatewayPermanentlyUnavailable) return
        logger.debug(message, *args)
    }

    internal fun isGrpcChannelActive(): Boolean =
        grpcChannel?.state?.active == true

    internal fun isAvailableToSendGrpc(): Boolean =
        grpcChannel?.isAvailableToSend() == true

    internal fun isGrpcChannelShutDownOrNull(): Boolean =
        grpcChannel == null || grpcChannel?.state?.shutdown == true

    override fun toString() = "Connector(id='$connectorId', url='$targetUrl')"

    companion object {
        private val lastConnectorId = AtomicLong()
    }
}
