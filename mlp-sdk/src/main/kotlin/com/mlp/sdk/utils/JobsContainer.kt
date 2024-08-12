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
import java.util.concurrent.atomic.AtomicLong
import kotlin.Long.Companion.MAX_VALUE
import kotlin.Long.Companion.MIN_VALUE

class JobsContainer(
    private val config: MlpServiceConfig,
    override val context: MlpExecutionContext
) : WithExecutionContext {

    private val containers = ConcurrentHashMap<Long, ConnectorContainer>()

    fun isAbleProcessNewJobs(connectorId: Long): Boolean {
        val container = containers.get(connectorId) ?: return true
        return container.ableToProcessNewJobs.get()
    }

    fun put(connectorId: Long, requestId: Long, job: Job): Boolean {
        val connectorContainer = containers.computeIfAbsent(connectorId) {
            ConnectorContainer(ConcurrentHashMap<Long, Job>())
        }

        return if (isAbleProcessNewJobs(connectorId)) {
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
            it.value.disableNewOnes(MAX_VALUE)?.cancelAll()
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
                if (it.isGrpcChannelOutdated(grpcChannelId))
                    return
                
                it.ableToProcessNewJobs.set(true)
                logger.info("$this: enable new tasks of connector $connectorId with actual id is ${it.actualGrpcChannel.get()}")
            }
    }
    
    private fun ConnectorContainer.disableNewOnes(grpcChannelId: Long) = this
        .let {
            if (it.isGrpcChannelOutdated(grpcChannelId))
                return@let null

            it.ableToProcessNewJobs.set(false)
            logger.info("$this: disable new tasks of connector with actual id is ${it.actualGrpcChannel.get()}")
            it
        }

    private fun ConnectorContainer.isGrpcChannelOutdated(grpcChannelId: Long): Boolean {
        while (true) {
            val localActualGrpcChannel = actualGrpcChannel.get()

            if (localActualGrpcChannel > grpcChannelId) {
                // no changed of able property
                return true
            }

            if (localActualGrpcChannel < grpcChannelId) {
                val changed = actualGrpcChannel.compareAndSet(localActualGrpcChannel, grpcChannelId)
                if (changed) break
            }

            break
        }

        return false
    }

    private fun ConnectorContainer.cancelAll() = requestJobMap
        .values
        .forEach(Job::cancel)
}

data class ConnectorContainer(
    val requestJobMap: ConcurrentHashMap<Long, Job>,
    val actualGrpcChannel: AtomicLong = AtomicLong(MIN_VALUE),
    val ableToProcessNewJobs: AtomicBoolean = AtomicBoolean(true)
)
