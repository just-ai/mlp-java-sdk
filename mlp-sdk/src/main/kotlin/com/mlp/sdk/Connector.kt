package com.mlp.sdk

import com.mlp.gate.ApiErrorProto
import com.mlp.gate.ClusterUpdateProto
import com.mlp.gate.GateGrpc
import com.mlp.gate.GateToServiceProto
import com.mlp.gate.HeartBeatProto
import com.mlp.gate.ServiceToGateProto
import com.mlp.gate.ServiceToGateProtoOrBuilder
import com.mlp.gate.StartServingProto
import com.mlp.gate.StopServingProto
import com.mlp.gate.SyncSequenceNumbersProto
import com.mlp.sdk.State.Condition.ACTIVE
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
import java.util.UUID
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.TimeUnit.SECONDS
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.slf4j.MDC

const val CALLER_ACCOUNT_ID = "Z-callerAccountId"
const val CONTENT_HIDDEN_HEADER = "Content-Hidden"

class Connector(
    @Volatile
    var targetUrl: String,
    val pool: ConnectorsPool,
    val executor: TaskExecutor,
    val config: MlpServiceConfig,
    val scope: CoroutineScope,
    override val context: MlpExecutionContext,
) : WithExecutionContext, WithState(ACTIVE) {

    val connectorId = lastConnectorId.getAndIncrement()
    private var gatewayPermanentlyUnavailable = true

    private val grpcChannel = AtomicReference<GrpcChannel?>(null)
    private val keepConnectionJob: Job

    private val serviceToGateMessageStorage = ConcurrentSkipListMap<Long, ServiceToGateProto>()

    init {
        keepConnectionJob = launchKeepConnectionJob()
    }

    suspend fun sendServiceToGate(grpcResponse: ServiceToGateProto.Builder) {
        grpcChannel.get()?.send(grpcResponse)
    }

    suspend fun gracefulShutdown() {
        if (state.isShutdownTypeState()) {
            return
        }

        state.shuttingDown()
        logger.debug("{}: graceful shutting down ...", this)

        runCatching { keepConnectionJob.cancelAndJoin() }
            .onFailure { logger.error("$this: error while keep connection job cancelling", it) }

        grpcChannel.get()?.gracefulShutdown()

        state.shutdown()
        logger.debug("{}: ... has been successfully shutdown", this)
    }

    internal fun isConnected() = grpcChannel.get()
        ?.state
        ?.active == true

    fun isAvailableToSendGrpc() = isConnected() || grpcChannel.get()
        ?.state
        ?.shuttingDown == true

    fun shutdownNow() = runBlocking {
        shutdown()
    }

    internal suspend fun shutdown() {
        if (state.isShutdownTypeState()) {
            return
        }

        state.shuttingDown()
        logger.debug("{}: force shutting down ...", this)

        runCatching { runBlocking { keepConnectionJob.cancelAndJoin() } }
            .onFailure { logger.error("$this: error while keep connection job cancelling", it) }

        grpcChannel.get()?.shutdownNow()

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

                if (grpcChannel.isShutdownStateOrNull()) {
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

                if (grpcChannel.isActiveState()) {
                    lastActiveTime = now()
                    progressiveDelay = 100L
                    gatewayPermanentlyUnavailable = false

                    if (!executor.isAbleProcessNewJobs(connectorId)) {
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
        if (grpcChannel.get()?.state?.shutdownReason == "instance_by_token_not_found") {
            delay(1_000L)
        }
    }

    private suspend fun tryGrpcShutdown() {
        runCatching {
            grpcChannel.getAndSet(null)
                ?.shutdownNow()
        }.onFailure {
            logger.error("${this@Connector}: cannot shutdown grpc channel", it)
        }
    }

    private suspend fun tryConnectOrShutdown(): Boolean {
        val newGrpcChannel = GrpcChannel(context)

        return runCatching {
            newGrpcChannel.tryConnect()
            grpcChannel.getAndSet(newGrpcChannel)?.shutdownNow()
            true
        }.onFailure {
            logger.trace("{}: cannot create new grpc channel", this@Connector, it)
            newGrpcChannel.shutdownNow()
        }.getOrDefault(false)
    }

    override fun toString() = "Connector(id='$connectorId', url='$targetUrl')"

    private inner class GrpcChannel(
        override val context: MlpExecutionContext
    ) : StreamObserver<GateToServiceProto>, WithExecutionContext, WithState(logsEnabled = false) {

        private lateinit var managedChannel: ManagedChannel
        private lateinit var stream: StreamObserver<ServiceToGateProto>

        private val lastServerHeartbeat = AtomicReference(now())
        private val heartbeatInterval = AtomicReference<Duration>(null)

        private val grpcMutex = Mutex()

        init {
            launchHeartbeatJob()
        }

        suspend fun tryConnect() {
            check(state.notStarted) { "Connector $connectorId: GrpcChannel can connect only once" }
            logConnecting("Connector $connectorId: opening grpc channel to $targetUrl ...")
            state.starting()

            val channelBuilder = ManagedChannelBuilder
                .forTarget(targetUrl)
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
                val grpcResponse = grpcResponse
                    .setSequenceNumber(if (grpcResponse.isSequenced()) lastSentSequenceNumber.incrementAndGet() else -1)
                    .setRunningInstanceId(runningInstanceId)
                    .build()

                if (grpcResponse.sequenceNumber > 0) {
                    serviceToGateMessageStorage[grpcResponse.sequenceNumber] = grpcResponse
                }

                if (grpcResponse.hasHeartBeat()) {
                    logger.trace("ServiceToGateProto: heartbeat")
                } else {
                    logProto(grpcResponse, prompt = "ServiceToGate", noContentLogging = contentHidden)
                }

                stream.onNext(grpcResponse)
            }
        }

        suspend fun resend(grpcResponse: ServiceToGateProto) {
            val contentHidden = grpcResponse.getHeadersOrDefault(CONTENT_HIDDEN_HEADER, "false").toBoolean()

            grpcMutex.withLock {
                logProto(grpcResponse, prompt = "ServiceToGate", noContentLogging = contentHidden)
                stream.onNext(grpcResponse)
            }
        }

        override fun onNext(request: GateToServiceProto) {
            val tracker = TimeTracker()
            val requestContext = buildRequestContext(request)

            MDC.setContextMap(
                mapOf(
                    "requestId" to requestContext.requestId,
                    "connectorId" to requestContext.connectorId.toString(),
                    "gateRequestId" to requestContext.gateRequestId.toString(),
                    "MLP-BILLING-KEY" to requestContext.billingKey,
                )
            )
            try {
                processRequest(request, requestContext, tracker)
            } finally {
                MDC.clear()
            }
        }

        private fun processRequest(request: GateToServiceProto, requestContext: RequestContext, tracker: TimeTracker) {
            if (request.hasHeartBeat())
                logger.trace("GateToService (connector $connectorId, requestId: ${request.requestId}): heartbeat")
            else
                logProto(request, prompt = "GateToService (connector $connectorId)", noContentLogging = requestContext.noContentLogging)

            when (request.bodyCase) {
                GateToServiceProto.BodyCase.HEARTBEAT -> processHeartbeat(request.heartBeat)
                GateToServiceProto.BodyCase.CLUSTER -> processCluster(request.cluster)
                GateToServiceProto.BodyCase.PREDICT -> executor.predict(request.predict, tracker, requestContext)
                GateToServiceProto.BodyCase.PARTIALPREDICT -> executor.streamPredict(request.partialPredict, requestContext)
                GateToServiceProto.BodyCase.FIT -> executor.fit(request.fit, requestContext)
                GateToServiceProto.BodyCase.EXT -> executor.ext(request.ext, requestContext)
                GateToServiceProto.BodyCase.BATCH -> executor.batch(request.batch, requestContext)
                GateToServiceProto.BodyCase.ERROR -> processError(request.error)
                GateToServiceProto.BodyCase.CANCEL -> executor.cancelRequest(connectorId, request.cancel.requestIdToCancel)
                GateToServiceProto.BodyCase.STOPSERVING -> processStopServing()
                GateToServiceProto.BodyCase.SYNCSEQUENCENUMBERS -> syncSequenceNumbers(request.syncSequenceNumbers)
                GateToServiceProto.BodyCase.BODY_NOT_SET -> logger.warn("Request body is not set")
                null -> logger.error("Connector $connectorId: body case is null")
                else -> logger.debug("Could not find request bodyCase with type {}", request.bodyCase)
            }
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

        private fun processError(error: ApiErrorProto) = runBlocking {
            when (error.code) {
                "mlp.gate.instance_by_token_not_found" ->
                    processTokenNotFound()

                else ->
                    logger.error("Connector $connectorId: error ${error.message}")
            }
        }

        private fun processStopServing() {
            logger.info("$this: receive graceful shutdown from gate ...")
            state.shuttingDown()

            scope.launch {
                gracefulShutdownPrivate()
            }
        }

        private fun syncSequenceNumbers(sequenceNumbers: SyncSequenceNumbersProto) {
            logger.debug("Connector $connectorId received sequenceNumber $sequenceNumbers")
            when (sequenceNumbers.bodyCase) {
                SyncSequenceNumbersProto.BodyCase.LASTPROCESSEDSEQUENCENUMBER -> {
                    serviceToGateMessageStorage.headMap(sequenceNumbers.lastProcessedSequenceNumber + 1).clear()
                }

                SyncSequenceNumbersProto.BodyCase.INITIALGATESEQUENCENUMBER -> {
                    runBlocking {
                        logger.info(
                            "Connector $connectorId received initialGateSequenceNumber=${sequenceNumbers.initialGateSequenceNumber}, " +
                                    "adapter lastSentSequenceNumber=${lastSentSequenceNumber.get()}"
                        )
                        serviceToGateMessageStorage.headMap(sequenceNumbers.initialGateSequenceNumber + 1).clear()
                        serviceToGateMessageStorage.headMap(lastSentSequenceNumber.get() + 1).forEach { (_, proto) ->
                            resend(proto)
                        }
                        setActiveState()
                    }
                }

                SyncSequenceNumbersProto.BodyCase.BODY_NOT_SET, null -> {}
            }
        }

        private fun processTokenNotFound() {
            logger.warn("Connector $connectorId: Receive instance_by_token_not_found error, so shutdown grpc channel")
            state.shuttingDown()

            scope.launch {
                gracefulShutdownPrivate()
                state.shutdownReason = "instance_by_token_not_found"
            }
        }

        suspend fun gracefulShutdown() {
            if (state.isShutdownTypeState())
                return

            logConnecting("{}: graceful shutting down grpc channel ...", this)
            state.shuttingDown()

            if (!this::stream.isInitialized) {
                logConnecting("{}: ... stream is not initialized, skipping stream completion ...", this)
                return gracefulShutdownManagedChannel()
            }

            runCatching {
                send(stopServingProto)
                logConnecting("{}: sent stopServing to gate, waiting for stopServing from gate ...", this)

                withTimeout(config.shutdownConfig.actionConnectorMs) {
                    while (!state.shutdown) {
                        delay(100)
                    }
                }
            }.onFailure { logger.error("$this: can't send stop serving, continue shutdown ...", it) }

            if (!state.shutdown)
                shutdownNow()
        }

        private suspend fun gracefulShutdownPrivate() {
            logConnecting("{}: completing stream to {} ...", this, targetUrl)

            grpcMutex.withLock {
                stream.onCompleted()
            }

            gracefulShutdownManagedChannel()
        }

        suspend fun shutdownNow() {
            if (state.isShutdownTypeState()) {
                return
            }

            logConnecting("{}: force shutting down grpc channel ...", this)
            state.shuttingDown()

            if (!this::stream.isInitialized) {
                logConnecting("{}: stream is not initialized, skipping stream completion", this)
                return shutdownNowManagedChannel()
            }

            runCatching { send(stopServingProto) }
                .onFailure { logger.error("$this: can't send stop serving", it) }

            logConnecting("{}: completing stream to {} ...", this, targetUrl)

            grpcMutex.withLock {
                stream.onCompleted()
            }

            shutdownNowManagedChannel()
        }

        private suspend fun sendStartServingProto() {
            logger.info("Connector $connectorId: sending start serving to $targetUrl ...")
            runCatching {
                send(
                    ServiceToGateProto.newBuilder()
                        .setStartServing(
                            StartServingProto.newBuilder()
                                .setRunningInstanceSequenceNumber(lastSentSequenceNumber.get())
                                .setConnectionToken(pool.token)
                                .setHostname(context.environment["HOSTNAME"] ?: "localhost")
                                .setVersion(SDK_VERSION)
                                .setImage(context.environment["IMAGE_NAME"] ?: "")
                                .setServiceDescriptor(executor.action.getDescriptor())
                                .build()
                        )
                )
            }.onFailure {
                logger.error("Connector $connectorId: error on first start serving to $targetUrl", it)
                gracefulShutdownManagedChannel()
            }
        }

        private fun setActiveState() {
            logger.info("Connector $connectorId: sending start serving to $targetUrl ...")
            runCatching {
                state.active()
            }.onFailure {
                logger.error("Connector $connectorId: error on first start serving to $targetUrl", it)
                gracefulShutdownManagedChannel()
            }
        }

        private fun gracefulShutdownManagedChannel(reason: String? = null) {
            logConnecting("{}: graceful shutting down managed channel to {} ...", this, targetUrl)

            try {
                if (!this::managedChannel.isInitialized) {
                    logConnecting("{}: managed channel is not initialized, skipping managed channel shutdown", this)
                    return
                }

                if (managedChannel.isShutdown) {
                    return
                }

                managedChannel.shutdown()
                state.shutdown()

                val timeoutSeconds = 10L
                if (managedChannel.awaitTermination(timeoutSeconds, SECONDS)) {
                    return logConnecting("{}: ... managed channel has been successfully shutdown", this)
                }

                logConnecting("{}: ... managed channel has not been shutdown in {} seconds, force shutdown ...", this, timeoutSeconds)
                runCatching { managedChannel.shutdownNow() }
                    .onFailure { logger.error("$this: can't force shutdown managed channel", it) }
                    .onSuccess { logConnecting("{}: ... managed channel has been successfully shutdown", this) }
            } catch (e: InterruptedException) {
                logger.error("$this: ... managed channel has not been shutdown", e)
            } finally {
                state.shutdown()
            }
        }

        private fun shutdownNowManagedChannel() {
            logConnecting("{}: force shutting down managed channel ...", this)

            try {
                if (!this::managedChannel.isInitialized) {
                    logConnecting("{}: managed channel is not initialized, skipping managed channel shutdown", this)
                    return
                }

                if (managedChannel.isShutdown) {
                    return
                }

                runCatching { managedChannel.shutdownNow() }
                    .onFailure { logger.error("$this: can't shutdown managed channel", it) }
                    .onSuccess { logConnecting("{}: ... managed channel has been successfully shutdown", this) }
            } finally {
                state.shutdown()
            }
        }

        private fun processHeartbeat(heartBeat: HeartBeatProto) {
            logger.info("Connector $connectorId: received heartbeat: $heartBeat")
            lastServerHeartbeat.set(now())

            if (heartbeatInterval.get() == null) {
                heartbeatInterval.set(ofMillis(heartBeat.interval.toLong()))
            }
        }

        private fun processCluster(cluster: ClusterUpdateProto) {
            if (config.ignoreClusterUpdates) return

            if (targetUrl != cluster.currentServer) {
                logger.info("$this: url is changed from $targetUrl to ${cluster.currentServer}")
                targetUrl = cluster.currentServer
            }

            scope.launch {
                pool.updateConnectors(cluster.serversList)
            }
        }

        private fun launchHeartbeatJob() = scope.launch {
            logConnecting("Connector {}: starting heartbeats with interval {} ms", connectorId, heartbeatInterval)

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

        override fun toString() = "GrpcChannel($targetUrl) of ${this@Connector}"
    }

    private fun logConnecting(message: String, vararg args: Any) {
        if (gatewayPermanentlyUnavailable) return
        logger.debug(message, *args)
    }

    private fun buildRequestContext(request: GateToServiceProto): RequestContext {
        return RequestContext(
            callerAccountId = request.getHeadersOrDefault(CALLER_ACCOUNT_ID, null)?.toLongOrNull(),
            noContentLogging = request.getHeadersOrDefault(CONTENT_HIDDEN_HEADER, "false").toBoolean(),
            requestId = request.headersMap["Z-requestId"] ?: request.requestId.toString(),
            billingKey = request.headersMap["MLP-BILLING-KEY"],
            connectorId = connectorId,
            gateRequestId = request.requestId,
        )
    }

    companion object {
        private val lastConnectorId = AtomicLong()
        private val lastSentSequenceNumber = AtomicLong(0)
        private val runningInstanceId = UUID.randomUUID().toString()
        private const val LIVENESS_PROBE = "/tmp/liveness-probe"

        private fun AtomicReference<GrpcChannel?>.isShutdownStateOrNull() = get() == null
                || get()?.state?.shutdown == true

        private fun AtomicReference<GrpcChannel?>.isActiveState() = get()
            ?.state
            ?.active == true
    }
}

private val stopServingProto: ServiceToGateProto.Builder =
    ServiceToGateProto.newBuilder().setStopServing(StopServingProto.getDefaultInstance())

private val heartbeatProto: ServiceToGateProto.Builder =
    ServiceToGateProto.newBuilder().setHeartBeat(HeartBeatProto.getDefaultInstance())

fun ServiceToGateProtoOrBuilder.isSequenced() = when (bodyCase) {
    ServiceToGateProto.BodyCase.PREDICT,
    ServiceToGateProto.BodyCase.PARTIALPREDICT,
    ServiceToGateProto.BodyCase.FIT,
    ServiceToGateProto.BodyCase.EXT,
    ServiceToGateProto.BodyCase.BATCH,
    ServiceToGateProto.BodyCase.ERROR,
    ServiceToGateProto.BodyCase.DEFERREDBILLINGCHARGE -> true

    ServiceToGateProto.BodyCase.HEARTBEAT,
    ServiceToGateProto.BodyCase.STARTSERVING,
    ServiceToGateProto.BodyCase.STOPSERVING,
    ServiceToGateProto.BodyCase.STATUS,
    ServiceToGateProto.BodyCase.FITSTATUS,
    ServiceToGateProto.BodyCase.BODY_NOT_SET -> false

    null -> false
}