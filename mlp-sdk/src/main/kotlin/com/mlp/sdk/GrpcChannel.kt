package com.mlp.sdk

import com.mlp.gate.GateGrpc
import com.mlp.gate.GateToServiceProto
import com.mlp.gate.HeartBeatProto
import com.mlp.gate.ServiceToGateProto
import com.mlp.gate.StartServingProto
import com.mlp.gate.StopServingProto
import com.mlp.sdk.utils.CONTENT_HIDDEN_HEADER
import com.mlp.sdk.utils.logProto
import com.mlp.sdk.utils.runningInstanceId
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import java.io.File
import java.time.Duration
import java.time.Duration.between
import java.time.Duration.ofMillis
import java.time.Instant
import java.time.Instant.now
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GrpcChannel(
    private val connector: Connector,
    private val storage: ServiceToGateMessageStorage,
    private val processor: GateToServiceMessageProcessor,
    override val context: MlpExecutionContext,
) : StreamObserver<GateToServiceProto>, WithState(logsEnabled = false) {

    private val connectorId: Long = connector.connectorId
    private val executor: TaskExecutor = connector.executor
    private val scope: CoroutineScope = connector.scope
    private val config: MlpServiceConfig = connector.config
    private val pool: ConnectorsPool = connector.pool

    private lateinit var managedChannel: ManagedChannel
    private lateinit var stream: StreamObserver<ServiceToGateProto>

    private val lastServerHeartbeat = AtomicReference(now())
    private val heartbeatInterval = AtomicReference<Duration>(null)

    private val grpcMutex = Mutex()

    /** Half-close делаем ровно один раз, кто бы ни начал остановку — мы или гейт. */
    private val streamCompleted = AtomicBoolean(false)
    private val shutdownMutex = Mutex()

    /** Гейт закрыл свою половину стрима: дальше ждать завершения запросов бессмысленно. */
    @Volatile
    private var gateStreamClosed = false

    init {
        launchHeartbeatJob()
    }

    override fun onNext(request: GateToServiceProto) {
        processor.process(request)
    }

    override fun onError(e: Throwable) {
        gateStreamClosed = true
        if (e is StatusRuntimeException && e.status == Status.UNAVAILABLE) {
            // shutdown method has been called
            return
        }
        logger.error("$this: RECEIVED error ${e.message}", e)
        state.shuttingDown()

        gracefulShutdownManagedChannel()
    }

    override fun onCompleted() {
        gateStreamClosed = true
        state.shuttingDown()
        logger.info("$this: RECEIVED completed")

        gracefulShutdownManagedChannel()
    }

    suspend fun tryConnect() {
        check(state.notStarted) { "Connector $connectorId: GrpcChannel can connect only once" }
        connector.logConnecting("Connector $connectorId: opening grpc channel to ${connector.targetUrl} ...")
        state.starting()

        val channelBuilder = ManagedChannelBuilder
            .forTarget(connector.targetUrl)
            .maxInboundMessageSize(Int.MAX_VALUE)

        if (!config.grpcSecure) {
            channelBuilder.usePlaintext()
        }
        managedChannel = channelBuilder.build()

        val healthCheck = GateGrpc.newBlockingStub(managedChannel)
            .healthCheck(HeartBeatProto.getDefaultInstance())
        if (healthCheck.status != "Ok") {
            gracefulShutdownManagedChannel()
            return
        }

        stream = GateGrpc.newStub(managedChannel)
            .processAsync(this)

        sendStartServingProto()
        executor.initContainer(connectorId)

        setActiveState()
    }

    suspend fun send(grpcResponse: ServiceToGateProto.Builder) {
        val contentHidden = grpcResponse.getHeadersOrDefault(CONTENT_HIDDEN_HEADER, "false").toBoolean()

        check(!state.notStarted && !state.shutdown) { "$this: can't send message in state $state" }

        grpcMutex.withLock {
            check(!streamCompleted.get()) { "$this: can't send message, stream is already half-closed" }

            val built = grpcResponse.build()

            if (built.hasHeartBeat()) {
                logger.trace("ServiceToGateProto: heartbeat")
            } else {
                logProto(built, prompt = "ServiceToGate", noContentLogging = contentHidden)
            }

            stream.onNext(built)
        }
    }

    suspend fun resend(grpcResponse: ServiceToGateProto.Builder) {
        val contentHidden = grpcResponse.getHeadersOrDefault(CONTENT_HIDDEN_HEADER, "false").toBoolean()

        val grpcResponse = grpcResponse
//            .setRunningInstanceId(runningInstanceId)
            .build()

        grpcMutex.withLock {
            logProto(grpcResponse, prompt = "ServiceToGate", noContentLogging = contentHidden)
            stream.onNext(grpcResponse)
        }
    }

    /** Канал может отправлять: активен или доживает остановку (дренаж до half-close). */
    fun isAvailableToSend(): Boolean =
        (state.active || state.shuttingDown) && !streamCompleted.get()

    fun updateHeartbeat(intervalMs: Long) {
        lastServerHeartbeat.set(now())

        if (heartbeatInterval.get() == null) {
            heartbeatInterval.set(ofMillis(intervalMs))
        }
    }

    /**
     * Остановка по нашей инициативе (SIGTERM). Порядок обязателен и держится на контракте гейта:
     * по нашему stopServing гейт перестаёт маршрутизировать новые запросы, а соединение добивает
     * только по half-close. Поэтому сначала stopServing, потом дренаж активных запросов в пределах
     * MLP_GRACEFUL_SHUTDOWN_CONNECTOR_MS и лишь затем half-close.
     * Ответного stopServing от гейта не ждём: ждать надо задачи, а не ack.
     */
    suspend fun gracefulShutdown() {
        if (state.shutdown)
            return

        val deadline = now() + ofMillis(config.shutdownConfig.actionConnectorMs)

        if (state.shuttingDown) {
            // Остановку уже ведёт другой сценарий (stopServing от гейта). Не выходим сразу:
            // иначе пул посчитает коннектор остановленным, и JVM выйдет посреди дренажа.
            connector.logConnecting("{}: shutdown is already in progress, waiting for it to finish ...", this)
            awaitShutdown(deadline.plusSeconds(MANAGED_CHANNEL_SHUTDOWN_TIMEOUT_SEC))
            return
        }

        connector.logConnecting("{}: graceful shutting down grpc channel ...", this)
        state.shuttingDown()

        executor.disableNewJobs(connectorId)

        if (!this::stream.isInitialized) {
            connector.logConnecting("{}: ... stream is not initialized, skipping stream completion ...", this)
            return gracefulShutdownManagedChannel()
        }

        runCatching { send(stopServingProto) }
            .onFailure { logger.error("$this: can't send stop serving, continue shutdown ...", it) }
        connector.logConnecting("{}: sent stopServing to gate, draining in-flight requests ...", this)

        drainAndCompleteStream(deadline)
    }

    /**
     * Остановка по инициативе гейта (его stopServing, рестарт гейта, неизвестный токен).
     * Свою половину стрима закрываем так же — только после дренажа: гейт ждёт наш half-close.
     */
    fun gracefulShutdownFromGate(reason: String? = null) {
        if (state.shutdown)
            return

        state.shuttingDown()
        state.shutdownReason = reason

        val deadline = now() + ofMillis(config.shutdownConfig.actionConnectorMs)
        executor.disableNewJobs(connectorId)

        scope.launch {
            drainAndCompleteStream(deadline)
        }
    }

    /**
     * Общий хвост остановки: дождаться активных запросов коннектора до [deadline],
     * закрыть свою половину стрима и погасить managed channel.
     * Идемпотентен — параллельные остановки (наша и от гейта) не дублируют half-close.
     */
    private suspend fun drainAndCompleteStream(deadline: Instant) {
        shutdownMutex.withLock {
            if (!streamCompleted.get()) {
                executor.gracefulShutdownAll(connectorId, deadline) { gateStreamClosed || state.shutdown }
                completeStreamOnce()
            }
        }

        gracefulShutdownManagedChannel()
    }

    private suspend fun awaitShutdown(until: Instant) {
        while (!state.shutdown && now() < until) {
            delay(50)
        }
    }

    private suspend fun completeStreamOnce() {
        if (!this::stream.isInitialized)
            return
        if (!streamCompleted.compareAndSet(false, true))
            return

        connector.logConnecting("{}: completing stream to {} ...", this, connector.targetUrl)
        grpcMutex.withLock {
            runCatching { stream.onCompleted() }
                .onFailure { logger.warn("$this: can't complete stream: ${it.message}") }
        }
    }

    suspend fun shutdownNow() {
        if (state.isShutdownTypeState()) {
            return
        }

        connector.logConnecting("{}: force shutting down grpc channel ...", this)
        state.shuttingDown()

        if (!this::stream.isInitialized) {
            connector.logConnecting("{}: stream is not initialized, skipping stream completion", this)
            return shutdownNowManagedChannel()
        }

        runCatching { send(stopServingProto) }
            .onFailure { logger.error("$this: can't send stop serving", it) }

        completeStreamOnce()

        shutdownNowManagedChannel()
    }

    private suspend fun sendStartServingProto() {
        logger.info("Connector $connectorId: sending start serving to ${connector.targetUrl} ...")
        runCatching {
            send(
                ServiceToGateProto.newBuilder()
                    .setStartServing(
                        StartServingProto.newBuilder()
//                            .setRunningInstanceSequenceNumber(storage.lastSentSequenceNumber)
                            .setConnectionToken(pool.token)
                            .setHostname(context.environment["HOSTNAME"] ?: "localhost")
                            .setVersion(SDK_VERSION)
                            .setImage(context.environment["IMAGE_NAME"] ?: "")
                            .setServiceDescriptor(executor.action.getDescriptor())
                            .setInstanceBootUuid(MlpServiceSDK.processInstanceUuid)
                            .build()
                    )
            )
        }.onFailure {
            logger.error("Connector $connectorId: error on first start serving to ${connector.targetUrl}", it)
            gracefulShutdownManagedChannel()
        }
    }

    fun setActiveState() {
        logger.info("Connector $connectorId: sending start serving to ${connector.targetUrl} ...")
        runCatching {
            state.active()
        }.onFailure {
            logger.error("Connector $connectorId: error on first start serving to ${connector.targetUrl}", it)
            gracefulShutdownManagedChannel()
        }
    }

    private fun gracefulShutdownManagedChannel(reason: String? = null) {
        connector.logConnecting("{}: graceful shutting down managed channel to {} ...", this, connector.targetUrl)

        try {
            if (!this::managedChannel.isInitialized) {
                connector.logConnecting("{}: managed channel is not initialized, skipping managed channel shutdown", this)
                return
            }

            if (managedChannel.isShutdown) {
                return
            }

            managedChannel.shutdown()
            state.shutdown()

            val timeoutSeconds = MANAGED_CHANNEL_SHUTDOWN_TIMEOUT_SEC
            if (managedChannel.awaitTermination(timeoutSeconds, SECONDS)) {
                return connector.logConnecting("{}: ... managed channel has been successfully shutdown", this)
            }

            connector.logConnecting("{}: ... managed channel has not been shutdown in {} seconds, force shutdown ...", this, timeoutSeconds)
            runCatching { managedChannel.shutdownNow() }
                .onFailure { logger.error("$this: can't force shutdown managed channel", it) }
                .onSuccess { connector.logConnecting("{}: ... managed channel has been successfully shutdown", this) }
        } catch (e: InterruptedException) {
            logger.error("$this: ... managed channel has not been shutdown", e)
        } finally {
            state.shutdown()
        }
    }

    private fun shutdownNowManagedChannel() {
        connector.logConnecting("{}: force shutting down managed channel ...", this)

        try {
            if (!this::managedChannel.isInitialized) {
                connector.logConnecting("{}: managed channel is not initialized, skipping managed channel shutdown", this)
                return
            }

            if (managedChannel.isShutdown) {
                return
            }

            runCatching { managedChannel.shutdownNow() }
                .onFailure { logger.error("$this: can't shutdown managed channel", it) }
                .onSuccess { connector.logConnecting("{}: ... managed channel has been successfully shutdown", this) }
        } finally {
            state.shutdown()
        }
    }

    private fun launchHeartbeatJob() = scope.launch {
        connector.logConnecting("Connector {}: starting heartbeats with interval {} ms", connectorId, heartbeatInterval)

        while (!state.shutdown && !streamCompleted.get()) {
            val interval = heartbeatInterval.get()

            if (interval == null) {
                delay(1000)
                continue
            }

            if (interval.toMillis() < 10)
                logger.error("Too small heartbeat interval")

            runCatching { livenessProbe() }
                .onFailure { logger.error("$this: error on liveness probe", it) }
            runCatching { send(heartbeatProto) }
                .onFailure {
                    if (!state.shutdown) {
                        logger.error("Connector $connectorId: can't send heartbeat", it)
                    }
                }

            delay(interval.toMillis())

            val maxTimeout = interval.multipliedBy(3).plusSeconds(1)
            if (between(lastServerHeartbeat.get(), now()) > maxTimeout) {
                logger.error("Connector $connectorId: no heartbeat for $maxTimeout ms")
                shutdownNow()
            }
        }
    }

    fun livenessProbe() {
        File(LIVENESS_PROBE)
            .writeText((System.currentTimeMillis() / 1000).toString())
    }

    override fun toString() = "GrpcChannel(${connector.targetUrl}) of $connector"

    companion object {
        private const val LIVENESS_PROBE = "/tmp/liveness-probe"
        private const val MANAGED_CHANNEL_SHUTDOWN_TIMEOUT_SEC = 10L

        private val stopServingProto: ServiceToGateProto.Builder =
            ServiceToGateProto.newBuilder().setStopServing(StopServingProto.getDefaultInstance())

        private val heartbeatProto: ServiceToGateProto.Builder =
            ServiceToGateProto.newBuilder().setHeartBeat(HeartBeatProto.getDefaultInstance())
    }
}
