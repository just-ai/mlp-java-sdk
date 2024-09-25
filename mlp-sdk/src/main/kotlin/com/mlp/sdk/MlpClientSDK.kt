package com.mlp.sdk

import com.mlp.gate.AsrRequestProto
import com.mlp.gate.ClientAsrRequestProto
import com.mlp.gate.ClientAsrResponseProto
import com.mlp.gate.ClientRequestProto
import com.mlp.gate.ClientResponseProto
import com.mlp.gate.ExtendedRequestProto
import com.mlp.gate.GateGrpcKt.GateCoroutineStub
import com.mlp.gate.PartialPredictRequestProto
import com.mlp.gate.PayloadProto
import com.mlp.gate.PredictRequestProto
import com.mlp.sdk.MlpExecutionContext.Companion.systemContext
import com.mlp.sdk.datatypes.asr.common.AsrRequest
import com.mlp.sdk.datatypes.asr.common.AsrResponse
import com.mlp.sdk.utils.JSON
import com.mlp.sdk.utils.JSON.parseOrThrowBadRequestMlpException
import io.grpc.ConnectivityState.READY
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.slf4j.MDC
import java.time.Duration
import java.time.Duration.between
import java.time.Duration.ofSeconds
import java.time.Instant.now
import java.util.concurrent.Executors.defaultThreadFactory
import java.util.concurrent.Executors.newSingleThreadExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicReference
import kotlin.Int.Companion.MAX_VALUE
import kotlin.coroutines.resume

