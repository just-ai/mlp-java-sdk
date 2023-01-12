package com.mlp.sdk

import com.mlp.gate.ServiceToGateProto
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
import com.mlp.gate.GateToServiceProto.BodyCase.PREDICT
import com.mlp.gate.GateToServiceProto.BodyCase.RESPONSE
import com.mlp.gate.GateToServiceProto.BodyCase.SERVICEINFO
import com.mlp.gate.HeartBeatProto
import com.mlp.gate.ServiceInfoProto
import com.mlp.gate.StartServingProto
import com.mlp.gate.StopServingProto
import com.mlp.sdk.ConnectorsPool.Companion.clusterDispatcher
import com.mlp.sdk.utils.WithLogger
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.time.Duration
import java.time.Duration.between
import java.time.Duration.ofMillis
import java.time.Instant
import java.time.Instant.now
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class Connector(
    var targetUrl: String,
    val pool: ConnectorsPool,
    val executor: TaskExecutor,
    val pipelineClient: PipelineClient,
    val config: MlpServiceConfig
) : WithLogger, WithState(State.Condition.ACTIVE) {

    val id = lastConnectorId.getAndIncrement()

    private val grpcChannel = AtomicReference<GrpcChannel?>(null)
    private val keepConnectionJob = launchKeepConnectionJob()

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

    private fun launchKeepConnectionJob() = clusterDispatcher.launch {
        logger.debug("${this@Connector}: keep connection job is started ...")

        var lastActiveTime = now()
        runCatching {
            while (isActive) {
                if (grpcChannel.isShutdownStateOrNull()) {
                    lastActiveTime = tryConnectOrShutdown(lastActiveTime)
                }
                if (grpcChannel.isActiveState()) {
                    lastActiveTime = now()
                }
                if (now() > lastActiveTime + ofMillis(config.grpcConnectTimeoutMs)) {
                    tryGrpcShutdown()
                }

                delay(100)
            }
            logger.debug("${this@Connector}: ... keep connection job is stopped because scope is not active")
        }.onFailure { logger.error("${this@Connector}: error while keep connection job running", it) }
        logger.debug("${this@Connector}: ... keep connection job has been stopped")
    }

    private suspend fun tryGrpcShutdown() {
        logger.debug("${this@Connector}: grpc channel is not active for 10 seconds, reconnecting ...")

        runCatching {
            grpcChannel.getAndSet(null)
                ?.shutdownNow()
        }.onFailure {
            logger.error("${this@Connector}: cannot shutdown grpc channel", it)
        }
    }

    private suspend fun tryConnectOrShutdown(
        lastActiveTime: Instant
    ): Instant {
        logger.debug("${this@Connector}: creating new grpc channel ...")
        val newGrpcChannel = GrpcChannel()
        runCatching {
            newGrpcChannel.tryConnect()
            grpcChannel.getAndSet(newGrpcChannel)?.shutdownNow()
            return now()
        }.onFailure {
            logger.error("${this@Connector}: cannot create new grpc channel", it)
            newGrpcChannel.shutdownNow()
        }
        return lastActiveTime
    }

    override fun toString() = "Connector(id='$id', url='$targetUrl')"

    private val startServingProto = ServiceToGateProto.newBuilder()
        .setStartServing(
            StartServingProto.newBuilder()
                .setConnectionToken(pool.token)
                .setServiceDescriptor(executor.action.getDescriptor())
                .build()
        )
        .build()

    companion object {
        private val lastConnectorId = AtomicLong()
        const val LIVENESS_PROBE = "/tmp/liveness-probe"
    }

    private inner class GrpcChannel : StreamObserver<GateToServiceProto>, WithLogger, WithState() {

        private lateinit var managedChannel: ManagedChannel
        private lateinit var stream: StreamObserver<ServiceToGateProto>

        private val lastServerHeartbeat = AtomicReference(now())
        private val heartbeatInterval = AtomicReference<Duration>(null)
        private val grpcMutex = Mutex()

        init {
            launchHeartbeatJob()
        }

        suspend fun tryConnect() {
            check(state.notStarted) { "Connector $id: GrpcChannel can connect only once" }
            logger.debug("Connector $id: opening grpc channel to $targetUrl ...")
            state.starting()

            val channelBuilder = ManagedChannelBuilder.forTarget(targetUrl)
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
        }

        suspend fun send(grpcResponse: ServiceToGateProto) {
            if (grpcResponse.hasHeartBeat())
                logger.trace("$this: send message ${grpcResponse.bodyCase} to $targetUrl")
            else
                logger.debug("$this: send message ${grpcResponse.bodyCase} to $targetUrl")

            logRequest(grpcResponse)

            check(!state.notStarted && !state.shutdown) { "$this: can't send message in state $state" }

            grpcMutex.withLock {
                stream.onNext(grpcResponse)
            }
        }

        override fun onNext(request: GateToServiceProto) {
            if (request.hasHeartBeat())
                logger.trace("Connector $id: received request ${request.bodyCase} with id ${request.requestId}")
            else
                logger.debug("Connector $id: received request ${request.bodyCase} with id ${request.requestId}")

            when (request.bodyCase) {
                SERVICEINFO -> pipelineClient.modelInfo = request.serviceInfo.asModelInfo
                HEARTBEAT -> processHeartbeat(request.heartBeat)
                CLUSTER -> processCluster(request.cluster)
                PREDICT -> executor.predict(request.predict, request.requestId, id)
                FIT -> executor.fit(request.fit, request.requestId, id)
                EXT -> executor.ext(request.ext, request.requestId, id)
                BATCH -> executor.batch(request.batch, request.requestId, id)
                ERROR -> logger.error("Connector $id: error ${request.error.message}")
                RESPONSE -> pipelineClient.registerResponse(request.requestId, request.response)
                BODY_NOT_SET -> logger.warn("Request body is not set")
                null -> logger.error("Connector $id: body case is null")
                else -> logger.debug("Could not find request bodyCase with type ${request.bodyCase}")
            }
        }

        override fun onError(e: Throwable) {
            state.shuttingDown()
            logger.error("$this: RECEIVED error ${e.message}", e)

            executor.cancelAll(id)
            gracefulShutdownManagedChannel()
        }

        override fun onCompleted() {
            state.shuttingDown()
            logger.info("$this: RECEIVED completed")

            runBlocking {
                executor.cancelAll(id)
            }
            gracefulShutdownManagedChannel()
        }

        suspend fun gracefulShutdown() {
            if (state.isShutdownTypeState()) {
                return
            }

            logger.debug("$this: graceful shutting down grpc channel ...")
            state.shuttingDown()

            runCatching { send(stopServingProto) }
                .onFailure { logger.error("$this: can't send stop serving", it) }

            executor.gracefulShutdownAll(id)

            logger.debug("$this: completing stream to $targetUrl ...")
            runCatching { grpcMutex.withLock { stream.onCompleted() } }
                .onFailure { logger.error("$this: can't complete stream", it) }

            gracefulShutdownManagedChannel()
        }

        suspend fun shutdownNow() {
            if (state.isShutdownTypeState()) {
                return
            }

            logger.debug("$this: force shutting down grpc channel ...")
            state.shuttingDown()

            runCatching { send(stopServingProto) }
                .onFailure { logger.error("$this: can't send stop serving", it) }

            executor.cancelAll(id)

            logger.debug("$this: completing stream to $targetUrl ...")
            runCatching {
                grpcMutex.withLock {
                    stream.onCompleted()
                }
            }.onFailure { logger.error("$this: can't complete stream", it) }

            logger.debug("$this: force shutting down managed channel ...")
            runCatching {
                managedChannel.shutdownNow()
            }.onFailure { logger.error("$this: can't shutdown managed channel", it) }

            state.shutdown()
        }


        private suspend fun sendStartServingProto() {
            logger.info("Connector $id: sending start serving to $targetUrl ...")
            runCatching {
                send(startServingProto)
                state.active()
            }.onFailure {
                logger.error("Connector $id: error on first start serving to $targetUrl", it)
                gracefulShutdownManagedChannel()
            }
        }

        private fun gracefulShutdownManagedChannel() {
            logger.debug("$this: graceful shutting down managed channel to $targetUrl ...")

            try {
                if (managedChannel.isShutdown) {
                    return
                }

                managedChannel.shutdown()

                val timeoutSeconds = 10L
                if (managedChannel.awaitTermination(timeoutSeconds, SECONDS)) {
                    return logger.debug("$this: ... managed channel has been successfully shutdown")
                }

                logger.debug("$this: ... managed channel has not been shutdown in $timeoutSeconds seconds, force shutdown ...")
                runCatching { managedChannel.shutdownNow() }
                    .onFailure { logger.error("$this: can't shutdown managed channel", it) }
            } catch (e: InterruptedException) {
                logger.error("$this: ... managed channel has not been shutdown", e)
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

            clusterDispatcher.launch {
                pool.updateConnectors(cluster.serversList)
            }
        }

        private fun launchHeartbeatJob() = clusterDispatcher.launch {
            logger.debug("Connector $id: starting heartbeats with interval $heartbeatInterval ms")

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
                    .onFailure { logger.error("Connector $id: can't send heartbeat", it) }

                delay(interval.toMillis())

                val maxTimeout = interval.multipliedBy(3).plusSeconds(1)
                if (between(lastServerHeartbeat.get(), now()) > maxTimeout) {
                    logger.error("Connector $id: no heartbeat for $maxTimeout ms")
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
