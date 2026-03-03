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
import java.time.Instant.now
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

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

    init {
        launchHeartbeatJob()
    }

    fun updateHeartbeat(intervalMs: Long) {
        lastServerHeartbeat.set(now())

        if (heartbeatInterval.get() == null) {
            heartbeatInterval.set(ofMillis(intervalMs))
        }
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
    }

    suspend fun send(grpcResponse: ServiceToGateProto.Builder) {
        val contentHidden = grpcResponse.getHeadersOrDefault(CONTENT_HIDDEN_HEADER, "false").toBoolean()

        check(!state.notStarted && !state.shutdown) { "$this: can't send message in state $state" }

        grpcMutex.withLock {
            storage.setSequenceNumberAndStoreMessage(grpcResponse)

            val grpcResponse = grpcResponse
                .setRunningInstanceId(runningInstanceId)
                .build()

            if (grpcResponse.hasHeartBeat()) {
                logger.trace("ServiceToGateProto: heartbeat")
            } else {
                logProto(grpcResponse, prompt = "ServiceToGate", noContentLogging = contentHidden)
            }

            stream.onNext(grpcResponse)
        }
    }

    suspend fun resend(grpcResponse: ServiceToGateProto.Builder) {
        val contentHidden = grpcResponse.getHeadersOrDefault(CONTENT_HIDDEN_HEADER, "false").toBoolean()

        val grpcResponse = grpcResponse
            .setRunningInstanceId(runningInstanceId)
            .build()

        grpcMutex.withLock {
            logProto(grpcResponse, prompt = "ServiceToGate", noContentLogging = contentHidden)
            stream.onNext(grpcResponse)
        }
    }

    override fun onNext(request: GateToServiceProto) {
        processor.process(request)
    }

    override fun onError(e: Throwable) {
        if (e is StatusRuntimeException && e.status == Status.UNAVAILABLE) {
            // shutdown method has been called
            return
        }
        logger.error("$this: RECEIVED error ${e.message}", e)
        state.shuttingDown()

        gracefulShutdownManagedChannel()
    }

    override fun onCompleted() {
        state.shuttingDown()
        logger.info("$this: RECEIVED completed")

        gracefulShutdownManagedChannel()
    }

    suspend fun gracefulShutdown() {
        if (state.isShutdownTypeState())
            return

        connector.logConnecting("{}: graceful shutting down grpc channel ...", this)
        state.shuttingDown()

        if (!this::stream.isInitialized) {
            connector.logConnecting("{}: ... stream is not initialized, skipping stream completion ...", this)
            return gracefulShutdownManagedChannel()
        }

        runCatching {
            send(stopServingProto)
            connector.logConnecting("{}: sent stopServing to gate, waiting for stopServing from gate ...", this)

            withTimeout(config.shutdownConfig.actionConnectorMs) {
                while (!state.shutdown) {
                    delay(100)
                }
            }
        }.onFailure { logger.error("$this: can't send stop serving, continue shutdown ...", it) }

        if (!state.shutdown) {
            shutdownNow()
        }
    }

    fun gracefulShutdownFromGate(reason: String? = null) {
        state.shuttingDown()

        scope.launch {
            gracefulShutdownPrivate()
            state.shutdownReason = reason
        }
    }

    private suspend fun gracefulShutdownPrivate() {
        connector.logConnecting("{}: completing stream to {} ...", this, connector.targetUrl)

        grpcMutex.withLock {
            stream.onCompleted()
        }

        gracefulShutdownManagedChannel()
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

        connector.logConnecting("{}: completing stream to {} ...", this, connector.targetUrl)

        grpcMutex.withLock {
            stream.onCompleted()
        }

        shutdownNowManagedChannel()
    }

    private suspend fun sendStartServingProto() {
        logger.info("Connector $connectorId: sending start serving to ${connector.targetUrl} ...")
        runCatching {
            send(
                ServiceToGateProto.newBuilder()
                    .setStartServing(
                        StartServingProto.newBuilder()
                            .setRunningInstanceSequenceNumber(storage.lastSentSequenceNumber)
                            .setConnectionToken(pool.token)
                            .setHostname(context.environment["HOSTNAME"] ?: "localhost")
                            .setVersion(SDK_VERSION)
                            .setImage(context.environment["IMAGE_NAME"] ?: "")
                            .setServiceDescriptor(executor.action.getDescriptor())
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

            val timeoutSeconds = 10L
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

        while (!state.shutdown) {
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

        private val stopServingProto: ServiceToGateProto.Builder =
            ServiceToGateProto.newBuilder().setStopServing(StopServingProto.getDefaultInstance())

        private val heartbeatProto: ServiceToGateProto.Builder =
            ServiceToGateProto.newBuilder().setHeartBeat(HeartBeatProto.getDefaultInstance())
    }
}
