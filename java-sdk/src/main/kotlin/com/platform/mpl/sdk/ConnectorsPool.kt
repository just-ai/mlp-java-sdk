package com.platform.mpl.sdk

import com.platform.mpl.gate.ActionToGateProto
import com.platform.mpl.sdk.State.Condition.ACTIVE
import com.platform.mpl.sdk.utils.WithLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration.ofSeconds
import java.time.Instant.now

class ConnectorsPool(
    val token: String,
    private val executor: ActionTaskExecutor,
    private val pipelineClient: PipelineClient,
    private val config: PlatformActionConfig
) : WithLogger, WithState(ACTIVE) {

    private var connectors = config.initialGateUrls.map {
        Connector(it, this, executor, pipelineClient, config)
    }.associateBy { it.id }

    init {
        launchConnectorsMonitor()
    }

    suspend fun send(connectorId: Long, toGateProto: ActionToGateProto) {
        connectors[connectorId]
            ?.sendActionToGate(toGateProto)
            ?: throw NoSuchElementException("There is no connector $connectorId")
    }

    suspend fun sendToAnyGate(toGateProto: ActionToGateProto) {
        connectors.values
            .filter { it.isAvailableToSendGrpc() }
            .randomOrNull()
            ?.sendActionToGate(toGateProto)
            ?: throw NoSuchElementException("There is no connected to send connector")
    }

    suspend fun gracefulShutdown() {
        if (state.isShutdownTypeState()) {
            return
        }

        clusterMutex.withLock {
            state.shuttingDown()
            logger.info("$this: graceful shutting down connectors pool ...")

            runBlocking {
                connectors.values.forEach {
                    launch { it.gracefulShutdown() }
                }
            }

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
                connectorsMap[url] ?: Connector(url, this, executor, pipelineClient, config)
            }.associateBy { it.id }

            logger.info("$this: ... connectors are updated")
        }

        runBlocking {
            logger.info("$this: shutting down old connectors ...")
            connectorsToShutdown.forEach {
                launch { it.gracefulShutdown() }
            }
        }

        logger.info("$this: ... old connectors are shut down")
    }

    private fun launchConnectorsMonitor() = clusterDispatcher.launch {
        logger.info("$this: launched connectors monitor")
        var lastActiveTime = now()
        while (state.active && isActive) {
            if (connectors.values.any {it.isConnected()})
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

    companion object {
        val clusterDispatcher = CoroutineScope(Dispatchers.IO)
        private val clusterMutex = Mutex()
    }
}

internal fun WithLogger.logRequest(
    request: Any,
    messageBody: String = "SENDING request",
) {
    val minimizedRequest = request.toString()
        .replace("\n", " ")
        .replace("  ", " ")
    val messageFitted = minimizedRequest.length <= 1000

    if (messageFitted)
        logger.trace("$this: $messageBody \t$minimizedRequest")
    else
        logger.trace("$this: $messageBody with data length ${minimizedRequest.length}")
}
