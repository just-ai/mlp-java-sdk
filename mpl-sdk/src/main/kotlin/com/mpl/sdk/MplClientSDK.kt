package com.mpl.sdk

import com.mpl.gate.ClientRequestProto
import com.mpl.gate.ClientResponseProto
import com.mpl.gate.ExtendedRequestProto
import com.mpl.gate.GateGrpc
import com.mpl.gate.PayloadProto
import com.mpl.gate.PredictRequestProto
import com.mpl.sdk.utils.WithLogger
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Status.Code.UNAVAILABLE
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.Duration.ofMillis
import java.time.Instant.now
import java.util.concurrent.TimeUnit

class MplClientSDK(private val config: MplClientConfig = loadClientConfig()) : WithLogger {

    private lateinit var channel: ManagedChannel
    private lateinit var stub: GateGrpc.GateBlockingStub
    private lateinit var gateUrl: String
    private var token: String? = null

    fun init() {
        gateUrl = config.initialGateUrls.firstOrNull() ?: error("There is not MPL_URL")
        token = config.connectionToken
        logger.debug("Starting mpl client for url $gateUrl")
        connect()
    }

    private fun connect() {
        if (this::channel.isInitialized) {
            config.initialGateUrls[0]
        }

        val channelBuilder = ManagedChannelBuilder.forTarget(gateUrl)
        if (!config.grpcSecure) {
            channelBuilder.usePlaintext()
        }
        channel = channelBuilder.build()
        stub = GateGrpc.newBlockingStub(channel)
    }

    fun predict(
        account: String,
        model: String,
        payload: String,
    ): String {
        val authToken = requireNotNull(token) {
            "Set authToken in environment variables, or in init method, or directly in predict method"
        }
        return runBlocking {
            predictSuspendable(account, model, authToken, Payload(dataType = "", data = payload)).data
        }
    }

    fun predict(
        account: String,
        model: String,
        authToken: String,
        payload: String,
    ) = runBlocking {
        predictSuspendable(account, model, authToken, Payload(dataType = "", data = payload)).data
    }

    fun predict(
        account: String,
        model: String,
        authToken: String,
        payload: Payload,
        configPayload: Payload? = null
    ): Payload = runBlocking {
        predictSuspendable(account, model, authToken, payload, configPayload)
    }

    suspend fun predictSuspendable(
        account: String,
        model: String,
        authToken: String,
        payload: Payload,
        configPayload: Payload? = null
    ) = sendRequest(
        ClientRequestProto.newBuilder()
            .setAccount(account)
            .setModel(model)
            .setAuthToken(authToken)
            .setPredict(
                PredictRequestProto.newBuilder().apply {
                    data = PayloadProto.newBuilder()
                        .setDataType(payload.dataType)
                        .setJson(payload.data)
                        .build()
                    configPayload?.let {
                        config = PayloadProto.newBuilder()
                            .setDataType(configPayload.dataType)
                            .setJson(configPayload.data)
                            .build()
                    }
                }
            )
            .build()
    )


    fun ext(
        account: String,
        model: String,
        methodName: String,
        vararg params: Pair<String, PayloadProto>
    ): Payload {
        val authToken = requireNotNull(token) {
            "Set authToken in environment variables, or in init method, or directly in predict method"
        }

        return runBlocking {
            extSuspendable(account, model, authToken, methodName, *params)
        }
    }

    fun ext(
        account: String,
        model: String,
        authToken: String,
        methodName: String,
        vararg params: Pair<String, PayloadProto>
    ) = runBlocking {
        extSuspendable(account, model, authToken, methodName, *params)
    }

    suspend fun extSuspendable(
        account: String,
        model: String,
        authToken: String,
        methodName: String,
        vararg params: Pair<String, PayloadProto>
    ) = sendRequest(
        ClientRequestProto.newBuilder()
            .setAccount(account)
            .setModel(model)
            .setAuthToken(authToken)
            .setExt(
                ExtendedRequestProto.newBuilder().apply {
                    this.methodName = methodName
                    this.putAllParams(params.toMap())
                }
            )
            .build()
    )

    private suspend fun sendRequest(request: ClientRequestProto): Payload {
        val timeout = ofMillis(config.clientPredictTimeoutMs)
        val end = now() + timeout

        val response = executePredictRequest(request, timeout)
            ?: throw MplClientException("UNAVAILABLE", "Cannot connect after ${timeout.seconds} seconds", emptyMap())

        when {
            response.hasPredict() ->
                return Payload(response.predict.data.dataType, response.predict.data.json)

            response.hasExt() ->
                return Payload(response.ext.data.dataType, response.ext.data.json)

            response.hasError() -> {
                logger.error("Error from gate. Error \n${response.error}")
                throw MplClientException(response.error.code, response.error.message, response.error.argsMap)
            }

            else ->
                throw MplClientException("wrong-response", "Wrong response type: $response", emptyMap())
        }
    }

    private suspend fun executePredictRequest(request: ClientRequestProto, timeout: Duration): ClientResponseProto? {
        val end = now() + timeout

        var response: ClientResponseProto? = null

        while (now() < end) {
            val result = runCatching {
                stub.process(request)
            }
            if (result.isSuccess) {
                response = result.getOrThrow()
                break
            }
            result.onFailure {
                processResultFailure(it)
            }
            delay(1000)
        }
        return response
    }

    private fun processResultFailure(exception: Throwable) = when {
        exception is StatusRuntimeException
                && exception.status.code == UNAVAILABLE -> connect()

        exception is StatusRuntimeException ->
            throw MplClientException(exception.status.code.name, exception.message ?: "$exception", emptyMap())

        else ->
            throw MplClientException("wrong-response", exception.message ?: "$exception", emptyMap())
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

    private fun predictRequestProto(
        payload: Payload,
        configPayload: Payload?
    ) = PredictRequestProto.newBuilder().apply {
        data = PayloadProto.newBuilder()
            .setDataType(payload.dataType)
            .setJson(payload.data)
            .build()
        configPayload?.let {
            config = PayloadProto.newBuilder()
                .setDataType(configPayload.dataType)
                .setJson(configPayload.data)
                .build()
        }
    }.build()
}