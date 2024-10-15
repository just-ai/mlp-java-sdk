package com.mlp.sdk

import com.mlp.gate.ApiErrorProto
import com.mlp.gate.BatchPayloadResponseProto
import com.mlp.gate.BatchRequestProto
import com.mlp.gate.BatchResponseProto
import com.mlp.gate.ExtendedRequestProto
import com.mlp.gate.ExtendedResponseProto
import com.mlp.gate.FitRequestProto
import com.mlp.gate.FitResponseProto
import com.mlp.gate.FitStatusProto
import com.mlp.gate.PartialPredictRequestProto
import com.mlp.gate.PartialPredictResponseProto
import com.mlp.gate.PayloadProto
import com.mlp.gate.PredictRequestProto
import com.mlp.gate.PredictResponseProto
import com.mlp.gate.ServiceToGateProto
import com.mlp.gate.ServiceToGateProto.Builder
import com.mlp.sdk.CommonErrorCode.PROCESSING_EXCEPTION
import com.mlp.sdk.State.Condition.ACTIVE
import com.mlp.sdk.utils.JobsContainer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.MDC
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors.newFixedThreadPool

class TaskExecutor(
    val action: MlpService,
    val config: MlpServiceConfig,
    dispatcher: CoroutineDispatcher?,
    override val context: MlpExecutionContext
) : WithExecutionContext, WithState(ACTIVE) {

    private val jobsContainer = JobsContainer(config, context)
    private val channelsContainer = ConcurrentHashMap<Long, Channel<PayloadWithConfig>>() // requestId to Channel
    private val scope = CoroutineScope( SupervisorJob() + (dispatcher ?: newFixedThreadPool(config.threadPoolSize).asCoroutineDispatcher()))
    internal lateinit var connectorsPool: ConnectorsPool

    fun isAbleProcessNewJobs(connectorId: Long, grpcChannelId: Long) =
        jobsContainer.isAbleProcessNewJobs(connectorId, grpcChannelId)

    fun predict(request: PredictRequestProto, requestId: Long, connectorId: Long, grpcChannelId: Long, tracker: TimeTracker) {
        launchAndStore(requestId, connectorId, grpcChannelId) {
            val responseBuilder = ServiceToGateProto.newBuilder().setRequestId(requestId)
            val dataPayload = requireNotNull(request.data.asPayload) { "Payload data" }

            runCatching {
                when (val responsePayload = action.predict(dataPayload, request.config.asPayload)) {
                    is PayloadInterface -> responseBuilder.setPredict(responsePayload)
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

    fun streamPredict(request: PartialPredictRequestProto, requestId: Long, connectorId: Long, grpcChannelId: Long) {
        val channel = channelsContainer.computeIfAbsent(requestId) {
            val channel = Channel<PayloadWithConfig>()
            launchAndStore(requestId, connectorId, grpcChannelId) {
                runCatching {
                    action.streamPredictRaw(channel.receiveAsFlow()).onStart {
                        logger.info("requestId: $requestId Start processing stream flow")
                    }.onCompletion {
                        logger.info("requestId: $requestId Finish processing stream flow")
                        channel.close(it)
                        channelsContainer.remove(requestId)
                    }.catch {
                        logger.error("requestId: $requestId Error while processing stream predict request", it)
                        val responseBuilder = ServiceToGateProto.newBuilder().setRequestId(requestId)
                        responseBuilder.setError(it.asErrorProto)
                        runCatching { connectorsPool.send(connectorId, responseBuilder.build()) }
                            .onFailure { logger.error("Error while sending predict response", it) }
                    }.collect { response ->
                        val responseBuilder = ServiceToGateProto.newBuilder().setRequestId(requestId)
                        responseBuilder.setPartialPredict(response.payload, response.last)
                        runCatching { connectorsPool.send(connectorId, responseBuilder.build()) }
                            .onFailure { logger.error("Error while sending predict response", it) }
                    }
                }.onFailure {
                    logger.error("Error while processing predict request", it)
                    channel.close(it)
                    channelsContainer.remove(requestId)
                    val responseBuilder = ServiceToGateProto.newBuilder().setRequestId(requestId)
                    responseBuilder.setError(it.asErrorProto)
                    runCatching { connectorsPool.send(connectorId, responseBuilder.build()) }
                        .onFailure { logger.error("Error while sending predict response", it) }
                }
            }
            channel
        }

        if (request.hasData()) {
            val dataPayload = requireNotNull(request.data?.asPayloadInterface) { "Payload data" }
            val config = if (request.config == request.config.defaultInstanceForType) null else request.config?.asPayload
            runBlocking { channel.send(PayloadWithConfig(dataPayload, config)) }
        }

        if (request.finish) channel.close()
    }

    fun fit(request: FitRequestProto, requestId: Long, connectorId: Long, grpcChannelId: Long) {
        launchAndStore(requestId, connectorId, grpcChannelId) {
            val responseBuilder = ServiceToGateProto.newBuilder().setRequestId(requestId)

            val trainPayload = request.trainData.asPayload
            val targetsPayload = request.targetsData?.asPayload
            val configPayload = request.config?.asPayload
            val modelDir = request.modelDir

            runCatching {
                val percentageConsumer: suspend (Int) -> Unit = { percentage ->
                    runCatching {
                        val status = FitStatusProto.newBuilder().setPercentage(percentage).build()
                        val proto = ServiceToGateProto.newBuilder().setRequestId(requestId).setFitStatus(status).build()
                        connectorsPool.send(connectorId, proto)
                    }
                }
                action.fit(
                    trainPayload, targetsPayload, configPayload, modelDir, request.previousModelDir,
                    request.targetServiceInfo,
                    request.datasetInfo,
                    percentageConsumer
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

    fun ext(request: ExtendedRequestProto, requestId: Long, connectorId: Long, grpcChannelId: Long) {
        launchAndStore(requestId, connectorId, grpcChannelId) {
            val responseBuilder = ServiceToGateProto.newBuilder().setRequestId(requestId)

            val methodName = requireNotNull(request.methodName) { "methodName" }
            val params =
                requireNotNull(request.paramsMap.mapValues { requireNotNull(it.value.asPayload) }) { "paramsMap" }

            runCatching {
                when (val responsePayload = action.ext(methodName, params)) {
                    is PayloadInterface -> responseBuilder.setExt(responsePayload)
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

    fun batch(request: BatchRequestProto, requestId: Long, connectorId: Long, grpcChannelId: Long) {
        launchAndStore(requestId, connectorId, grpcChannelId) {
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

    fun enableNewTasks(connectorId: Long, grpcChannelId: Long) {
        logger.info("$this: enable new requests for connector $connectorId")
        jobsContainer.enableNewOnes(connectorId, grpcChannelId)
    }

    fun cancelAll() {
        logger.info("$this: cancel all tasks")
        runCatching { jobsContainer.cancelAllForever() }
    }

    fun cancelAll(connectorId: Long, grpcChannelId: Long) {
        logger.info("$this: cancelling all tasks of connector $connectorId ...")
        runCatching { jobsContainer.cancel(connectorId, grpcChannelId) }
        logger.info("$this: cancelled all tasks of connector $connectorId")
    }

    suspend fun gracefulShutdownAll(connectorId: Long, grpcChannelId: Long) {
        logger.info("$this: graceful shutting down all tasks of connector $connectorId ...")
        runCatching { jobsContainer.gracefulShutdownByConnector(connectorId, grpcChannelId) }
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
        grpcChannelId: Long,
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

        val added = jobsContainer.put(connectorId, grpcChannelId, requestId, job)

        if (added) {
            job.start()
        } else {
            job.cancel()
        }
    }

    override fun toString() = "ActionTaskExecutor(action=$action)"
}

internal val PayloadInterface.asProto
    get() = PayloadProto.newBuilder().also { builder ->
        when (this) {
            is Payload -> builder.setJson(data)
            is ProtobufPayload -> builder.setProtobuf(data)
        }
        dataType?.let { builder.dataType = it }
    }

private val PayloadProto.asPayload: Payload
    get() = Payload(dataType, json)

private val PayloadProto.asPayloadInterface: PayloadInterface
    get() = if (hasJson()) Payload(dataType, json) else ProtobufPayload(dataType, protobuf)

private fun Builder.setPredict(prediction: PayloadInterface) {
    BillingUnitsThreadLocal.getUnits()?.also {
        putHeaders("Z-custom-billing", it.toString())
    }
    setPredict(PredictResponseProto.newBuilder().setData(prediction.asProto))
}

private fun Builder.setPartialPredict(prediction: PayloadInterface, last: Boolean) {
    BillingUnitsThreadLocal.getUnits()?.also {
        putHeaders("Z-custom-billing", it.toString())
    }
    setPartialPredict(PartialPredictResponseProto.newBuilder().setData(prediction.asProto).setFinish(last))
}

private fun Builder.setFit() =
    setFit(FitResponseProto.newBuilder())

private fun Builder.setExt(extResult: PayloadInterface) =
    setExt(ExtendedResponseProto.newBuilder().setData(extResult.asProto))

private fun Builder.setBatch(batchResult: List<MlpResponse>, requestsIdes: List<Long>): Builder {
    require(batchResult.size == requestsIdes.size) { "Batch responses size must be equal to requests size" }
    val actionToGateProtos = batchResult.zip(requestsIdes)
        .map { (data, requestId) ->
            val builder = BatchPayloadResponseProto.newBuilder().setRequestId(requestId)
            when (data) {
                is PayloadInterface -> builder.setPredict(PredictResponseProto.newBuilder().setData(data.asProto))
                is MlpResponseException -> builder.setError(data.exception.asErrorProto)
                is MlpPartialBinaryResponse -> builder.setError(
                    ApiErrorProto.newBuilder()
                        .setCode(CommonErrorCode.PARTIAL_RESPONSE_NOT_SUPPORTED_IN_BATCH.code)
                        .setMessage(CommonErrorCode.PARTIAL_RESPONSE_NOT_SUPPORTED_IN_BATCH.message)
                        .setStatus(CommonErrorCode.PARTIAL_RESPONSE_NOT_SUPPORTED_IN_BATCH.status)
                )

                is RawPayload -> builder.setError(
                    ApiErrorProto.newBuilder()
                        .setCode(CommonErrorCode.RAW_PAYLOAD_NOT_SUPPORTED_IN_BATCH.code)
                        .setMessage(CommonErrorCode.RAW_PAYLOAD_NOT_SUPPORTED_IN_BATCH.message)
                        .setStatus(CommonErrorCode.RAW_PAYLOAD_NOT_SUPPORTED_IN_BATCH.status)
                )
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
