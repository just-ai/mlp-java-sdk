package com.mpl.sdk

import com.fasterxml.jackson.databind.ObjectMapper
import com.mpl.gate.ActionToGateProto
import com.mpl.gate.ClientTokenRequestProto
import com.mpl.gate.ExtendedRequestProto
import com.mpl.gate.PipelineRequestProto
import com.mpl.gate.PipelineResponseProto
import com.mpl.gate.PredictRequestProto
import com.mpl.api.ApiClient
import com.mpl.sdk.Payload.Companion.emptyPayload
import kotlin.concurrent.schedule
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicLong

class PipelineClient(
    private val sdk: MplActionSDK,
    private val mplConfig: MplActionConfig,
    private val restTemplate: RestTemplate = getRestTemplate()
) {

    private val timer = Timer(true)
    private val requests = ConcurrentHashMap<Long, CompletableFuture<PipelineResponseProto>>()

    lateinit var modelInfo: ModelInfo

    val clientApiToken by lazy {
        requestClientApiToken()
    }

    val apiClient: ApiClient by lazy {
        ApiClient(restTemplate).apply {
            basePath = mplConfig.clientApiGateUrl
            addDefaultHeader("MPL-API-KEY", clientApiToken.token)
        }
    }

    fun predict(model: String, data: Payload, config: Payload = emptyPayload) =
        predict(null, model, data, config)

    fun predict(account: String?, model: String, data: Payload, config: Payload = emptyPayload) =
        sendRequest { buildPredictProto(it, account, model, data, config) }

    fun ext(model: String, method: String, vararg params: Pair<String, Payload>) =
        ext(null, model, method, *params)

    fun ext(account: String?, model: String, method: String, vararg params: Pair<String, Payload>) =
        sendRequest { buildExtProto(it, account, model, method, *params) }

    private fun requestClientApiToken() =
        sendRequest { buildClientTokenProto(it) }
            .get()
            .token

    private fun sendRequest(protoBuilder: (Long) -> ActionToGateProto): CompletableFuture<PipelineResponseProto> {
        val requestId = lastId.getAndDecrement()
        val future = CompletableFuture<PipelineResponseProto>()
            .orTimeout(mplConfig.pipeFutureTimeoutMs, SECONDS)
        requests[requestId] = future

        timer.schedule(mplConfig.pipeFutureScheduleMs) { requests.remove(requestId) }
        sdk.sendToAnyGate(protoBuilder(requestId))

        return future
    }

    fun registerResponse(requestId: Long, toActionProto: PipelineResponseProto) {
        requests[requestId]
            ?.complete(toActionProto)
    }

    private fun buildPredictProto(
        requestId: Long,
        account: String?,
        model: String,
        data: Payload,
        config: Payload
    ) = ActionToGateProto.newBuilder()
        .setRequestId(requestId)
        .setRequest(
            PipelineRequestProto.newBuilder().also {
                if (account != null)
                    it.account = account
                it.model = model
                it.predict = PredictRequestProto.newBuilder()
                    .setData(data.asProto)
                    .setConfig(config.asProto)
                    .build()
            }
        )
        .build()

    private fun buildExtProto(
        requestId: Long,
        account: String?,
        model: String,
        methodName: String,
        vararg params: Pair<String, Payload>
    ) = ActionToGateProto.newBuilder()
        .setRequestId(requestId)
        .setRequest(
            PipelineRequestProto.newBuilder().also {
                if (account != null)
                    it.account = account
                it.model = model
                it.ext = ExtendedRequestProto.newBuilder()
                    .setMethodName(methodName)
                    .putAllParams(params.toMap().mapValues { (name, payload) ->
                        payload.asProto.build()
                    })
                    .build()
            }
        )
        .build()

    private fun buildClientTokenProto(
        requestId: Long
    ) = ActionToGateProto.newBuilder()
        .setRequestId(requestId)
        .setRequest(
            PipelineRequestProto.newBuilder().also {
                it.token = ClientTokenRequestProto.newBuilder().build()
            }
        )
        .build()

    companion object {
        private val lastId = AtomicLong(-1)

        private fun getRestTemplate(): RestTemplate {
            val restTemplate = RestTemplate()

            val jacksonConverter = restTemplate.messageConverters.find {
                it is MappingJackson2HttpMessageConverter
            } as MappingJackson2HttpMessageConverter

            jacksonConverter.objectMapper = ObjectMapper()

            return restTemplate
        }
    }
}