class MlpClientSDK(
    initConfig: MlpClientConfig? = null,
    override val context: MlpExecutionContext = systemContext
) : WithExecutionContext {

    val config = initConfig ?: loadClientConfig(environment = environment)
    var connectionToken: String?
    var billingToken: String?
    private val channel = AtomicReference<ManagedChannel>()
    private lateinit var stub: GateCoroutineStub

    val apiClient by lazy { MlpApiClient.getInstance(config.clientToken, config.clientApiGateUrl) }
    val backoffJob: Job

    init {
        val gateUrl = config.initialGateUrls.firstOrNull() ?: error("There is not MLP_GRPC_HOSTS")
        connectionToken = config.clientToken
        billingToken = config.billingToken
        logger.debug("Starting mlp client for url $gateUrl")
        connect(gateUrl)

        backoffJob = launchBackoffJob()
    }

    private fun reconnect() {
        logger.info("Reconnecting to gate")
        val gateUrl = config.initialGateUrls.firstOrNull() ?: error("There is not MLP_GRPC_HOSTS")
        connect(gateUrl)
    }

    private fun connect(gateUrl: String) {
        val channelBuilder = ManagedChannelBuilder
            .forTarget(gateUrl)
            .enableRetry()
            .maxRetryAttempts(MAX_VALUE)
            .maxInboundMessageSize(MAX_VALUE)
            .keepAliveTime(config.keepAliveTimeSeconds, SECONDS)
            .keepAliveTimeout(config.keepAliveTimeoutSeconds, SECONDS)
            .keepAliveWithoutCalls(config.keepAliveWithoutCalls)

        if (!config.grpcSecure)
            channelBuilder.usePlaintext()
        val newChannel = channelBuilder.build()
        val previousChannel = channel.getAndSet(newChannel)
        stub = GateCoroutineStub(newChannel)

        runCatching { previousChannel?.shutdown() }
            .onFailure { logger.warn("Exception while shutting down managed channel") }
    }

    /**
     * Connection may be IDLE because of inactivity some time ago. If you want to await for connection to be ready, use [awaitConnection] instead.
     */
    fun isConnected() =
        channel.get().getState(true) == READY

    /**
     * Await for connection to be ready. If connection is already ready, returns immediately.
     */
    suspend fun awaitConnection() {
        while (true) {
            val state = channel.get().getState(true)

            if (state == READY)
                break

            suspendCancellableCoroutine<Unit> {
                channel.get().notifyWhenStateChanged(state) { it.resume(Unit) }
            }
        }
    }

    fun predictBlocking(
        account: String,
        model: String,
        data: String,
        config: String? = null,
        timeout: Duration? = null,
        authToken: String = ensureDefaultToken(),
        requestHeaders: Map<String, String> = emptyMap()
    ) = runBlocking {
        predict(
            account,
            model,
            data,
            config,
            timeout,
            authToken,
            requestHeaders
        )
    }

    fun predictBlocking(
        account: String,
        model: String,
        data: Payload,
        config: Payload? = null,
        timeout: Duration? = null,
        authToken: String = ensureDefaultToken(),
        requestHeaders: Map<String, String> = emptyMap()
    ) = runBlocking {
        predict(
            account,
            model,
            data,
            config,
            timeout,
            authToken,
            requestHeaders
        )
    }

    suspend fun predict(
        account: String,
        model: String,
        data: String,
        config: String? = null,
        timeout: Duration? = null,
        authToken: String = ensureDefaultToken(),
        requestHeaders: Map<String, String> = emptyMap()
    ) =
        predict(
            account,
            model,
            Payload("", data),
            config?.let { Payload("", it) },
            timeout,
            authToken,
            requestHeaders
        ).data

    suspend fun predictStream(
        account: String,
        model: String,
        data: Payload,
        config: Payload? = null,
        timeout: Duration? = null,
        authToken: String = ensureDefaultToken(),
        requestHeaders: Map<String, String> = emptyMap()
    ): Flow<ClientResponseProto> =
        sendRequestPayloadStream(
            buildPredictRequest(
                account,
                model,
                data,
                config,
                timeout,
                authToken,
                requestHeaders
            ),
            timeout
        )

    suspend fun predict(
        account: String,
        model: String,
        data: Payload,
        config: Payload? = null,
        timeout: Duration? = null,
        authToken: String = ensureDefaultToken(),
        requestHeaders: Map<String, String> = emptyMap()
    ) =
        sendRequestPayload(
            buildPredictRequest(
                account,
                model,
                data,
                config,
                timeout,
                authToken,
                requestHeaders
            ),
            timeout
        )

    fun streamPredict(
        account: String,
        model: String,
        stream: Flow<PayloadWithConfig>,
        timeout: Duration? = null,
        authToken: String = ensureDefaultToken(),
    ): Flow<PayloadInterface> {
        val clientRequestFlow = stream.map {
            buildPartialPredict(account, model, it.payload, it.config, timeout, authToken)
        }
        return sendStream(clientRequestFlow)
    }

    fun asrPredict(
        account: String,
        model: String,
        stream: Flow<AsrRequest>,
        timeout: Duration? = null,
        authToken: String = ensureDefaultToken(),
    ): Flow<AsrResponse> {
        val clientRequestFlow = stream.map {
            val data = ProtobufPayload(dataType = AsrRequest.DATATYPE, it.audioContent)
            val config = it.config?.let { conf -> Payload(JSON.stringify(conf)) }
            buildPartialPredict(account, model, data, config, timeout, authToken)
        }
        return sendStream(clientRequestFlow).map {
            when (it) {
                is Payload -> JSON.parseOrThrowBadRequestMlpException(it.data, AsrResponse::class.java)
                is ProtobufPayload -> JSON.parseOrThrowBadRequestMlpException(it.data.toStringUtf8(), AsrResponse::class.java)
            }
        }
    }

    fun asrPredictProto(
        account: String,
        model: String,
        stream: Flow<AsrRequestProto>,
        timeout: Duration? = null,
        authToken: String = ensureDefaultToken(),
    ): Flow<ClientAsrResponseProto> {
        val clientAsrRequestFlow = stream.map {
            ClientAsrRequestProto.newBuilder()
                .setAccount(account)
                .setModel(model)
                .setAuthToken(authToken)
                .setAsr(it)
                .build()
        }
        return stub.processRecognition(clientAsrRequestFlow)
    }

    fun extBlocking(
        account: String,
        model: String,
        methodName: String,
        timeout: Duration? = null,
        authToken: String = ensureDefaultToken(),
        vararg params: Pair<String, PayloadProto>
    ) =
        runBlocking { ext(account, model, methodName, timeout, authToken, *params) }

    suspend fun ext(
        account: String,
        model: String,
        methodName: String,
        timeout: Duration? = null,
        authToken: String = ensureDefaultToken(),
        vararg params: Pair<String, PayloadProto>
    ) =
        sendRequest(buildExtRequest(account, model, methodName, timeout, authToken, params), timeout)

    private fun ensureDefaultToken() = requireNotNull(connectionToken) {
        "Set authToken in environment variables, or in init method, or directly in predict method"
    }

    private fun sendRequestPayloadStream(
        request: ClientRequestProto,
        timeout: Duration? = null
    ): Flow<ClientResponseProto> {
        return stub.processResponseStream(request)
    }

    private fun sendStream(requests: Flow<ClientRequestProto>): Flow<PayloadInterface> {
        val flow = stub.processStream(requests)
        return flow.map { response ->
            when {
                response.hasPredict() -> {
                    val data = response.predict.data
                    if (data.hasJson()) {
                        Payload(dataType = data.dataType, data = data.json)
                    } else {
                        ProtobufPayload(dataType = data.dataType, data = data.protobuf)
                    }
                }

                response.hasPartialPredict() -> {
                    val data = response.partialPredict.data
                    if (data.hasJson()) {
                        Payload(dataType = data.dataType, data = data.json)
                    } else {
                        ProtobufPayload(dataType = data.dataType, data = data.protobuf)
                    }
                }

                response.hasError() -> {
                    throw MlpClientException(
                        response.error.code,
                        response.error.message,
                        response.error.argsMap,
                        response.headersMap["Z-requestId"]
                    )
                }

                else ->
                    throw MlpClientException(
                        "wrong-response",
                        "Wrong response type: $response",
                        emptyMap(),
                        response.headersMap["Z-requestId"]
                    )
            }
        }
    }

    private suspend fun sendRequestPayload(request: ClientRequestProto, timeout: Duration? = null): RawPayload {
        val response = sendRequest(request, timeout)

        return when {
            response.hasPredict() ->
                RawPayload(response.predict.data.dataType, response.predict.data.json, response.headersMap)

            response.hasPartialPredict() ->
                RawPayload(
                    response.partialPredict.data.dataType,
                    response.partialPredict.data.json,
                    response.headersMap
                )

            response.hasExt() ->
                RawPayload(response.ext.data.dataType, response.ext.data.json, response.headersMap)

            else ->
                throw MlpClientException(
                    "wrong-response",
                    "Wrong response type: $response",
                    emptyMap(),
                    response.headersMap["Z-requestId"]
                )
        }
    }

    suspend fun sendRequest(request: ClientRequestProto, timeout: Duration? = null): ClientResponseProto {
        val timeoutMs = timeout?.toMillis() ?: config.clientPredictTimeoutMs
        val response = withTimeout(timeoutMs) { withRetry { executePredictRequest(request) } }

        if (response.hasError()) {
            logger.error("Error from gate. Error \n${response.error}")
            throw MlpClientException(
                response.error.code,
                response.error.message,
                response.error.argsMap,
                response.headersMap["Z-requestId"]
            )
        }
        return response
    }

    private suspend fun executePredictRequest(request: ClientRequestProto) = try {
        stub.process(request)
    } catch (t: Throwable) {
        processResultFailure(t)
    }

    private suspend fun withRetry(action: suspend () -> ClientResponseProto): ClientResponseProto {
        val retryConfig = config.clientPredictRetryConfig

        var attempt = 0
        while (true) {
            attempt++
            val response = action()

            when {
                !response.hasError() || attempt >= retryConfig.maxAttempts ->
                    return response

                response.error.code in reconnectErrorCodes -> {
                    logger.warn("Reconnect error from gate, attempt $attempt/${retryConfig.maxAttempts}.")

                    reconnect()
                }

                response.error.code in retryConfig.retryableErrorCodes ->
                    logger.error("Error from gate, attempt $attempt/${retryConfig.maxAttempts}. Error \n${response.error}")

                else ->
                    return response
            }

            delay(retryConfig.backoffMs)
        }
    }

    private fun processResultFailure(exception: Throwable): Nothing = when (exception) {
        is TimeoutCancellationException ->
            throw MlpClientException("timeout", exception.message ?: "$exception", emptyMap(), MDC.get("requestId"))

        is StatusRuntimeException, is StatusException ->
            throw exception

        else ->
            throw MlpClientException(
                "wrong-response",
                exception.message ?: "$exception",
                emptyMap(),
                MDC.get("requestId")
            )
    }

    fun shutdown() {
        backoffJob.cancel()

        channel.get()?.shutdown()
        try {
            if (channel.get()?.awaitTermination(config.shutdownConfig.clientMs, TimeUnit.MILLISECONDS) != false) {
                logger.debug("Shutdown completed")
            } else {
                logger.error("Failed to shutdown grpc channel!")
            }
        } catch (e: InterruptedException) {
            logger.error("Interrupted while waiting for shutdown", e)
        }
    }

    private fun launchBackoffJob() = backoffCleanerScope.launch {
        val maxBackoffMs = ofSeconds(config.maxBackoffSeconds)
        var lastReadyTime = now()

        while (isActive) {
            delay(1000L)

            if (channel.get().getState(true) == READY) {
                lastReadyTime = now()
                continue
            }

            if (between(lastReadyTime, now()) > maxBackoffMs) {
                logger.warn("Channel is not ready for ${config.maxBackoffSeconds} seconds, resetting backoff")
                channel.get().resetConnectBackoff()
            }
        }
    }

    private fun buildPredictRequest(
        account: String,
        model: String,
        data: Payload,
        config: Payload?,
        timeout: Duration?,
        authToken: String,
        requestHeaders: Map<String, String> = emptyMap()
    ): ClientRequestProto {
        val builder = ClientRequestProto.newBuilder()

        builder
            .setAccount(account)
            .setModel(model)
            .setAuthToken(authToken)
            .setPredict(
                PredictRequestProto.newBuilder().apply {
                    this.data = PayloadProto.newBuilder()
                        .setDataType(data.dataType ?: "")
                        .setJson(data.data)
                        .build()

                    config?.let {
                        this.config = PayloadProto.newBuilder()
                            .setDataType(config.dataType ?: "")
                            .setJson(config.data)
                            .build()
                    }
                }
            )

        builder.putAllHeaders(requestHeaders)

        if (MDC.get("requestId") != null)
            builder.putHeaders("Z-requestId", MDC.get("requestId"))

        val billingKey = billingToken ?: MDC.get("MLP-BILLING-KEY")
        if (billingKey != null)
            builder.putHeaders("MLP-BILLING-KEY", billingKey)

        if (timeout != null)
            builder.timeoutSec = timeout.seconds.toInt()

        return builder.build()
    }

    private fun buildExtRequest(
        account: String,
        model: String,
        methodName: String,
        timeout: Duration?,
        authToken: String,
        params: Array<out Pair<String, PayloadProto>>
    ): ClientRequestProto {
        val builder = ClientRequestProto.newBuilder()

        builder
            .setAccount(account)
            .setModel(model)
            .setAuthToken(authToken)
            .setExt(
                ExtendedRequestProto.newBuilder().apply {
                    this.methodName = methodName
                    this.putAllParams(params.toMap())
                }
            )

        if (MDC.get("requestId") != null)
            builder.putHeaders("Z-requestId", MDC.get("requestId"))

        if (timeout != null)
            builder.timeoutSec = timeout.seconds.toInt()

        return builder.build()
    }

    private fun buildPartialPredict(
        account: String,
        model: String,
        data: PayloadInterface,
        config: Payload?,
        timeout: Duration?,
        authToken: String,
        requestHeaders: Map<String, String> = emptyMap(),
    ): ClientRequestProto {
        val builder = ClientRequestProto.newBuilder()

        builder
            .setAccount(account)
            .setModel(model)
            .setAuthToken(authToken)
            .setPartialPredict(
                PartialPredictRequestProto.newBuilder().apply {
                    this.data = PayloadProto.newBuilder()
                        .setDataType(data.dataType ?: "")
                        .apply {
                            when(data) {
                                is Payload -> setJson(data.data)
                                is ProtobufPayload -> setProtobuf(data.data)
                            }
                        }
                        .build()

                    config?.let {
                        this.config = PayloadProto.newBuilder()
                            .setDataType(config.dataType ?: "")
                            .setJson(config.data)
                            .build()
                    }
                }
            )

        builder.putAllHeaders(requestHeaders)

        if (MDC.get("requestId") != null)
            builder.putHeaders("Z-requestId", MDC.get("requestId"))

        val billingKey = billingToken ?: MDC.get("MLP-BILLING-KEY")
        if (billingKey != null)
            builder.putHeaders("MLP-BILLING-KEY", billingKey)

        if (timeout != null)
            builder.timeoutSec = timeout.seconds.toInt()

        return builder.build()
    }

    companion object {
        private val dispatcher = newDaemonSingleThreadExecutor().asCoroutineDispatcher()
        private val backoffCleanerScope = CoroutineScope(dispatcher + SupervisorJob())

        private fun newDaemonSingleThreadExecutor() =
            newSingleThreadExecutor { defaultThreadFactory().newThread(it).apply { isDaemon = true } }


        val reconnectErrorCodes: List<String> =
            listOf("mlp.gate.gate_is_shut_down")
    }
}
