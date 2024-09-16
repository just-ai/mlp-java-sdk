package com.mlp.sdk

import com.mlp.gate.ApiErrorProto
import com.mlp.gate.ClusterUpdateProto
import com.mlp.gate.GateGrpc
import com.mlp.gate.GateToServiceProto
import com.mlp.gate.GateToServiceProto.BodyCase.BATCH
import com.mlp.gate.GateToServiceProto.BodyCase.BODY_NOT_SET
import com.mlp.gate.GateToServiceProto.BodyCase.CLUSTER
import com.mlp.gate.GateToServiceProto.BodyCase.ERROR
import com.mlp.gate.GateToServiceProto.BodyCase.EXT
import com.mlp.gate.GateToServiceProto.BodyCase.FIT
import com.mlp.gate.GateToServiceProto.BodyCase.HEARTBEAT
import com.mlp.gate.GateToServiceProto.BodyCase.PARTIALPREDICT
import com.mlp.gate.GateToServiceProto.BodyCase.PREDICT
import com.mlp.gate.GateToServiceProto.BodyCase.STOPSERVING
import com.mlp.gate.HeartBeatProto
import com.mlp.gate.ServiceInfoProto
import com.mlp.gate.ServiceToGateProto
import com.mlp.gate.StartServingProto
import com.mlp.gate.StopServingProto
import com.mlp.sdk.State.Condition.ACTIVE
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.*
import java.io.File
import java.time.Duration
import java.time.Duration.between
import java.time.Duration.ofMillis
import java.time.Instant.now
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.min
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.MDC
import kotlin.Long.Companion.MIN_VALUE

