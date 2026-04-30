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

    suspend fun gracefulShutdown() {
        if (state.isShutdownTypeState()) {
            return
        }

        state.shuttingDown()
        logger.debug("{}: graceful shutting down ...", this)

        runCatching { keepConnectionJob.cancelAndJoin() }
            .onFailure { logger.error("$this: error while keep connection job cancelling", it) }

        grpcChannel?.gracefulShutdown()

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
            grpcChannelRef.getAndSet(newGrpcChannel)?.shutdownNow()
            true
        }.onFailure {
            logger.trace("{}: cannot create new grpc channel", this@Connector, it)
            newGrpcChannel.shutdownNow()
        }.getOrDefault(false)
    }

    suspend fun sendServiceToGate(grpcResponse: ServiceToGateProto.Builder) {
        grpcChannel?.send(grpcResponse)
    }

    internal fun logConnecting(message: String, vararg args: Any) {
        if (gatewayPermanentlyUnavailable) return
        logger.debug(message, *args)
    }

    internal fun isGrpcChannelActive(): Boolean =
        grpcChannel?.state?.active == true

    internal fun isAvailableToSendGrpc(): Boolean =
        isGrpcChannelActive() || grpcChannel?.state?.shuttingDown == true

    internal fun isGrpcChannelShutDownOrNull(): Boolean =
        grpcChannel == null || grpcChannel?.state?.shutdown == true

    override fun toString() = "Connector(id='$connectorId', url='$targetUrl')"

    companion object {
        private val lastConnectorId = AtomicLong()
    }
}
