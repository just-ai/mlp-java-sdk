package com.mlp.sdk.utils

import com.mlp.sdk.MlpServiceConfig
import com.mlp.sdk.MlpExecutionContext
import com.mlp.sdk.WithExecutionContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.time.Duration.ofMillis
import java.time.Instant.now
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class JobsContainer(
    private val config: MlpServiceConfig,
    override val context: MlpExecutionContext
) : WithExecutionContext {

    private val containers = ConcurrentHashMap<Long, ConnectorContainer>()

    fun put(connectorId: Long, requestId: Long, job: Job): Boolean {
        val connectorContainer = containers.computeIfAbsent(connectorId) {
            ConnectorContainer(ConcurrentHashMap<Long, Job>())
        }

        return if (connectorContainer.ableToProcessNewJobs.get()) {
            connectorContainer.requestJobMap[requestId] = job
            true
        } else {
            false
        }
    }

    fun remove(connectorId: Long, requestId: Long) {
        containers[connectorId]
            ?.requestJobMap
            ?.remove(requestId)
    }

    fun cancel(connectorId: Long) {
        containers[connectorId]
            ?.disableNewOnes()
            ?.cancelAll()
    }

    fun cancelAll() {
        containers.forEach {
            it.value.disableNewOnes().cancelAll()
        }
    }

    suspend fun gracefulShutdownByConnector(connectorId: Long) {
        val container = containers[connectorId] ?: return

        container.disableNewOnes()

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

    fun enableNewOnes(connectorId: Long) {
        containers[connectorId]
            ?.ableToProcessNewJobs?.set(true)
    }

    private fun ConnectorContainer.disableNewOnes() = this
        .also { it.ableToProcessNewJobs.set(false) }

    private fun ConnectorContainer.cancelAll() = requestJobMap
        .values
        .forEach(Job::cancel)
}

data class ConnectorContainer(
    val requestJobMap: ConcurrentHashMap<Long, Job>,
    val ableToProcessNewJobs: AtomicBoolean = AtomicBoolean(true)
)
