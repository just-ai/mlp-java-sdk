package com.mlp.sdk.utils

import com.mlp.sdk.MlpServiceConfig
import java.time.Duration.ofMillis
import java.time.Instant.now
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class JobsContainer(
    private val config: MlpServiceConfig,
) : WithLogger {

    private val containers = ConcurrentHashMap<Long, ConnectorContainer>()

    fun initContainer(connectorId: Long) {
        containers.computeIfAbsent(connectorId) { ConnectorContainer() }
        logger.info("$this: enable new tasks of connector $connectorId")
    }

    fun canProcessNewJobs(connectorId: Long): Boolean {
        val container = containers[connectorId] ?: return true
        return !container.disabledAllNewRequests.get()
    }

    fun put(connectorId: Long, requestId: Long, job: Job): Boolean {
        val connectorContainer = containers.computeIfAbsent(connectorId) { ConnectorContainer() }

        return if (!connectorContainer.disabledAllNewRequests.get()) {
            connectorContainer.requestJobMap[requestId] = job
            true
        } else false
    }

    fun remove(connectorId: Long, requestId: Long) {
        containers[connectorId]
            ?.requestJobMap
            ?.remove(requestId)
    }

    fun cancelRequest(connectorId: Long, requestId: Long) {
        val requestsMap = containers[connectorId]
            ?.requestJobMap
            ?: return
        val job = requestsMap.remove(requestId) ?: return
        job.cancel()
    }

    fun cancelAllForever() {
        containers.forEach {
            it.value.disabledAllNewRequests.set(true)
            it.value.cancelAll()
        }
    }

    suspend fun gracefulShutdownByConnector(connectorId: Long) {
        val container = containers[connectorId] ?: return

        val deadline = now() + ofMillis(config.shutdownConfig.actionConnectorMs)
        while (now() < deadline) {
            val allJobsComplete = container
                .requestJobMap
                .isEmpty()
            if (allJobsComplete) {
                logger.info("$this: graceful shutdown all tasks of connector $connectorId")
                return
            }
            delay(100)
        }

        container.cancelAll()
    }

    private fun ConnectorContainer.cancelAll() = requestJobMap
        .values
        .forEach(Job::cancel)

    companion object {
        private data class ConnectorContainer(
            val requestJobMap: ConcurrentHashMap<Long, Job> = ConcurrentHashMap(),
            val disabledAllNewRequests: AtomicBoolean = AtomicBoolean(false)
        )
    }
}
