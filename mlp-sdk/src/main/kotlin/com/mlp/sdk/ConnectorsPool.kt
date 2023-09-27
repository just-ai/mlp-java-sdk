package com.mlp.sdk

import com.google.protobuf.MessageLite
import com.mlp.gate.ServiceToGateProto
import com.mlp.sdk.Connector.Companion.getConnector
import com.mlp.sdk.State.Condition.ACTIVE
import com.mlp.sdk.utils.WithLogger
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
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import org.slf4j.ILoggerFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// TODO возможно сюда стоит положить и конфиги и ещё что нить. И передавать одним объектом

/**
 * val config: MlpServiceConfig = loadActionConfig(),
 *     override val environment: Environment = Environment(emptyMap()),
 *     dispatcher: CoroutineDispatcher? = null,
 *     override val loggerFactory: ILoggerFactory? = null
 */
data class SdkContext(
    val loggerFactory: ILoggerFactory? = null,
    val environment: Environment = Environment(),
//    val config: MlpServiceConfig,
//    val dispatcher: CoroutineDispatcher = Executors.newFixedThreadPool(config.threadPoolSize).asCoroutineDispatcher(),

    )

interface WithSdkContext {

//    val config: MlpServiceConfig
//        get() = context.config
    val loggerFactory: ILoggerFactory?
        get() = context.loggerFactory
    val environment: Environment
        get() = context.environment

    val context: SdkContext

    val logger: Logger
        get() = loggerFactory?.getLogger(javaClass.name)
            ?: LoggerFactory.getLogger(javaClass)
}

class ConnectorsPool private constructor(
    val token: String,
    private val executor: TaskExecutor,
    private val config: MlpServiceConfig,
    override val context: SdkContext
) : WithSdkContext, WithState(ACTIVE) {

    private val clusterMutex = Mutex()

    private var connectors = config.initialGateUrls
        .map { getConnector(it, this, executor, config) }
        .associateBy { it.id }

    init {
        launchConnectorsMonitor()
    }

    suspend fun send(connectorId: Long, toGateProto: ServiceToGateProto) {
        connectors[connectorId]
            ?.sendServiceToGate(toGateProto)
            ?: throw NoSuchElementException("There is no connector $connectorId")
    }

    suspend fun sendToAnyGate(toGateProto: ServiceToGateProto) {
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
                connectorsMap[url] ?: getConnector(url, this, executor, config)
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
        val clusterDispatcher = CoroutineScope(Dispatchers.IO + SupervisorJob())

        fun WithSdkContext.getConnectorsPool(token: String, executor: TaskExecutor, config: MlpServiceConfig) =
            ConnectorsPool(token, executor, config, context)
    }
}

internal fun WithSdkContext.logProto(
    body: MessageLite,
    prompt: String,
) {
    // This size is always smaller than string version
    val approximateSize = body.serializedSize
    if (approximateSize > 1000) {
        logger.debug("$prompt: data length at least $approximateSize")
        return
    }

    // Stringify can produce OOM for large bodies
    val minimizedRequest = body.toString()
        .replace("\n", " ")
        .replace("  ", " ")
    val messageFitted = minimizedRequest.length <= 1000

    if (messageFitted)
        logger.debug("$prompt: \t$minimizedRequest")
    else
        logger.debug("$prompt: data length ${minimizedRequest.length}")
}
