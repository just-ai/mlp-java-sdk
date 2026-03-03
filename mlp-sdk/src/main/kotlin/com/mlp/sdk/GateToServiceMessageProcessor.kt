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
import com.mlp.gate.ServiceToGateProto
import com.mlp.gate.SyncSequenceNumbersProto
import com.mlp.sdk.utils.CALLER_ACCOUNT_ID
import com.mlp.sdk.utils.CONTENT_HIDDEN_HEADER
import com.mlp.sdk.utils.WithLogger
import com.mlp.sdk.utils.logProto
import kotlinx.coroutines.CoroutineScope
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
                "requestId" to requestContext.requestId,
                "connectorId" to connectorId.toString(),
                "gateRequestId" to requestContext.gateRequestId.toString(),
                "MLP-BILLING-KEY" to requestContext.billingKey,
            )
        )
        try {
            process(request, requestContext, tracker)
        } finally {
            MDC.clear()
        }
    }

    private fun process(request: GateToServiceProto, context: RequestContext, tracker: TimeTracker) {
        if (request.hasHeartBeat())
            logger.trace("GateToService (connector $connectorId, requestId: ${request.requestId}): heartbeat")
        else
            logProto(request, prompt = "GateToService (connector $connectorId)", noContentLogging = context.noContentLogging)

        when (request.bodyCase) {
            GateToServiceProto.BodyCase.CLUSTER -> processCluster(request.cluster)
            GateToServiceProto.BodyCase.ERROR -> processError(request.error)
            GateToServiceProto.BodyCase.HEARTBEAT -> processHeartbeat(request.heartBeat)
            GateToServiceProto.BodyCase.STOPSERVING -> processStopServing()
            GateToServiceProto.BodyCase.SYNCSEQUENCENUMBERS -> processSyncSequenceNumbers(request.syncSequenceNumbers)

            GateToServiceProto.BodyCase.BATCH -> processBatch(request.batch, context)
            GateToServiceProto.BodyCase.CANCEL -> processCancelRequest(request.cancel)
            GateToServiceProto.BodyCase.EXT -> processExt(request.ext, context)
            GateToServiceProto.BodyCase.FIT -> processFit(request.fit, context)
            GateToServiceProto.BodyCase.PARTIALPREDICT -> processPartialPredict(request.partialPredict, context)
            GateToServiceProto.BodyCase.PREDICT -> processPredict(request.predict, tracker, context)

            GateToServiceProto.BodyCase.BODY_NOT_SET -> logger.warn("Request body is not set")
            null -> logger.error("Connector $connectorId: body case is null")
            else -> logger.debug("Could not find request bodyCase with type {}", request.bodyCase)
        }
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

    private fun processHeartbeat(heartBeat: HeartBeatProto) {
        logger.info("Connector $connectorId: received heartbeat: $heartBeat")
        connector.grpcChannel?.updateHeartbeat(heartBeat.interval.toLong())
    }

    private fun processStopServing() {
        logger.info("$this: receive graceful shutdown from gate ...")
        connector.grpcChannel?.gracefulShutdownFromGate()
    }

    private fun processSyncSequenceNumbers(sequenceNumbers: SyncSequenceNumbersProto) {
        logger.debug("Connector $connectorId received sequenceNumber $sequenceNumbers")

        when (sequenceNumbers.bodyCase) {
            SyncSequenceNumbersProto.BodyCase.LASTPROCESSEDSEQUENCENUMBER -> {
                storage.removeMessagesUntilSequenceNumber(sequenceNumbers.lastProcessedSequenceNumber)
            }

            SyncSequenceNumbersProto.BodyCase.INITIALGATESEQUENCENUMBER -> {
                runBlocking {
                    logger.info(
                        "Connector $connectorId received initialGateSequenceNumber=${sequenceNumbers.initialGateSequenceNumber}, " +
                                "adapter lastSentSequenceNumber=${storage.lastSentSequenceNumber}"
                    )
                    storage.removeMessagesUntilSequenceNumber(sequenceNumbers.initialGateSequenceNumber)
                    storage.getAllStoredMessages().forEach { message ->
                        connector.grpcChannel?.resend(message)
                    }
                    connector.grpcChannel?.setActiveState()
                }
            }

            SyncSequenceNumbersProto.BodyCase.BODY_NOT_SET, null -> {}
        }
    }

    private fun processBatch(request: BatchRequestProto, context: RequestContext) {
        executor.runJob(context) { action ->
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

    private fun processCancelRequest(request: CancelRequestProto) {
        executor.cancelRequest(connector.connectorId, request.requestIdToCancel)
    }

    private fun processExt(request: ExtendedRequestProto, context: RequestContext) {
        executor.runJob(context) { action ->
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
        executor.runJob(context) { action ->
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

    private fun processPartialPredict(request: PartialPredictRequestProto, context: RequestContext) {

    }

    private fun processPredict(request: PredictRequestProto, tracker: TimeTracker, context: RequestContext) {
        executor.runJob(context) { action ->
            val dataPayload = requireNotNull(request.data.getAsPayload(context.noContentLogging)) { "Payload data" }
            val configPayload = request.config.getAsPayload(context.noContentLogging)

            val responseBuilder = ServiceToGateProto.newBuilder().setRequestId(context.gateRequestId)
                .putHeaders(CONTENT_HIDDEN_HEADER, context.noContentLogging.toString())

            runCatching {
                val responsePayload = action.predict(dataPayload, configPayload, context)
                val headers = responsePayload.headers
                val statusCode = responsePayload.statusCode
                when (responsePayload) {
                    is RawPayload -> responseBuilder.setPredict(responsePayload.asPayload, headers, statusCode)
                    is PayloadInterface -> responseBuilder.setPredict(responsePayload, headers, statusCode)
                    is MlpResponseException -> throw responsePayload.exception
                    is MlpPartialBinaryResponse ->
                        if (headers == null && statusCode == null)
                            return@runJob
                        else
                            responseBuilder.setStartPartialPredict(headers, statusCode)
                    // если partialResponse, то просто ничего не делаем. Респонзы будет отправлять сам сервис.
                }
            }.onFailure {
                logger.error("Error while processing predict request", it)
                responseBuilder.setError(it.asErrorProto)
            }

            val elapsed = System.currentTimeMillis() - tracker.startTime
            responseBuilder.putHeaders("Z-Server-Time", elapsed.toString())
            runCatching { pool.send(connectorId, responseBuilder) }
                .onFailure { logger.error("Error while sending predict response", it) }
        }
    }

    private fun processTokenNotFound() {
        logger.warn("Connector $connectorId: Receive instance_by_token_not_found error, so shutdown grpc channel")
        connector.grpcChannel?.gracefulShutdownFromGate(reason = "instance_by_token_not_found")
    }

    private fun buildRequestContext(
        request: GateToServiceProto,
        connector: Connector,
    ): RequestContext {
        return RequestContext(
            connectorId = connector.connectorId,
            callerAccountId = request.getHeadersOrDefault(CALLER_ACCOUNT_ID, null)?.toLongOrNull(),
            noContentLogging = request.getHeadersOrDefault(CONTENT_HIDDEN_HEADER, "false").toBoolean(),
            requestId = request.headersMap["Z-requestId"] ?: request.requestId.toString(),
            billingKey = request.headersMap["MLP-BILLING-KEY"],
            gateRequestId = request.requestId,
        )
    }
}
