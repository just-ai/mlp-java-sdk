package com.mlp.sdk

import com.mlp.sdk.State.Condition.ACTIVE
import com.mlp.sdk.utils.JobsContainer
import java.time.Duration.ofMillis
import java.time.Instant
import java.time.Instant.now
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors.newFixedThreadPool
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.MDC

class TaskExecutor(
    val action: MlpService,
    val config: MlpServiceConfig,
    dispatcher: CoroutineDispatcher?,
    override val context: MlpExecutionContext
) : WithState(ACTIVE) {

    private val jobsContainer = JobsContainer(config)
    private val channelsContainer = ConcurrentHashMap<Long, Channel<PayloadWithConfig>>() // requestId to Channel
    private val scope = CoroutineScope(
        SupervisorJob() + (dispatcher ?: newFixedThreadPool(config.threadPoolSize).asCoroutineDispatcher())
    )
    internal lateinit var connectorsPool: ConnectorsPool

    fun canProcessNewJobs(connectorId: Long) =
        jobsContainer.canProcessNewJobs(connectorId)

    fun runAsync(
        requestContext: RequestContext,
        processor: suspend (MlpService) -> Unit,
    ) {
        val ctx = MDC.getCopyOfContextMap()
        val job = scope.launch(start = CoroutineStart.LAZY) {
            withContext(MDCContext(ctx)) {
                BillingUnitsThreadLocal.clearAll()

                processor(action)
            }
        }

        job.invokeOnCompletion {
            jobsContainer.remove(requestContext)
        }

        val added = jobsContainer.put(requestContext, job)

        if (added) {
            job.start()
        } else {
            job.cancel()
        }
    }

    fun runAsyncWithChannel(
        requestContext: RequestContext,
        initializer: suspend (MlpService, Channel<PayloadWithConfig>) -> Unit,
        processor: (Channel<PayloadWithConfig>) -> Unit,
    ) {
        val requestId = requestContext.gateRequestId

        val channel = channelsContainer.computeIfAbsent(requestId) {
            val channel = Channel<PayloadWithConfig>()
            @OptIn(ExperimentalCoroutinesApi::class)
            channel.invokeOnClose {
                channelsContainer.remove(requestId)
            }
            runAsync(requestContext) {
                initializer(action, channel)
            }
            channel
        }

        processor(channel)
    }

    fun cancelRequest(connectorId: Long, requestId: Long) {
        jobsContainer.cancelRequest(connectorId, requestId)
    }

    /**
     * Запрос открыл стрим: кадры уйдут вне его job (predict отдал MlpPartialBinaryResponse).
     * До финального кадра такой запрос считается активным при дренаже остановки.
     */
    fun streamOpened(connectorId: Long, requestId: Long) {
        jobsContainer.streamOpened(connectorId, requestId)
    }

    /** Стрим запроса завершён: ушёл финальный кадр, обычный ответ или ошибка. */
    fun streamFinished(connectorId: Long, requestId: Long) {
        jobsContainer.streamFinished(connectorId, requestId)
    }

    /** По стриму ушёл очередной кадр: запись считается живой, а не брошенной. */
    fun streamTouched(connectorId: Long, requestId: Long) {
        jobsContainer.streamTouched(connectorId, requestId)
    }

    fun initContainer(connectorId: Long) {
        logger.info("$this: enable new requests for connector $connectorId")
        jobsContainer.initContainer(connectorId)
    }

    fun disableNewJobs(connectorId: Long) {
        logger.info("$this: disable new requests for connector $connectorId")
        jobsContainer.disableNewJobs(connectorId)
    }

    fun cancelAll() {
        logger.info("$this: cancel all tasks")
        runCatching { jobsContainer.cancelAllForever() }
    }

    suspend fun gracefulShutdownAll(
        connectorId: Long,
        deadline: Instant = now() + ofMillis(config.shutdownConfig.actionConnectorMs),
        abortEarly: () -> Boolean = { false },
    ) {
        logger.info("$this: graceful shutting down all tasks of connector $connectorId ...")
        runCatching { jobsContainer.gracefulShutdownByConnector(connectorId, deadline, abortEarly) }
            .onFailure { logger.error("$this: error while graceful shutting down tasks of connector $connectorId", it) }
        logger.info("$this: graceful shut down all tasks of connector $connectorId")
    }

    override fun toString() = "ActionTaskExecutor(action=$action)"
}