class Connector(
    @Volatile
    var targetUrl: String,
    val pool: ConnectorsPool,
    val executor: TaskExecutor,
    val config: MlpServiceConfig,
    val scope: CoroutineScope,
    override val context: MlpExecutionContext
) : WithExecutionContext, WithState(ACTIVE) {

    val connectorId = lastConnectorId.getAndIncrement()

    private val startServingProto = ServiceToGateProto.newBuilder()
        .setStartServing(
            StartServingProto.newBuilder()
                .setConnectionToken(pool.token)
                .setHostname(context.environment["HOSTNAME"] ?: "localhost")
                .setVersion(SDK_VERSION)
                .setImage(context.environment["IMAGE_NAME"] ?: "")
                .setServiceDescriptor(executor.action.getDescriptor())
                .build()
        )
        .build()
    private val grpcChannel = AtomicReference<GrpcChannel?>(null)
    private val keepConnectionJob: Job

    init {
        keepConnectionJob = launchKeepConnectionJob()
    }

    suspend fun sendServiceToGate(grpcResponse: ServiceToGateProto) {
        check(isAvailableToSendGrpc()) { "$this: cannot send message because it is not connected. Current grpcChannel state ${grpcChannel.get()?.state}" }

        grpcChannel.get()
            ?.send(grpcResponse)
    }

    suspend fun gracefulShutdown() {
        if (state.isShutdownTypeState()) {
            return
        }

        state.shuttingDown()
        logger.debug("$this: graceful shutting down ...")

        runCatching { runBlocking { keepConnectionJob.cancelAndJoin() } }
            .onFailure { logger.error("$this: error while keep connection job cancelling", it) }

        grpcChannel.get()?.gracefulShutdown()

        state.shutdown()
        logger.debug("$this: ... has been successfully shutdown")
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
        logger.debug("$this: force shutting down ...")

        runCatching { runBlocking { keepConnectionJob.cancelAndJoin() } }
            .onFailure { logger.error("$this: error while keep connection job cancelling", it) }

        grpcChannel.get()?.shutdownNow()

        state.shutdown()
        logger.debug("$this: ... has been successfully shutdown")
    }

    private fun launchKeepConnectionJob() = scope.launch {
        logger.debug("${this@Connector}: keep connection job is started ...")

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
                            logger.debug("${this@Connector}: reset progressiveDelay to 100 because channel was connected")
                        }
                        progressiveDelay = 100L
                    } else {
                        progressiveDelay = min(progressiveDelay * 2, 10_000L)
                        logger.debug("${this@Connector}: increase progressiveDelay to $progressiveDelay")
                    }
                }

                if (grpcChannel.isActiveState()) {
                    lastActiveTime = now()
                    progressiveDelay = 100L

                    if (!executor.isAbleProcessNewJobs(connectorId, grpcChannel.grpcChannelId())) {
                        logger.error("${this@Connector}: grpc channel is active, but can not process new jobs")
                    }
                }

                if (now() > lastActiveTime + ofMillis(config.grpcConnectTimeoutMs)) {
                    tryGrpcShutdown()
                }

                delay(progressiveDelay)
            }
            logger.debug("${this@Connector}: ... keep connection job is stopped because scope is not active")
        }.onFailure {
            if (state.isShutdownTypeState() && it is CancellationException)
                return@onFailure
            else
                logger.error("${this@Connector}: error while keep connection job running", it)
        }

        logger.debug("${this@Connector}: ... keep connection job has been stopped")
    }

    private suspend fun throttleIfUnknownConnectionToken() {
        if (grpcChannel.get()?.state?.shutdownReason == "instance_by_token_not_found") {
            delay(1_000L)
        }
    }

    private suspend fun tryGrpcShutdown() {
//        logger.debug("${this@Connector}: grpc channel is not active for 10 seconds, reconnecting ...")

        runCatching {
            grpcChannel.getAndSet(null)
                ?.shutdownNow()
        }.onFailure {
            logger.error("${this@Connector}: cannot shutdown grpc channel", it)
        }
    }

    private suspend fun tryConnectOrShutdown(): Boolean {
//        logger.debug("${this@Connector}: creating new grpc channel ...")
        val newGrpcChannel = GrpcChannel(context)

        return runCatching {
            newGrpcChannel.tryConnect()
            grpcChannel.getAndSet(newGrpcChannel)?.shutdownNow()
            true
        }.onFailure {
//            logger.error("${this@Connector}: cannot create new grpc channel", it)
            newGrpcChannel.shutdownNow()
        }.getOrDefault(false)
    }

    override fun toString() = "Connector(id='$connectorId', url='$targetUrl')"

    companion object {
        private val lastConnectorId = AtomicLong()
        private val lastGrpcChannelId = AtomicLong()
        const val LIVENESS_PROBE = "/tmp/liveness-probe"
    }

    private inner class GrpcChannel(
        override val context: MlpExecutionContext
    ) : StreamObserver<GateToServiceProto>, WithExecutionContext, WithState() {

        private lateinit var managedChannel: ManagedChannel
        private lateinit var stream: StreamObserver<ServiceToGateProto>

        private val lastServerHeartbeat = AtomicReference(now())
        private val heartbeatInterval = AtomicReference<Duration>(null)
        private val grpcMutex = Mutex()
        val grpcChannelId = lastGrpcChannelId.getAndIncrement()

        init {
            launchHeartbeatJob()
        }

        suspend fun tryConnect() {
            check(state.notStarted) { "Connector $connectorId: GrpcChannel can connect only once" }
            logger.debug("Connector $connectorId: opening grpc channel to $targetUrl ...")
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
            executor.enableNewTasks(connectorId, grpcChannelId)
        }

        suspend fun send(grpcResponse: ServiceToGateProto) {
            if (grpcResponse.hasHeartBeat())
                logger.trace("ServiceToGateProto: heartbeat")
            else
                logProto(grpcResponse, prompt = "ServiceToGate")

            check(!state.notStarted && !state.shutdown) { "$this: can't send message in state $state" }

            grpcMutex.withLock {
                stream.onNext(grpcResponse)
            }
        }

        override fun onNext(request: GateToServiceProto) {
            val tracker = TimeTracker()
            val requestId = request.headersMap["Z-requestId"] ?: request.requestId.toString()
            MDC.setContextMap(mapOf(
                "requestId" to requestId,
                "connectorId" to connectorId.toString(),
                "gateRequestId" to request.requestId.toString(),
                "MLP-BILLING-KEY" to request.headersMap["MLP-BILLING-KEY"],
            ))
            try {
                processRequest(request, tracker)
            } finally {
                MDC.clear()
            }
        }

        private fun processRequest(request: GateToServiceProto, tracker: TimeTracker) {
            if (request.hasHeartBeat())
                logger.trace("GateToService (connector $connectorId, requestId: ${request.requestId}): heartbeat")
            else
                logProto(request, prompt = "GateToService (connector $connectorId)")

            when (request.bodyCase) {
                HEARTBEAT -> processHeartbeat(request.heartBeat)
                CLUSTER -> processCluster(request.cluster)
                PREDICT -> executor.predict(request.predict, request.requestId, connectorId, grpcChannelId, tracker)
                PARTIALPREDICT -> scope.launch { executor.streamPredict(request.partialPredict, request.requestId, connectorId, grpcChannelId) }
                FIT -> executor.fit(request.fit, request.requestId, connectorId, grpcChannelId)
                EXT -> executor.ext(request.ext, request.requestId, connectorId, grpcChannelId)
                BATCH -> executor.batch(request.batch, request.requestId, connectorId, grpcChannelId)
                ERROR -> processError(request.error)
                STOPSERVING -> processStopServing()
                BODY_NOT_SET -> logger.warn("Request body is not set")
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

            executor.cancelAll(connectorId, grpcChannelId)
            gracefulShutdownManagedChannel()
        }

        override fun onCompleted() {
            state.shuttingDown()
            logger.info("$this: RECEIVED completed")

            executor.cancelAll(connectorId, grpcChannelId)
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

            logger.debug("$this: graceful shutting down grpc channel ...")
            state.shuttingDown()

            if (!this::stream.isInitialized) {
                logger.debug("$this: ... stream is not initialized, skipping stream completion ...")
                return gracefulShutdownManagedChannel()
            }

            runCatching {
                send(stopServingProto)
                logger.debug("$this: sent stopServing to gate, waiting for stopServing from gate ...")

                withTimeout(config.shutdownConfig.actionConnectorMs) {
                    while(!state.shutdown) {
                        delay(100)
                    }
                }
            }.onFailure { logger.error("$this: can't send stop serving, continue shutdown ...", it) }

            if (!state.shutdown)
                shutdownNow()
        }

        private suspend fun gracefulShutdownPrivate() {
            executor.gracefulShutdownAll(connectorId, grpcChannelId)

            logger.debug("$this: completing stream to $targetUrl ...")
            runCatching { grpcMutex.withLock { stream.onCompleted() } }
                .onFailure { if (it !is IllegalStateException) logger.error("$this: can't complete stream", it) }

            gracefulShutdownManagedChannel()
        }

        suspend fun shutdownNow() {
            if (state.isShutdownTypeState()) {
                return
            }

            logger.debug("$this: force shutting down grpc channel ...")
            state.shuttingDown()

            if (!this::stream.isInitialized) {
                logger.debug("$this: stream is not initialized, skipping stream completion")
                return shutdownNowManagedChannel()
            }

            runCatching { send(stopServingProto) }
                .onFailure { logger.error("$this: can't send stop serving", it) }

            logger.debug("$this: completing stream to $targetUrl ...")
            runCatching { grpcMutex.withLock { stream.onCompleted() } }
                .onFailure { logger.error("$this: can't complete stream", it) }

            executor.cancelAll(connectorId, grpcChannelId)

            shutdownNowManagedChannel()
        }

        private suspend fun sendStartServingProto() {
            logger.info("Connector $connectorId: sending start serving to $targetUrl ...")
            runCatching {
                send(startServingProto)
                state.active()
            }.onFailure {
                logger.error("Connector $connectorId: error on first start serving to $targetUrl", it)
                gracefulShutdownManagedChannel()
            }
        }

        private fun gracefulShutdownManagedChannel(reason: String? = null) {
            logger.debug("$this: graceful shutting down managed channel to $targetUrl ...")

            try {
                if (!this::managedChannel.isInitialized) {
                    logger.debug("$this: managed channel is not initialized, skipping managed channel shutdown")
                    return
                }

                if (managedChannel.isShutdown) {
                    return
                }

                managedChannel.shutdown()
                state.shutdown()

                val timeoutSeconds = 10L
                if (managedChannel.awaitTermination(timeoutSeconds, SECONDS)) {
                    return logger.debug("$this: ... managed channel has been successfully shutdown")
                }

                logger.debug("$this: ... managed channel has not been shutdown in $timeoutSeconds seconds, force shutdown ...")
                runCatching { managedChannel.shutdownNow() }
                    .onFailure { logger.error("$this: can't force shutdown managed channel", it) }
                    .onSuccess { logger.debug("$this: ... managed channel has been successfully shutdown") }
            } catch (e: InterruptedException) {
                logger.error("$this: ... managed channel has not been shutdown", e)
            } finally {
                state.shutdown()
            }
        }

        private fun shutdownNowManagedChannel() {
            logger.debug("$this: force shutting down managed channel ...")

            try {
                if (!this::managedChannel.isInitialized) {
                    logger.debug("$this: managed channel is not initialized, skipping managed channel shutdown")
                    return
                }

                if (managedChannel.isShutdown) {
                    return
                }

                runCatching { managedChannel.shutdownNow() }
                    .onFailure { logger.error("$this: can't shutdown managed channel", it) }
                    .onSuccess { logger.debug("$this: ... managed channel has been successfully shutdown") }
            } finally {
                state.shutdown()
            }
        }

        private fun processHeartbeat(heartBeat: HeartBeatProto) {
            lastServerHeartbeat.set(now())

            if (heartbeatInterval.get() == null)
                heartbeatInterval.set(ofMillis(heartBeat.interval.toLong()))
        }

        private fun processCluster(cluster: ClusterUpdateProto) {
            if (targetUrl != cluster.currentServer) {
                logger.info("$this: url is changed from $targetUrl to ${cluster.currentServer}")
                targetUrl = cluster.currentServer
            }

            scope.launch {
                pool.updateConnectors(cluster.serversList)
            }
        }

        private fun launchHeartbeatJob() = scope.launch {
            logger.debug("Connector $connectorId: starting heartbeats with interval $heartbeatInterval ms")

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

    private fun AtomicReference<GrpcChannel?>.isShutdownStateOrNull() = get() == null
            || get()?.state?.shutdown == true

    private fun AtomicReference<GrpcChannel?>.isActiveState() = get()
        ?.state
        ?.active == true

    private fun AtomicReference<GrpcChannel?>.grpcChannelId() = get()
        ?.grpcChannelId
        ?: MIN_VALUE
}

private val ServiceInfoProto.asModelInfo
    get() = ModelInfo(
        accountId,
        modelId,
        modelName
    )

private val stopServingProto: ServiceToGateProto =
    ServiceToGateProto.newBuilder().setStopServing(StopServingProto.getDefaultInstance()).build()

private val heartbeatProto: ServiceToGateProto =
    ServiceToGateProto.newBuilder().setHeartBeat(HeartBeatProto.getDefaultInstance()).build()
