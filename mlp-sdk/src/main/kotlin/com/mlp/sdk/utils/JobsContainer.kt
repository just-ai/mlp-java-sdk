package com.mlp.sdk.utils

import com.mlp.sdk.MlpServiceConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration.ofMillis
import java.time.Instant.now
import java.util.concurrent.ConcurrentHashMap

class JobsContainer(
    private val config: MlpServiceConfig,
) : WithLogger {

    private val container = ConcurrentHashMap<Long, ConnectorContainer>()

    fun put(connectorId: Long, requestId: Long, job: Job): Boolean {
        val connectorContainer = container.computeIfAbsent(connectorId) {
            ConnectorContainer(ConcurrentHashMap<Long, Job>())
        }

        val isShuttingDownConnector = container[connectorId]
            ?.let {
                shutdownMutex.holdsLock(it)
            } == true
        return if (!isShuttingDownConnector) {
            connectorContainer.requestJobMap[requestId] = job
            true
        } else {
            false
        }
    }

    fun cancel(connectorId: Long) {
        container[connectorId]
            ?.cancelAll()
    }

    fun cancelAll() {
        container.forEach {
            it.value.cancelAll()
        }
    }

    fun remove(connectorId: Long, requestId: Long) {
        container[connectorId]
            ?.requestJobMap
            ?.remove(requestId)
    }

    suspend fun gracefulShutdownByConnector(connectorId: Long) {
        val shutdownConfig = config.shutdownConfig
        delay(shutdownConfig.actionConnectorRequestDelayMs)
        val owner = container[connectorId]
        shutdownMutex.withLock(owner) {
            cancel(connectorId, shutdownConfig.actionConnectorMs - shutdownConfig.actionConnectorRequestDelayMs)
        }
    }

    private suspend fun cancel(connectorId: Long, delayMs: Long) {
        val endInstance = now() + ofMillis(delayMs)
        while (now() < endInstance) {
            val allJobsComplete = container[connectorId]
                ?.requestJobMap
                .isNullOrEmpty()
            if (allJobsComplete) {
                logger.info("$this: graceful shutdown all tasks of connector $connectorId")
                return
            }
            delay(100)
        }

        container[connectorId]
            ?.cancelAll()
    }

    private fun ConnectorContainer.cancelAll() = requestJobMap
        .values
        .forEach {
            it.cancel()
        }

    companion object {
        private val shutdownMutex = Mutex()
    }
}

data class ConnectorContainer(
    val requestJobMap: ConcurrentHashMap<Long, Job>
)