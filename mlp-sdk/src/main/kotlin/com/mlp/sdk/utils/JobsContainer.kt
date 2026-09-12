package com.mlp.sdk.utils

import com.mlp.sdk.MlpServiceConfig
import com.mlp.sdk.RequestContext
import java.time.Duration.ofMillis
import java.time.Instant
import java.time.Instant.now
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class JobsContainer(
    private val config: MlpServiceConfig,
) : WithLogger {

    private val containers = ConcurrentHashMap<Long, ConnectorContainer>()

    /**
     * Вызывается при (пере)подключении коннектора: контейнер снова принимает задачи.
     * Сброс флага обязателен — после остановки, инициированной гейтом, тот же connectorId
     * переподключается, и без сброса он навсегда остался бы закрытым для новых запросов.
     */
    fun initContainer(connectorId: Long) {
        containers.computeIfAbsent(connectorId) { ConnectorContainer() }
            .disabledAllNewRequests.set(false)
        logger.info("$this: enable new tasks of connector $connectorId")
    }

    /**
     * Закрывает приём новых задач коннектора, не трогая уже выполняющиеся.
     * Первый шаг остановки: пока идёт дренаж, гейт не должен получить ответ
     * на запрос, который мы уже не собираемся обрабатывать.
     */
    fun disableNewJobs(connectorId: Long) {
        containers.computeIfAbsent(connectorId) { ConnectorContainer() }
            .disabledAllNewRequests.set(true)
        logger.info("$this: disable new tasks of connector $connectorId")
    }

    fun canProcessNewJobs(connectorId: Long): Boolean {
        val container = containers[connectorId] ?: return true
        return !container.disabledAllNewRequests.get()
    }

    /**
     * Отмечает, что по запросу открыт стрим, кадры которого сервис шлёт сам — вне job запроса
     * (predict вернул MlpPartialBinaryResponse). Для такого запроса завершение job ничего не
     * говорит о завершении работы, поэтому дренаж остановки обязан ждать ещё и финальный кадр.
     */
    fun streamOpened(connectorId: Long, requestId: Long) {
        containers.computeIfAbsent(connectorId) { ConnectorContainer() }
            .openStreams
            .add(requestId)
    }

    /** Финальный кадр стрима ушёл (или запрос завершён иначе) — запрос больше не держит остановку. */
    fun streamFinished(connectorId: Long, requestId: Long) {
        containers[connectorId]
            ?.openStreams
            ?.remove(requestId)
    }

    fun put(requestContext: RequestContext, job: Job): Boolean {
        val connectorContainer = containers.computeIfAbsent(requestContext.connectorId) { ConnectorContainer() }

        return if (!connectorContainer.disabledAllNewRequests.get()) {
            connectorContainer.requestJobMap[requestContext.gateRequestId] = job
            true
        } else false
    }

    fun remove(requestContext: RequestContext) {
        containers[requestContext.connectorId]
            ?.requestJobMap
            ?.remove(requestContext.gateRequestId)
    }

    fun cancelRequest(connectorId: Long, requestId: Long) {
        val container = containers[connectorId] ?: return
        container.openStreams.remove(requestId)
        val job = container.requestJobMap.remove(requestId) ?: return
        job.cancel()
    }

    fun cancelAllForever() {
        containers.forEach {
            it.value.disabledAllNewRequests.set(true)
            it.value.cancelAll()
        }
    }

    /**
     * Ждёт завершения активных задач коннектора до [deadline] и отменяет то, что не успело.
     *
     * Ждать приходится двух вещей: job запросов и открытых стримов. Стрим учитывается отдельно,
     * потому что сервис может отдать из predict MlpPartialBinaryResponse и досылать кадры из своей
     * корутины: job такого запроса завершается сразу, и по одному requestJobMap дренаж закончился бы
     * мгновенно, оборвав стрим half-close'ом на середине.
     *
     * [deadline] приходит снаружи, потому что бюджет остановки один на весь сценарий
     * (stopServing → дренаж → half-close), а не на этот вызов.
     * [abortEarly] прекращает ожидание, когда канал уже закрыт гейтом: ответы всё равно
     * некуда отдавать, и держать остановку до конца бюджета бессмысленно.
     */
    suspend fun gracefulShutdownByConnector(
        connectorId: Long,
        deadline: Instant = now() + ofMillis(config.shutdownConfig.actionConnectorMs),
        abortEarly: () -> Boolean = { false },
    ) {
        val container = containers[connectorId] ?: return

        while (now() < deadline) {
            if (container.requestJobMap.isEmpty() && container.openStreams.isEmpty()) {
                logger.info("$this: graceful shutdown all tasks of connector $connectorId")
                return
            }
            if (abortEarly()) {
                logger.warn(
                    "$this: grpc channel of connector $connectorId is already closed, " +
                            "cancelling ${container.requestJobMap.size} in-flight task(s) and dropping " +
                            "${container.openStreams.size} unfinished stream(s) without waiting for the deadline"
                )
                container.cancelAll()
                return
            }
            delay(50)
        }

        logger.warn(
            "$this: graceful shutdown budget of connector $connectorId is over, " +
                    "cancelling ${container.requestJobMap.size} in-flight task(s) and dropping " +
                    "${container.openStreams.size} unfinished stream(s)"
        )
        container.cancelAll()
    }

    private fun ConnectorContainer.cancelAll() {
        requestJobMap.values.forEach(Job::cancel)
        // Стрим отменить нечем — его ведёт корутина сервиса. Снимаем с учёта, чтобы остановка
        // не ждала кадр, которого уже никто не отправит.
        openStreams.clear()
    }

    companion object {
        private data class ConnectorContainer(
            val requestJobMap: ConcurrentHashMap<Long, Job> = ConcurrentHashMap(),
            /** gateRequestId запросов, чьи кадры сервис досылает сам, вне job. */
            val openStreams: MutableSet<Long> = ConcurrentHashMap.newKeySet(),
            val disabledAllNewRequests: AtomicBoolean = AtomicBoolean(false)
        )
    }
}
