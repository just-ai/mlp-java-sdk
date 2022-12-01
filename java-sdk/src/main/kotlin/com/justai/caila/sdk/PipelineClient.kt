package com.justai.caila.sdk

import com.justai.caila.gate.ActionToGateProto
import com.justai.caila.gate.ClientRequestProto
import com.justai.caila.gate.ClientResponseProto
import com.justai.caila.gate.PredictRequestProto
import com.justai.caila.sdk.Payload.Companion.emptyPayload
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.schedule

class PipelineClient(private val sdk: CailaActionSDK, private val cailaConfig: CailaActionConfig) {

    private val timer = Timer(true)
    private val requests = ConcurrentHashMap<Long, CompletableFuture<ClientResponseProto>>()

    fun predict(model: String, data: Payload, config: Payload = emptyPayload) =
        predict(null, model, data, config)

    private fun predict(
        account: String?,
        model: String,
        data: Payload,
        config: Payload = emptyPayload
    ): CompletableFuture<ClientResponseProto> {

        val requestId = lastId.getAndDecrement()
        val future = CompletableFuture<ClientResponseProto>()
            .orTimeout(cailaConfig.pipeFutureTimeoutMs, SECONDS)
        requests[requestId] = future

        val toGateProto = buildToGateProto(requestId, account, model, data, config)
        timer.schedule(cailaConfig.pipeFutureScheduleMs) { requests.remove(requestId) }

        sdk.sendToAnyGate(toGateProto)
        return future
    }

    fun registerResponse(requestId: Long, toActionProto: ClientResponseProto) {
        requests[requestId]
            ?.complete(toActionProto)
    }

    private fun buildToGateProto(
        requestId: Long,
        account: String?,
        model: String,
        data: Payload,
        config: Payload
    ) = ActionToGateProto.newBuilder()
        .setRequestId(requestId)
        .setRequest(
            ClientRequestProto.newBuilder().also {
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

    companion object {
        private val lastId = AtomicLong(-1)
    }
}
