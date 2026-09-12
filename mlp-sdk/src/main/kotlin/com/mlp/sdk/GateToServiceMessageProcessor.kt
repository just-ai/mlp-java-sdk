package com.mlp.sdk

import com.mlp.gate.ApiErrorProto
import com.mlp.gate.BatchRequestProto
import com.mlp.gate.CancelRequestProto
import com.mlp.gate.ClusterUpdateProto
import com.mlp.gate.ExtendedRequestProto
import com.mlp.gate.FitRequestProto
import com.mlp.gate.FitStatusProto
import com.mlp.gate.GateToServiceProto
import com.mlp.gate.HeartBeatProto
import com.mlp.gate.PartialPredictRequestProto
import com.mlp.gate.PredictRequestProto
import com.mlp.gate.ServiceInfoProto
import com.mlp.gate.ServiceToGateProto
import com.mlp.sdk.utils.CALLER_ACCOUNT_ID_HEADER
import com.mlp.sdk.utils.CONNECTOR_ID_MDC_PARAM
import com.mlp.sdk.utils.CONTENT_HIDDEN_HEADER
import com.mlp.sdk.utils.GATE_REQUEST_ID_MDC_PARAM
import com.mlp.sdk.utils.MLP_API_KEY_NAME_HEADER
import com.mlp.sdk.utils.MLP_BILLING_ACCOUNT_ID_HEADER
import com.mlp.sdk.utils.MLP_BILLING_KEY_HEADER
import com.mlp.sdk.utils.MLP_BILLING_KEY_MDC_PARAM
import com.mlp.sdk.utils.MLP_BILLING_KEY_NAME_HEADER
import com.mlp.sdk.utils.MLP_BILLING_USER_ID_HEADER
import com.mlp.sdk.utils.REQUEST_ID_HEADER
import com.mlp.sdk.utils.REQUEST_ID_MDC_PARAM
import com.mlp.sdk.utils.SERVER_TIME_HEADER
import com.mlp.sdk.utils.WithLogger
import com.mlp.sdk.utils.asErrorProto
import com.mlp.sdk.utils.getAsPayload
import com.mlp.sdk.utils.getAsPayloadInterface
import com.mlp.sdk.utils.logProto
import com.mlp.sdk.utils.setBatch
import com.mlp.sdk.utils.setExt
import com.mlp.sdk.utils.setFit
import com.mlp.sdk.utils.setPartialPredict
import com.mlp.sdk.utils.setPredict
import com.mlp.sdk.utils.setStartPartialPredict
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.MDC

