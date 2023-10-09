package com.mlp.sdk

import com.mlp.gate.ApiErrorProto
import com.mlp.gate.BatchPayloadResponseProto
import com.mlp.gate.BatchRequestProto
import com.mlp.gate.BatchResponseProto
import com.mlp.gate.ExtendedRequestProto
import com.mlp.gate.ExtendedResponseProto
import com.mlp.gate.FitRequestProto
import com.mlp.gate.FitResponseProto
import com.mlp.gate.PayloadProto
import com.mlp.gate.PredictRequestProto
import com.mlp.gate.PredictResponseProto
import com.mlp.gate.ServiceToGateProto
import com.mlp.gate.ServiceToGateProto.Builder
import com.mlp.sdk.CommonErrorCode.PROCESSING_EXCEPTION
import com.mlp.sdk.State.Condition.ACTIVE
import com.mlp.sdk.utils.JobsContainer
import java.util.concurrent.Executors.newFixedThreadPool
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.MDC

class TaskExecutor (
    val action: MlpService,
    val config: MlpServiceConfig,
    dispatcher: CoroutineDispatcher?,
    override val context: MlpExecutionContext
) : WithExecutionContext, WithState(ACTIVE) {

    private val jobsContainer = JobsContainer(config, context)
    private val scope = CoroutineScope(dispatcher ?: newFixedThreadPool(config.threadPoolSize).asCoroutineDispatcher())
    internal lateinit var connectorsPool: ConnectorsPool

    fun predict(request: PredictRequestProto, requestId: Long, connectorId: Long, tracker: TimeTracker) {
        launchAndStore(requestId, connectorId) {
            val responseBuilder = ServiceToGateProto.newBuilder().setRequestId(requestId)
            val dataPayload = requireNotNull(request.data.asPayload) { "Payload data" }

            runCatching {
                when (val responsePayload = action.predict(dataPayload, request.config.asPayload)) {
                    is Payload -> responseBuilder.setPredict(responsePayload)
                    is RawPayload -> responseBuilder.setPredict(responsePayload.asPayload)
                    is MlpResponseException -> throw responsePayload.exception
                    is MlpPartialBinaryResponse -> return@launchAndStore
                    // если partialResponse, то просто ничего не делаем. Респонзы будет отправлять сам сервис.
                }
            }.onFailure {
                logger.error("Error while processing predict request", it)
                responseBuilder.setError(it.asErrorProto)
            }

            val elapsed = System.currentTimeMillis() - tracker.startTime
            responseBuilder.putHeaders("Z-Server-Time", elapsed.toString())
            runCatching { connectorsPool.send(connectorId, responseBuilder.build()) }
                .onFailure { logger.error("Error while sending predict response", it) }
        }
    }

    fun fit(request: FitRequestProto, requestId: Long, connectorId: Long) {
        launchAndStore(requestId, connectorId) {
            val responseBuilder = ServiceToGateProto.newBuilder().setRequestId(requestId)

            val trainPayload = request.trainData.asPayload
            val targetsPayload = request.targetsData?.asPayload
            val configPayload = request.config?.asPayload
            val modelDir = request.modelDir

            runCatching {
                action.fit(trainPayload, targetsPayload, configPayload, modelDir, request.previousModelDir,
                    request.targetServiceInfo,
                    request.datasetInfo
                )
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
            val responseBuilder = ServiceToGateProto.newBuilder().setRequestId(requestId)

            val methodName = requireNotNull(request.methodName) { "methodName" }
            val params =
                requireNotNull(request.paramsMap.mapValues { requireNotNull(it.value.asPayload) }) { "paramsMap" }

            runCatching {
                when (val responsePayload = action.ext(methodName, params)) {
                    is Payload -> responseBuilder.setExt(responsePayload)
                    is RawPayload -> responseBuilder.setExt(responsePayload.asPayload)
                    is MlpResponseException -> throw responsePayload.exception
                    is MlpPartialBinaryResponse -> throw NotImplementedError()
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
            val responseBuilder = ServiceToGateProto.newBuilder().setRequestId(requestId)

            val data = request.dataList

            val payloadData = data.map { it.data.asPayload }
            val requestsIdes = data.map { it.requestId }

            runCatching {
                val responses = action.batch(payloadData, request.config.asPayload)
                responseBuilder.setBatch(responses, requestsIdes)
            }.onFailure {
                logger.error("Error while processing batch request", it)
                responseBuilder.setError(it.asErrorProto)
            }

            runCatching { connectorsPool.send(connectorId, responseBuilder.build()) }
                .onFailure { logger.error("Error while sending batch response", it) }
        }
    }

    fun enableNewTasks(id: Long) {
        logger.info("$this: enable new requests for connector $id")
        jobsContainer.enableNewOnes(id)
    }

    fun cancelAll() {
        logger.info("$this: cancel all tasks")
        runCatching { jobsContainer.cancelAll() }
    }

    fun cancelAll(connectorId: Long) {
        logger.info("$this: cancelling all tasks of connector $connectorId ...")
        runCatching { jobsContainer.cancel(connectorId) }
        logger.info("$this: cancelled all tasks of connector $connectorId")
    }

    suspend fun gracefulShutdownAll(connectorId: Long) {
        logger.info("$this: graceful shutting down all tasks of connector $connectorId ...")
        runCatching { jobsContainer.gracefulShutdownByConnector(connectorId) }
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
        val ctx = MDC.getCopyOfContextMap()
        val job = scope.launch(start = CoroutineStart.LAZY) {
            withContext(MDCContext(ctx)) {
                block.invoke()
            }
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

private fun Builder.setPredict(prediction: Payload) {
    BillingUnitsThreadLocal.getUnits()?.also {
        putHeaders("Z-custom-billing", it.toString())
    }
    setPredict(PredictResponseProto.newBuilder().setData(prediction.asProto))
}

private fun Builder.setFit() =
    setFit(FitResponseProto.newBuilder())

private fun Builder.setExt(extResult: Payload) =
    setExt(ExtendedResponseProto.newBuilder().setData(extResult.asProto))

private fun Builder.setBatch(batchResult: List<MlpResponse>, requestsIdes: List<Long>): Builder {
    require(batchResult.size == requestsIdes.size) { "Batch responses size must be equal to requests size" }
    val actionToGateProtos = batchResult.zip(requestsIdes)
        .map { (data, requestId) ->
            val builder = BatchPayloadResponseProto.newBuilder().setRequestId(requestId)
            when (data) {
                is Payload -> builder.setPredict(PredictResponseProto.newBuilder().setData(data.asProto))
                is MlpResponseException -> builder.setError(data.exception.asErrorProto)
            }
            builder.build()
        }

    return setBatch(BatchResponseProto.newBuilder().addAllData(actionToGateProtos))
}

private val Throwable.asErrorProto
    get() = when (this) {
        is MlpException ->
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
                .putArgs("message", message ?: "")

    }
