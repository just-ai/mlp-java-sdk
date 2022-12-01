package com.platform.mpl.sdk

import com.platform.mpl.gate.ActionToGateProto
import com.platform.mpl.gate.ActionToGateProto.Builder
import com.platform.mpl.gate.ApiErrorProto
import com.platform.mpl.gate.BatchRequestProto
import com.platform.mpl.gate.BatchResponseProto
import com.platform.mpl.gate.ExtendedRequestProto
import com.platform.mpl.gate.ExtendedResponseProto
import com.platform.mpl.gate.FitRequestProto
import com.platform.mpl.gate.FitResponseProto
import com.platform.mpl.gate.GateToActionProto
import com.platform.mpl.gate.PayloadProto
import com.platform.mpl.gate.PredictRequestProto
import com.platform.mpl.gate.PredictResponseProto
import com.platform.mpl.sdk.CommonErrorCode.PROCESSING_EXCEPTION
import com.platform.mpl.sdk.State.Condition.ACTIVE
import com.platform.mpl.sdk.utils.JobsContainer
import com.platform.mpl.sdk.utils.WithLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors.newFixedThreadPool

class ActionTaskExecutor(
    val action: PlatformAction,
    val config: PlatformActionConfig
) : WithLogger, WithState(ACTIVE) {

    private val jobsContainer = JobsContainer(config)
    private val scope = CoroutineScope(newFixedThreadPool(config.threadPoolSize).asCoroutineDispatcher())
    internal lateinit var connectorsPool: ConnectorsPool

    fun predict(request: PredictRequestProto, requestId: Long, connectorId: Long) {
        launchAndStore(requestId, connectorId) {
            val responseBuilder = ActionToGateProto.newBuilder().setRequestId(requestId)
            val dataPayload = requireNotNull(request.data.asPayload) { "Payload data" }

            runCatching {
                when (val responsePayload = action.predict(dataPayload, request.config.asPayload)) {
                    is Payload -> responseBuilder.setPredict(responsePayload)
                    is PlatformResponseException -> throw responsePayload.exception
                }
            }.onFailure {
                logger.error("Error while processing predict request", it)
                responseBuilder.setError(it)
            }

            runCatching { connectorsPool.send(connectorId, responseBuilder.build()) }
                .onFailure { logger.error("Error while sending predict response", it) }
        }
    }

    fun fit(request: FitRequestProto, requestId: Long, connectorId: Long) {
        launchAndStore(requestId, connectorId) {
            val responseBuilder = ActionToGateProto.newBuilder().setRequestId(requestId)

            val trainPayload = requireNotNull(request.trainData.asPayload) { "trainData" }
            val targetsPayload = requireNotNull(request.targetsData.asPayload) { "targetsData" }
            val configPayload = requireNotNull(request.config.asPayload) { "config" }

            runCatching {
                action.fit(trainPayload, targetsPayload, configPayload)
                responseBuilder.setFit()
            }.onFailure {
                logger.error("Error while processing fit request", it)
                responseBuilder.setError(it)
            }

            runCatching { connectorsPool.send(connectorId, responseBuilder.build()) }
                .onFailure { logger.error("Error while sending fit response", it) }
        }
    }

    fun ext(request: ExtendedRequestProto, requestId: Long, connectorId: Long) {
        launchAndStore(requestId, connectorId) {
            val responseBuilder = ActionToGateProto.newBuilder().setRequestId(requestId)

            val methodName = requireNotNull(request.methodName) { "methodName" }
            val params =
                requireNotNull(request.paramsMap.mapValues { requireNotNull(it.value.asPayload) }) { "paramsMap" }

            runCatching {
                when (val responsePayload = action.ext(methodName, params)) {
                    is Payload -> responseBuilder.setExt(responsePayload)
                    is PlatformResponseException -> throw responsePayload.exception
                }
            }.onFailure {
                logger.error("Error while processing ext request", it)
                responseBuilder.setError(it)
            }

            runCatching { connectorsPool.send(connectorId, responseBuilder.build()) }
                .onFailure { logger.error("Error while sending ext response", it) }
        }
    }

    fun batch(request: BatchRequestProto, requestId: Long, connectorId: Long) {
        launchAndStore(requestId, connectorId) {
            val responseBuilder = ActionToGateProto.newBuilder().setRequestId(requestId)

            val data = request.dataList

            if (!data.all(GateToActionProto::hasPredict)) {
                sendException(
                    connectorId,
                    responseBuilder,
                    IllegalArgumentException("Batch request should contain only predict requests")
                )
                return@launchAndStore
            }

            val payloadData = data.map {
                it.predict.data.asPayload
            }

            if (payloadData.any { it == null }) {
                sendException(
                    connectorId,
                    responseBuilder,
                    IllegalArgumentException("Payload data is null")
                )
                return@launchAndStore
            }

            val requestsIdes = data.map { it.requestId }

            runCatching {
                val responses = action.batch(payloadData.map(::requireNotNull))
                responseBuilder.setBatch(responses, requestsIdes)
            }.onFailure {
                logger.error("Error while processing batch request", it)
                responseBuilder.setError(it)
            }

            runCatching { connectorsPool.send(connectorId, responseBuilder.build()) }
                .onFailure { logger.error("Error while sending batch response", it) }
        }
    }

    fun cancelAll() {
        logger.info("$this: cancel all tasks")
        jobsContainer.cancelAll()
    }

    fun cancelAll(connectorId: Long) {
        logger.info("$this: cancelling all tasks of connector $connectorId ...")
        jobsContainer.cancel(connectorId)
        logger.info("$this: cancelled all tasks of connector $connectorId")
    }

    suspend fun gracefulShutdownAll(connectorId: Long) {
        logger.info("$this: graceful shutting down all tasks of connector $connectorId ...")
        jobsContainer.gracefulShutdownByConnector(connectorId)
        logger.info("$this: graceful shut down all tasks of connector $connectorId")
    }

    private suspend fun sendException(connectorId: Long, responseBuilder: Builder, throwable: Throwable) {
        responseBuilder.setError(throwable)
        logger.error("Exception while handle request", throwable)
        connectorsPool.send(connectorId, responseBuilder.build())
    }

    private fun launchAndStore(
        requestId: Long,
        connectorId: Long,
        block: suspend () -> Unit
    ) {
        val job = scope.launch(start = CoroutineStart.LAZY) {
            block.invoke()
        }

        job.invokeOnCompletion {
            jobsContainer.remove(connectorId, requestId)
        }

        val added = jobsContainer.put(connectorId, requestId, job)

        if (added) {
            job.start()
        } else {
            job.cancel()
        }
    }

    override fun toString() = "ActionTaskExecutor(action=$action)"
}

