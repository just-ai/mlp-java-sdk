package com.mlp.sdk.utils

import com.mlp.sdk.MlpServiceConfig
import com.mlp.sdk.MlpExecutionContext
import com.mlp.sdk.WithExecutionContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.time.Duration.ofMillis
import java.time.Instant.now
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentHashMap.KeySetView
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.Long.Companion.MAX_VALUE
import kotlin.Long.Companion.MIN_VALUE

class JobsContainer(
    private val config: MlpServiceConfig,
    override val context: MlpExecutionContext
) : WithExecutionContext {

    private val containers = ConcurrentHashMap<Long, ConnectorContainer>()

    fun isAbleProcessNewJobs(connectorId: Long, grpcChannelId: Long): Boolean {
        val container = containers.get(connectorId) ?: return true
        return !container.disabledAllNewRequests.get() && container.isGrpcChannelActual(grpcChannelId)
    }

    fun put(connectorId: Long, grpcChannelId: Long, requestId: Long, job: Job): Boolean {
        val connectorContainer = containers.computeIfAbsent(connectorId) {
            ConnectorContainer(ConcurrentHashMap<Long, Job>())
        }

        return if (isAbleProcessNewJobs(connectorId, grpcChannelId)) {
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

    fun cancel(connectorId: Long, grpcChannelId: Long) {
        containers[connectorId]
            ?.disableNewOnes(grpcChannelId)
            ?.cancelAll()
    }

    fun cancelAllForever() {
        containers.forEach {
            it.value.disabledAllNewRequests.set(true)
            it.value.cancelAll()
        }
    }

    suspend fun gracefulShutdownByConnector(connectorId: Long, grpcChannelId: Long) {
        val container = containers[connectorId] ?: return

        container.disableNewOnes(grpcChannelId)

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

    fun enableNewOnes(connectorId: Long, grpcChannelId: Long) {
        containers[connectorId]
            ?.let {
                it.actualGrpcChannels += grpcChannelId
                logger.info("$this: enable new tasks of connector $connectorId grpc channel $grpcChannelId")
            }
    }

    private fun ConnectorContainer.disableNewOnes(grpcChannelId: Long) = this
        .also {
            it.actualGrpcChannels.remove(grpcChannelId)
            logger.info("$this: disable new tasks of connector grpc channel $grpcChannelId")
        }

    private fun ConnectorContainer.isGrpcChannelActual(grpcChannelId: Long): Boolean {
        return grpcChannelId in actualGrpcChannels
    }

    private fun ConnectorContainer.cancelAll() = requestJobMap
        .values
        .forEach(Job::cancel)
}

data class ConnectorContainer(
    val requestJobMap: ConcurrentHashMap<Long, Job>,
    val actualGrpcChannels: KeySetView<Long, Boolean> = ConcurrentHashMap.newKeySet(),
    val disabledAllNewRequests: AtomicBoolean = AtomicBoolean(false)
)
