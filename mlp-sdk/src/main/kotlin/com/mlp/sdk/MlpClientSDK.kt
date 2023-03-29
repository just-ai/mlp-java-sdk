package com.mlp.sdk

import com.mlp.gate.ClientRequestProto
import com.mlp.gate.ExtendedRequestProto
import com.mlp.gate.GateGrpcKt.GateCoroutineStub
import com.mlp.gate.PayloadProto
import com.mlp.gate.PredictRequestProto
import com.mlp.sdk.utils.WithLogger
import io.grpc.ConnectivityState.READY
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.StatusRuntimeException
import kotlin.Int.Companion.MAX_VALUE
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors.defaultThreadFactory
import java.util.concurrent.Executors.newSingleThreadExecutor
import java.util.concurrent.TimeUnit

class MlpClientSDK(
    private val config: MlpClientConfig = loadClientConfig(),
) : WithLogger {

    private lateinit var channel: ManagedChannel
    private lateinit var stub: GateCoroutineStub
    private var token: String? = null

    val apiClient by lazy { MlpApiClient.getInstance(config.connectionToken, config.clientApiGateUrl) }

    init {
        val gateUrl = config.initialGateUrls.firstOrNull() ?: error("There is not MLP_GRPC_HOST")
        token = config.connectionToken
        logger.debug("Starting mlp client for url $gateUrl")
        connect(gateUrl)

        launchBackoffJob()
    }

    private fun connect(gateUrl: String) {
        val channelBuilder = ManagedChannelBuilder.forTarget(gateUrl)
            .enableRetry()
            .maxRetryAttempts(MAX_VALUE)

        if (!config.grpcSecure)
            channelBuilder.usePlaintext()
        channel = channelBuilder.build()

        stub = GateCoroutineStub(channel)
    }

    /**
     * Connection may be IDLE because of inactivity some time ago. If you want to await for connection to be ready, use [awaitConnection] instead.
     */
    fun isConnected() =
        channel.getState(true) == READY

    /**
     * Await for connection to be ready. If connection is already ready, returns immediately.
     */
    suspend fun awaitConnection() {
        while (true) {
            val state = channel.getState(true)

            if (state == READY)
                break

            suspendCancellableCoroutine<Unit> {
                channel.notifyWhenStateChanged(state) { it.resume(Unit) }
            }
        }
    }

    fun predictBlocking(
        account: String,
        model: String,
        data: String,
        config: String? = null,
        timeout: Duration? = null,
        authToken: String = ensureDefaultToken()
    ) =
        runBlocking { predict(account, model, data, config, timeout, authToken) }

    fun predictBlocking(
        account: String,
        model: String,
        data: Payload,
        config: Payload? = null,
        timeout: Duration? = null,
        authToken: String = ensureDefaultToken()
    ) =
        runBlocking { predict(account, model, data, config, timeout, authToken) }

    suspend fun predict(
        account: String,
        model: String,
        data: String,
        config: String? = null,
        timeout: Duration? = null,
        authToken: String = ensureDefaultToken()
    ) =
        predict(account, model, Payload("", data), config?.let { Payload("", data) }, timeout, authToken).data

    suspend fun predict(
        account: String,
        model: String,
        data: Payload,
        config: Payload? = null,
        timeout: Duration? = null,
        authToken: String = ensureDefaultToken()
    ) =
        sendRequest(buildPredictRequest(account, model, data, config, timeout, authToken), timeout)

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

    private fun ensureDefaultToken() = requireNotNull(token) {
        "Set authToken in environment variables, or in init method, or directly in predict method"
    }

    private suspend fun sendRequest(request: ClientRequestProto, timeout: Duration?): Payload {
        val timeoutMs = timeout?.toMillis() ?: config.clientPredictTimeoutMs
        val response = withTimeout(timeoutMs) { executePredictRequest(request) }

        return when {
            response.hasPredict() ->
                Payload(response.predict.data.dataType, response.predict.data.json)

            response.hasExt() ->
                Payload(response.ext.data.dataType, response.ext.data.json)

            response.hasError() -> {
                logger.error("Error from gate. Error \n${response.error}")
                throw MlpClientException(response.error.code, response.error.message, response.error.argsMap)
            }

            else ->
                throw MlpClientException("wrong-response", "Wrong response type: $response", emptyMap())
        }
    }

    private suspend fun executePredictRequest(request: ClientRequestProto) = try {
        stub.process(request)
    } catch (t: Throwable) {
        processResultFailure(t)
    }

    private fun processResultFailure(exception: Throwable): Nothing = when (exception) {
        is StatusRuntimeException ->
            throw MlpClientException(exception.status.code.name, exception.message ?: "$exception", emptyMap())

        is TimeoutCancellationException ->
            throw MlpClientException("timeout", exception.message ?: "$exception", emptyMap())

        else ->
            throw MlpClientException("wrong-response", exception.message ?: "$exception", emptyMap())
    }

    fun shutdown() {
        channel.shutdown()
        try {
            if (channel.awaitTermination(config.shutdownConfig.clientMs, TimeUnit.MILLISECONDS)) {
                logger.debug("Shutdown completed")
            } else {
                logger.error("Failed to shutdown gprc channel!")
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

            if (channel.getState(true) == READY) {
                lastReadyTime = now()
                continue
            }

            if (between(lastReadyTime, now()) > maxBackoffMs) {
                logger.warn("Channel is not ready for ${config.maxBackoffSeconds} seconds, resetting backoff")
                channel.resetConnectBackoff()
            }
        }
    }

    private fun buildPredictRequest(
        account: String,
        model: String,
        data: Payload,
        config: Payload?,
        timeout: Duration?,
        authToken: String
    ): ClientRequestProto {
        val builder = ClientRequestProto.newBuilder()

        builder
            .setAccount(account)
            .setModel(model)
            .setAuthToken(authToken)
            .setPredict(
                PredictRequestProto.newBuilder().apply {
                    this.data = PayloadProto.newBuilder()
                        .setDataType(data.dataType)
                        .setJson(data.data)
                        .build()

                    config?.let {
                        this.config = PayloadProto.newBuilder()
                            .setDataType(config.dataType)
                            .setJson(config.data)
                            .build()
                    }
                }
            )

        if (MDC.get("requestId") != null)
            builder.putHeaders("Z-requestId", MDC.get("requestId"))

        if (timeout != null)
            builder.timeoutSec = timeout.toSeconds().toInt()

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
            builder.timeoutSec = timeout.toSeconds().toInt()

        return builder.build()
    }

    companion object {
        private val dispatcher = newDaemonSingleThreadExecutor().asCoroutineDispatcher()
        private val backoffCleanerScope = CoroutineScope(dispatcher + SupervisorJob())

        private fun newDaemonSingleThreadExecutor() =
            newSingleThreadExecutor { defaultThreadFactory().newThread(it).apply { isDaemon = true } }
    }
}