class GateToServiceMessageProcessor(
    private val connector: Connector,
    private val storage: ServiceToGateMessageStorage,
) : WithLogger {

    private val connectorId: Long = connector.connectorId
    private val executor: TaskExecutor = connector.executor
    private val scope: CoroutineScope = connector.scope
    private val config: MlpServiceConfig = connector.config
    private val pool: ConnectorsPool = connector.pool

    fun process(request: GateToServiceProto) {
        val tracker = TimeTracker()
        val requestContext = buildRequestContext(request, connector)

        MDC.setContextMap(
            mapOf(
                REQUEST_ID_MDC_PARAM to requestContext.requestId,
                CONNECTOR_ID_MDC_PARAM to connectorId.toString(),
                GATE_REQUEST_ID_MDC_PARAM to requestContext.gateRequestId.toString(),
                MLP_BILLING_KEY_MDC_PARAM to requestContext.billingKey,
            )
        )
        try {
            process(request, requestContext, tracker)
        } finally {
            MDC.clear()
        }
    }

    private fun process(request: GateToServiceProto, context: RequestContext, tracker: TimeTracker) {
        if (request.hasHeartBeat()) {
            logger.trace("GateToService (connector $connectorId, requestId: ${request.requestId}): heartbeat")
        } else {
            logProto(request, prompt = "GateToService (connector $connectorId)", noContentLogging = context.noContentLogging)
        }

        when (request.bodyCase) {
            // System messages
            GateToServiceProto.BodyCase.SERVICEINFO -> processServiceInfo(request.serviceInfo)
            GateToServiceProto.BodyCase.CANCEL -> processCancelRequest(request.cancel)
            GateToServiceProto.BodyCase.CLUSTER -> processCluster(request.cluster)
            GateToServiceProto.BodyCase.ERROR -> processError(request.error)
            GateToServiceProto.BodyCase.HEARTBEAT -> processHeartbeat(request.heartBeat)
            GateToServiceProto.BodyCase.STOPSERVING -> processStopServing()
//            GateToServiceProto.BodyCase.SYNCSEQUENCENUMBERS -> processSyncSequenceNumbers(request.syncSequenceNumbers)

            // Business messages
            GateToServiceProto.BodyCase.BATCH -> processBatch(request.batch, context)
            GateToServiceProto.BodyCase.EXT -> processExt(request.ext, context)
            GateToServiceProto.BodyCase.FIT -> processFit(request.fit, context)
            GateToServiceProto.BodyCase.PARTIALPREDICT -> processPartialPredict(request.partialPredict, context)
            GateToServiceProto.BodyCase.PREDICT -> processPredict(request.predict, tracker, context)

            // Other
            GateToServiceProto.BodyCase.BODY_NOT_SET -> logger.warn("Request body is not set")
            null -> logger.error("Connector $connectorId: body case is null")
            else -> logger.debug("Could not find request bodyCase with type {}", request.bodyCase)
        }
    }

    private fun processServiceInfo(serviceInfo: ServiceInfoProto) {
        connector.serviceInfo = serviceInfo
        logger.info("Connector $connectorId: service info updated: modelId=${serviceInfo.modelId}, accountId=${serviceInfo.accountId}")
    }

    private fun processCancelRequest(request: CancelRequestProto) {
        executor.cancelRequest(connector.connectorId, request.requestIdToCancel)
    }

    private fun processCluster(cluster: ClusterUpdateProto) {
        if (config.ignoreClusterUpdates) return

        if (connector.targetUrl != cluster.currentServer) {
            logger.info("$this: url is changed from ${connector.targetUrl} to ${cluster.currentServer}")
            connector.targetUrl = cluster.currentServer
        }

        scope.launch {
            pool.updateConnectors(cluster.serversList)
        }
    }

    private fun processError(error: ApiErrorProto) {
        when (error.code) {
            "mlp.gate.instance_by_token_not_found" ->
                processTokenNotFound()

            else ->
                logger.error("Connector $connectorId: error ${error.message}")
        }
    }

    private fun processTokenNotFound() {
        logger.warn("Connector $connectorId: Receive instance_by_token_not_found error, so shutdown grpc channel")
        connector.grpcChannel?.gracefulShutdownFromGate(reason = "instance_by_token_not_found")
    }

    private fun processHeartbeat(heartBeat: HeartBeatProto) {
        logger.info("Connector $connectorId: received heartbeat: $heartBeat")
        connector.grpcChannel?.updateHeartbeat(heartBeat.interval.toLong())
    }

    private fun processStopServing() {
        logger.info("$this: receive graceful shutdown from gate ...")
        connector.grpcChannel?.gracefulShutdownFromGate()
    }

//    private fun processSyncSequenceNumbers(sequenceNumbers: SyncSequenceNumbersProto) {
//        logger.debug("Connector {} received sequenceNumber {}", connectorId, sequenceNumbers)
//
//        when (sequenceNumbers.bodyCase) {
//            SyncSequenceNumbersProto.BodyCase.LASTPROCESSEDSEQUENCENUMBER -> {
//                storage.removeMessagesUntilSequenceNumber(sequenceNumbers.lastProcessedSequenceNumber)
//            }
//
//            SyncSequenceNumbersProto.BodyCase.INITIALGATESEQUENCENUMBER -> {
//                runBlocking {
//                    logger.info(
//                        "Connector $connectorId received initialGateSequenceNumber=${sequenceNumbers.initialGateSequenceNumber}, " +
//                                "adapter lastSentSequenceNumber=${storage.lastSentSequenceNumber}"
//                    )
//                    storage.removeMessagesUntilSequenceNumber(sequenceNumbers.initialGateSequenceNumber)
//                    storage.getAllStoredMessages().forEach { message ->
//                        connector.grpcChannel?.resend(message)
//                    }
//                    connector.grpcChannel?.setActiveState()
//                }
//            }
//
//            SyncSequenceNumbersProto.BodyCase.BODY_NOT_SET, null -> {}
//        }
//    }

    private fun processBatch(request: BatchRequestProto, context: RequestContext) {
        executor.runAsync(context) { action ->
            val responseBuilder = ServiceToGateProto.newBuilder().setRequestId(context.gateRequestId)
                .putHeaders(CONTENT_HIDDEN_HEADER, context.noContentLogging.toString())

            val data = request.dataList

            val payloadData = data.map { it.data.getAsPayload(context.noContentLogging) }
            val requestsIdes = data.map { it.requestId }

            runCatching {
                val responses = action.batch(payloadData, request.config.getAsPayload(context.noContentLogging))
                responseBuilder.setBatch(responses, requestsIdes)
            }.onFailure {
                logger.error("Error while processing batch request", it)
                responseBuilder.setError(it.asErrorProto)
            }

            runCatching { pool.send(connectorId, responseBuilder) }
                .onFailure { logger.error("Error while sending batch response", it) }
        }
    }

    private fun processExt(request: ExtendedRequestProto, context: RequestContext) {
        executor.runAsync(context) { action ->
            val responseBuilder = ServiceToGateProto.newBuilder().setRequestId(context.gateRequestId)
                .putHeaders(CONTENT_HIDDEN_HEADER, context.noContentLogging.toString())

            val methodName = requireNotNull(request.methodName) { "methodName" }
            val params =
                requireNotNull(request.paramsMap.mapValues { requireNotNull(it.value.getAsPayload(context.noContentLogging)) }) { "paramsMap" }

            runCatching {
                val responsePayload = action.ext(methodName, params)
                val headers = responsePayload.headers ?: emptyMap()
                val statusCode = responsePayload.statusCode ?: 200

                when (val responsePayload = action.ext(methodName, params)) {
                    is RawPayload -> responseBuilder.setExt(responsePayload.asPayload, headers, statusCode)
                    is PayloadInterface -> responseBuilder.setExt(responsePayload, headers, statusCode)
                    is MlpResponseException -> throw responsePayload.exception
                    is MlpPartialBinaryResponse -> throw NotImplementedError()
                }
            }.onFailure {
                logger.error("Error while processing ext request", it)
                responseBuilder.setError(it.asErrorProto)
            }

            runCatching { pool.send(connectorId, responseBuilder) }
                .onFailure { logger.error("Error while sending ext response", it) }
        }
    }

    private fun processFit(request: FitRequestProto, context: RequestContext) {
        executor.runAsync(context) { action ->
            val responseBuilder = ServiceToGateProto.newBuilder().setRequestId(context.gateRequestId)
                .putHeaders(CONTENT_HIDDEN_HEADER, context.noContentLogging.toString())

            val trainPayload = request.trainData.getAsPayload(context.noContentLogging)
            val targetsPayload = request.targetsData?.getAsPayload(context.noContentLogging)
            val configPayload = request.config?.getAsPayload(context.noContentLogging)
            val modelDir = request.modelDir

            runCatching {
                val percentageConsumer: suspend (Int) -> Unit = { percentage ->
                    runCatching {
                        val status = FitStatusProto.newBuilder().setPercentage(percentage).build()
                        val proto = ServiceToGateProto.newBuilder().setRequestId(context.gateRequestId)
                            .putHeaders(CONTENT_HIDDEN_HEADER, context.noContentLogging.toString())
                            .setFitStatus(status)
                        pool.send(connectorId, proto)
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

            runCatching { pool.send(connectorId, responseBuilder) }
                .onFailure { logger.error("Error while sending fit response", it) }
        }
    }

    /**
     * Стриминговый запрос от гейта. Отдельного учёта стрима здесь не нужно: все кадры уходят внутри
     * job, который собирает flow сервиса (`collect`), и job живёт до конца этого flow. Отвязанного
     * окна, как у MlpPartialBinaryResponse, тут нет — дренаж по requestJobMap ждёт ровно столько,
     * сколько идёт стрим.
     */
    private fun processPartialPredict(request: PartialPredictRequestProto, context: RequestContext) {
        val requestId = context.gateRequestId
        val connectorId = context.connectorId

        executor.runAsyncWithChannel(context, { action, channel ->
            runCatching {
                action.streamPredictRaw(channel.receiveAsFlow()).onStart {
                    logger.info("requestId: $requestId Start processing stream flow")
                }.onCompletion {
                    logger.info("requestId: $requestId Finish processing stream flow")
                    channel.close(it)
                }.catch {
                    logger.error("requestId: $requestId Error while processing stream predict request", it)
                    val responseBuilder = ServiceToGateProto.newBuilder().setRequestId(requestId)
                        .putHeaders(CONTENT_HIDDEN_HEADER, context.noContentLogging.toString())
                    responseBuilder.setError(it.asErrorProto)
                    runCatching { pool.send(connectorId, responseBuilder) }
                        .onFailure { logger.error("Error while sending predict response", it) }
                }.collect { response ->
                    val responseBuilder = ServiceToGateProto.newBuilder().setRequestId(requestId)
                        .putHeaders(CONTENT_HIDDEN_HEADER, context.noContentLogging.toString())
                    responseBuilder.setPartialPredict(response.payload, response.last)
                    runCatching { pool.send(connectorId, responseBuilder) }
                        .onFailure { logger.error("Error while sending predict response", it) }
                }
            }.onFailure {
                logger.error("Error while processing predict request", it)
                channel.close(it)
                val responseBuilder = ServiceToGateProto.newBuilder().setRequestId(requestId)
                    .putHeaders(CONTENT_HIDDEN_HEADER, context.noContentLogging.toString())
                responseBuilder.setError(it.asErrorProto)
                runCatching { pool.send(connectorId, responseBuilder) }
                    .onFailure { logger.error("Error while sending predict response", it) }
            }
        }, { channel ->
            if (request.hasData()) {
                val dataPayload = requireNotNull(request.data?.getAsPayloadInterface(context.noContentLogging)) { "Payload data" }
                val config =
                    if (request.config == request.config.defaultInstanceForType) null else request.config?.getAsPayload(context.noContentLogging)
                runBlocking { channel.send(PayloadWithConfig(dataPayload, config)) }
            }

            if (request.finish) channel.close()
        })
    }

    private fun processPredict(request: PredictRequestProto, tracker: TimeTracker, context: RequestContext) {
        executor.runAsync(context) { action ->
            // Стрим открываем до вызова predict, а не после разбора ответа: сервис вправе отдать
            // MlpPartialBinaryResponse и начать слать кадры из своей корутины ещё до того, как мы
            // увидим её тип, — иначе между завершением job и первым кадром остаётся окно, в котором
            // дренаж остановки считает запрос завершённым. Закроет стрим терминальный кадр
            // (predict, error или partialPredict с finish=true) в Connector.sendServiceToGate,
            // так что обычный predict от этого учёта ничего не теряет.
            executor.streamOpened(connectorId, context.gateRequestId)

            val dataPayload = requireNotNull(request.data.getAsPayload(context.noContentLogging)) { "Payload data" }
            val configPayload = request.config.getAsPayload(context.noContentLogging)

            val responseBuilder = ServiceToGateProto.newBuilder().setRequestId(context.gateRequestId)
                .putHeaders(CONTENT_HIDDEN_HEADER, context.noContentLogging.toString())

            var capturedResponsePayload: MlpResponse? = null
            runCatching {
                val responsePayload = action.predict(dataPayload, configPayload, context)
                capturedResponsePayload = responsePayload

                val headers = responsePayload.headers
                val statusCode = responsePayload.statusCode
                when (responsePayload) {
                    is RawPayload -> responseBuilder.setPredict(responsePayload.asPayload, headers, statusCode)
                    is PayloadInterface -> responseBuilder.setPredict(responsePayload, headers, statusCode)
                    is MlpResponseException -> throw responsePayload.exception
                    is MlpPartialBinaryResponse ->
                        if (headers == null && statusCode == null)
                            return@runAsync
                        else
                            responseBuilder.setStartPartialPredict(headers, statusCode)
                    // если partialResponse, то просто ничего не делаем. Респонзы будет отправлять сам сервис.
                }
            }.onFailure {
                logger.error("Error while processing predict request", it)
                responseBuilder.setError(it.asErrorProto)
            }

            val elapsed = System.currentTimeMillis() - tracker.startTime
            responseBuilder.putHeaders(SERVER_TIME_HEADER, elapsed.toString())
            runCatching { pool.send(connectorId, responseBuilder) }
                .onFailure { logger.error("Error while sending predict response", it) }

            capturedResponsePayload?.callback?.invoke()
        }
    }

    private fun buildRequestContext(
        request: GateToServiceProto,
        connector: Connector,
    ): RequestContext {
        return RequestContext(
            connectorId = connector.connectorId,
            callerAccountId = request.getHeadersOrDefault(CALLER_ACCOUNT_ID_HEADER, null)?.toLongOrNull(),
            noContentLogging = request.getHeadersOrDefault(CONTENT_HIDDEN_HEADER, "false").toBoolean(),
            requestId = request.headersMap[REQUEST_ID_HEADER] ?: request.requestId.toString(),
            billingKey = request.headersMap[MLP_BILLING_KEY_HEADER],
            gateRequestId = request.requestId,
            modelId = connector.serviceInfo?.modelId ?: 0L,
            modelAccountId = connector.serviceInfo?.accountId ?: 0L,
            apiKeyName = request.getHeadersOrDefault(MLP_API_KEY_NAME_HEADER, null),
            billingKeyName = request.getHeadersOrDefault(MLP_BILLING_KEY_NAME_HEADER, null),
            billingAccountId = request.getHeadersOrDefault(MLP_BILLING_ACCOUNT_ID_HEADER, null)?.toLongOrNull(),
            billingUserId = request.getHeadersOrDefault(MLP_BILLING_USER_ID_HEADER, null)
        )
    }
}
