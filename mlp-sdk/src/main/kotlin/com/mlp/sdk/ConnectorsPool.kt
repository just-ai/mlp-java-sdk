package com.mlp.sdk

import com.mlp.gate.ServiceToGateProto
import com.mlp.sdk.State.Condition.ACTIVE
import java.time.Duration.ofSeconds
import java.time.Instant.now
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ConnectorsPool(
    val token: String,
    private val executor: TaskExecutor,
    private val config: MlpServiceConfig,
    override val context: MlpExecutionContext
) : WithExecutionContext, WithState(ACTIVE) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val clusterMutex = Mutex()

    private var connectors = config.initialGateUrls
        .map { Connector(it, this, executor, config, scope, context) }
        .associateBy { it.connectorId }

    init {
        launchConnectorsMonitor()
    }

    suspend fun send(connectorId: Long, toGateProto: ServiceToGateProto.Builder) {
        connectors[connectorId]
            ?.sendServiceToGate(toGateProto)
            ?: throw NoSuchElementException("There is no connector $connectorId")
    }

    suspend fun sendToAnyGate(toGateProto: ServiceToGateProto.Builder) {
        connectors.values
            .filter { it.isAvailableToSendGrpc() }
            .randomOrNull()
            ?.sendServiceToGate(toGateProto)
            ?: throw NoSuchElementException("There is no connected to send connector")
    }

    suspend fun gracefulShutdown() {
        if (state.isShutdownTypeState()) {
            return
        }

        clusterMutex.withLock {
            state.shuttingDown()
            logger.info("$this: graceful shutting down connectors pool ...")

            // Дожидаемся коннекторов: пул считается остановленным только когда каждый из них
            // отправил stopServing, слил активные запросы и закрыл стрим. Fire-and-forget здесь
            // означал бы выход JVM раньше, чем гейт получил ответы.
            connectors.values
                .map { connector ->
                    scope.launch {
                        runCatching { connector.gracefulShutdown() }
                            .onFailure { logger.error("$this: error while graceful shutting down $connector", it) }
                    }
                }
                .joinAll()

            state.shutdown()
            logger.info("$this: ... connectors pool is shut down")
        }
    }

    suspend fun shutdownNow() {
        if (state.isShutdownTypeState()) {
            return
        }

        clusterMutex.withLock {
            state.shuttingDown()
            logger.info("$this: force shutting down connectors pool ...")

            connectors.values.forEach { it.shutdown() }

            state.shutdown()
            logger.info("$this: ... connectors pool is shut down")
        }
    }

    internal suspend fun updateConnectors(urls: List<String>) {
        val connectorsToShutdown: Collection<Connector>
        clusterMutex.withLock {
            logger.info("$this: updating connectors by urls: $urls")

            check(state.active) { "Pool state must be active to update connectors, but it is $state" }

            val connectorsMap = connectors.map { it.value.targetUrl to it.value }.toMap()
            if (connectorsMap.keys == urls) {
                logger.debug("$this: ... connectors are up to date")
                return
            }

            connectorsToShutdown = connectors.filterValues { it.targetUrl !in urls }.values

            connectors = urls.map { url ->
                connectorsMap[url] ?: Connector(url, this, executor, config, scope, context)
            }.associateBy { it.connectorId }

            logger.info("$this: ... connectors are updated")
        }

        logger.info("$this: shutting down old connectors ...")
        connectorsToShutdown.forEach {
            scope.launch { it.gracefulShutdown() }
        }

        logger.info("$this: ... old connectors are shut down")
    }

    private fun launchConnectorsMonitor() = scope.launch {
        logger.info("$this: launched connectors monitor")
        var lastActiveTime = now()
        while (state.active && isActive) {
            if (connectors.values.any { it.isGrpcChannelActive() })
                lastActiveTime = now()

            if (now() > lastActiveTime + ofSeconds(5)) {
                logger.warn("$this: no active connectors for 5 seconds, updating connectors by urls: ${config.initialGateUrls}")
                updateConnectors(config.initialGateUrls)
                lastActiveTime = now()
            }

            delay(1000)
        }
        logger.info("$this: ... connectors monitor is stopped")
    }

    override fun toString() = "ConnectorsPool"
}