internal val Payload.asProto
    get() = PayloadProto.newBuilder().apply {
        json = data
        dataType?.let { dataType = it }
    }

private val PayloadProto?.asPayload: Payload?
    get() = this?.let { Payload(dataType, json) }

private fun Builder.setPredict(prediction: Payload) =
    setPredict(PredictResponseProto.newBuilder().setData(prediction.asProto))

private fun Builder.setFit() =
    setFit(FitResponseProto.newBuilder())

private fun Builder.setExt(extResult: Payload) =
    setExt(ExtendedResponseProto.newBuilder().setData(extResult.asProto))

private fun Builder.setBatch(batchResult: List<PlatformResponse>, requestsIdes: List<Long>): Builder {
    require(batchResult.size == requestsIdes.size) { "Batch responses size must be equal to requests size" }
    val actionToGateProtos = batchResult.zip(requestsIdes)
        .map { (data, requestId) ->
            val builder = ActionToGateProto.newBuilder().setRequestId(requestId)
            when (data) {
                is Payload -> builder.setPredict(data)
                is PlatformResponseException -> builder.setError(data.exception)
            }
            builder.build()
        }

    return setBatch(BatchResponseProto.newBuilder().addAllData(actionToGateProtos))
}

private fun Builder.setError(throwable: Throwable) = when (throwable) {
    is PlatformException -> setError(
        ApiErrorProto.newBuilder()
            .setCode(throwable.error.errorCode.code)
            .setMessage(throwable.error.errorCode.message)
            .setStatus(throwable.error.errorCode.status)
            .putAllArgs(throwable.error.args)
    )

    else -> setError(
        ApiErrorProto.newBuilder()
            .setCode(PROCESSING_EXCEPTION.code)
            .setMessage(PROCESSING_EXCEPTION.message)
            .setStatus(PROCESSING_EXCEPTION.status)
            .putArgs("message", throwable.message)
    )
}
