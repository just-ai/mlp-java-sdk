package com.mpl.sdk

import com.mpl.gate.ActionToGateProto
import com.mpl.gate.ActionToGateProto.Builder
import com.mpl.gate.ApiErrorProto
import com.mpl.gate.BatchRequestProto
import com.mpl.gate.BatchResponseProto
import com.mpl.gate.ExtendedRequestProto
import com.mpl.gate.ExtendedResponseProto
import com.mpl.gate.FitRequestProto
import com.mpl.gate.FitResponseProto
import com.mpl.gate.PayloadProto
import com.mpl.gate.PredictRequestProto
import com.mpl.gate.PredictResponseProto
import com.mpl.gate.SingleBatchPredictResponseProto
import com.mpl.sdk.CommonErrorCode.PROCESSING_EXCEPTION
import com.mpl.sdk.State.Condition.ACTIVE
import com.mpl.sdk.utils.JobsContainer
import com.mpl.sdk.utils.WithLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors.newFixedThreadPool

class ActionTaskExecutor(
    val action: MplAction,
    val config: MplActionConfig
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
                    is MplResponseException -> throw responsePayload.exception
                }
            }.onFailure {
                logger.error("Error while processing predict request", it)
                responseBuilder.setError(it.asErrorProto)
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
                responseBuilder.setError(it.asErrorProto)
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
                    is MplResponseException -> throw responsePayload.exception
                }
            }.onFailure {
                logger.error("Error while processing ext request", it)
                responseBuilder.setError(it.asErrorProto)
            }

            runCatching { connectorsPool.send(connectorId, responseBuilder.build()) }
                .onFailure { logger.error("Error while sending ext response", it) }
        }
    }

    fun batch(request: BatchRequestProto, requestId: Long, connectorId: Long) {
        launchAndStore(requestId, connectorId) {
            val responseBuilder = ActionToGateProto.newBuilder().setRequestId(requestId)

            val data = request.dataList

            val payloadData = data.map {
                BatchPayload(it.predict.data.asPayload, it.predict.config.asPayload)
            }

            val requestsIdes = data.map { it.requestId }

            runCatching {
                val responses = action.batch(payloadData.map(::requireNotNull))
                responseBuilder.setBatch(responses, requestsIdes)
            }.onFailure {
                logger.error("Error while processing batch request", it)
                responseBuilder.setError(it.asErrorProto)
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
        responseBuilder.setError(throwable.asErrorProto)
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

private val PayloadProto.asPayload: Payload
    get() = Payload(dataType, json)

private fun Builder.setPredict(prediction: Payload) =
    setPredict(PredictResponseProto.newBuilder().setData(prediction.asProto))

private fun Builder.setFit() =
    setFit(FitResponseProto.newBuilder())

private fun Builder.setExt(extResult: Payload) =
    setExt(ExtendedResponseProto.newBuilder().setData(extResult.asProto))

private fun Builder.setBatch(batchResult: List<MplResponse>, requestsIdes: List<Long>): Builder {
    require(batchResult.size == requestsIdes.size) { "Batch responses size must be equal to requests size" }
    val actionToGateProtos = batchResult.zip(requestsIdes)
        .map { (data, requestId) ->
            val builder = SingleBatchPredictResponseProto.newBuilder().setRequestId(requestId)
            when (data) {
                is Payload -> builder.setPredict(PredictResponseProto.newBuilder().setData(data.asProto))
                is MplResponseException -> builder.setError(data.exception.asErrorProto)
            }
            builder.build()
        }

    return setBatch(BatchResponseProto.newBuilder().addAllData(actionToGateProtos))
}

private val Throwable.asErrorProto
    get() = when (this) {
        is MplException ->
            ApiErrorProto.newBuilder()
                .setCode(error.errorCode.code)
                .setMessage(error.errorCode.message)
                .setStatus(error.errorCode.status)
                .putAllArgs(error.args)

        else ->
            ApiErrorProto.newBuilder()
                .setCode(PROCESSING_EXCEPTION.code)
                .setMessage(PROCESSING_EXCEPTION.message)
                .setStatus(PROCESSING_EXCEPTION.status)
                .putArgs("message", message)

    }
