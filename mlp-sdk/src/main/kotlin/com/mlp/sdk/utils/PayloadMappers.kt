package com.mlp.sdk.utils

import com.mlp.gate.ApiErrorProto
import com.mlp.gate.BatchPayloadResponseProto
import com.mlp.gate.BatchResponseProto
import com.mlp.gate.ExtendedResponseProto
import com.mlp.gate.FitResponseProto
import com.mlp.gate.PartialPredictResponseProto
import com.mlp.gate.PayloadProto
import com.mlp.gate.PredictResponseProto
import com.mlp.gate.ServiceToGateProto.Builder
import com.mlp.sdk.BillingUnitsThreadLocal
import com.mlp.sdk.CommonErrorCode
import com.mlp.sdk.CommonErrorCode.PROCESSING_EXCEPTION
import com.mlp.sdk.MlpException
import com.mlp.sdk.MlpPartialBinaryResponse
import com.mlp.sdk.MlpResponse
import com.mlp.sdk.MlpResponseException
import com.mlp.sdk.Payload
import com.mlp.sdk.Payload.Companion.emptyPayload
import com.mlp.sdk.PayloadInterface
import com.mlp.sdk.ProtobufPayload
import com.mlp.sdk.RawPayload

internal val PayloadInterface.asProto
    get() = PayloadProto.newBuilder().also { builder ->
        when (this) {
            is Payload -> builder.setJson(data)
            is RawPayload -> builder.setJson(data)
            is ProtobufPayload -> builder.setProtobuf(data)
        }
        dataType?.let { builder.dataType = it }
    }

internal fun PayloadProto.getAsPayload(noContentLogging: Boolean): Payload =
    Payload(dataType, json, noContentLogging)

internal fun PayloadProto.getAsPayloadInterface(noContentLogging: Boolean): PayloadInterface =
    if (hasJson()) Payload(dataType, json, noContentLogging) else ProtobufPayload(dataType, protobuf, noContentLogging)

internal fun Builder.setPredict(prediction: PayloadInterface, headers: Map<String, String>?, statusCode: Int?) {
    val messageHeaders = headers?.toMutableMap() ?: mutableMapOf()

    flushBillingHeaders(messageHeaders)

    setPredict(
        PredictResponseProto
            .newBuilder()
            .setData(prediction.asProto)
            .setStatusCode(statusCode ?: 200)
            .putAllHeaders(messageHeaders)
    )
    putAllHeaders(messageHeaders)
}

internal fun Builder.setStartPartialPredict(headers: Map<String, String>?, statusCode: Int?) {
    val messageHeaders = headers?.toMutableMap() ?: mutableMapOf()

    flushBillingHeaders(messageHeaders)

    setPartialPredict(
        PartialPredictResponseProto
            .newBuilder()
            .setData(emptyPayload.asProto)
            .setStart(true)
            .setStatusCode(statusCode ?: 200)
            .putAllHeaders(messageHeaders)
    )
    putAllHeaders(messageHeaders)
}

internal fun Builder.setPartialPredict(prediction: PayloadInterface, last: Boolean) {
    val messageHeaders = headers.toMutableMap()

    flushBillingHeaders(messageHeaders)

    setPartialPredict(
        PartialPredictResponseProto
            .newBuilder()
            .setData(prediction.asProto)
            .setFinish(last)
    )
}


internal fun Builder.setFit() =
    setFit(FitResponseProto.newBuilder())

internal fun Builder.setExt(extResult: PayloadInterface, headers: Map<String, String>, statusCode: Int): Builder? {
    return setExt(
        ExtendedResponseProto
            .newBuilder()
            .setData(extResult.asProto)
            .setStatusCode(statusCode)
            .putAllHeaders(headers)
    ).putAllHeaders(headers)
}

internal fun Builder.setBatch(batchResult: List<MlpResponse>, requestsIdes: List<Long>): Builder {
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
                        .setStatusCode(CommonErrorCode.PARTIAL_RESPONSE_NOT_SUPPORTED_IN_BATCH.status.number)
                )

                is RawPayload -> builder.setError(
                    ApiErrorProto.newBuilder()
                        .setCode(CommonErrorCode.RAW_PAYLOAD_NOT_SUPPORTED_IN_BATCH.code)
                        .setMessage(CommonErrorCode.RAW_PAYLOAD_NOT_SUPPORTED_IN_BATCH.message)
                        .setStatusCode(CommonErrorCode.RAW_PAYLOAD_NOT_SUPPORTED_IN_BATCH.status.number)
                )
            }
            builder.build()
        }

    return setBatch(BatchResponseProto.newBuilder().addAllData(actionToGateProtos))
}

private fun flushBillingHeaders(messageHeaders: MutableMap<String, String>) {
    BillingUnitsThreadLocal.getUnits()?.also {
        messageHeaders += CUSTOM_BILLING_HEADER to it.toString()
    }
    BillingUnitsThreadLocal.getDetailedUnits()?.also {
        messageHeaders += CUSTOM_BILLING_DETAILS_HEADER to JSON.stringify(it)
    }
    BillingUnitsThreadLocal.getSelfCostUnits()?.also {
        messageHeaders += CUSTOM_SELF_COST_HEADER to it.toString()
    }
    BillingUnitsThreadLocal.getSelfCostCurrency()?.also {
        messageHeaders += CUSTOM_SELF_COST_CURRENCY_HEADER to it
    }
    // Deferred billing headers
    BillingUnitsThreadLocal.getDeferredBillingRequestId()?.also {
        messageHeaders += DEFERRED_BILLING_ID_HEADER to it
    }
    BillingUnitsThreadLocal.getRecurringBillingRequestId()?.also {
        messageHeaders += RECURRING_BILLING_ID_HEADER to it
    }
    BillingUnitsThreadLocal.getBillingCurrencyType()?.also {
        messageHeaders += BILLING_CURRENCY_TYPE_HEADER to it
    }

    BillingUnitsThreadLocal.clearAll()
}

internal val Throwable.asErrorProto
    get() = when (this) {
        is MlpException -> {
            var message = error.errorCode.message
            error.args.forEach { (key, value) -> message = message.replace("\${$key}", value) }

            ApiErrorProto.newBuilder()
                .setCode(error.errorCode.code)
                .setMessage(message)
                .setStatus(error.errorCode.status)
                .setStatusCode(error.errorCode.statusCode)
                .putAllArgs(error.args)
        }

        else -> {
            ApiErrorProto.newBuilder()
                .setCode(PROCESSING_EXCEPTION.code)
                .setMessage(PROCESSING_EXCEPTION.message)
                .setStatus(PROCESSING_EXCEPTION.status)
                .setStatusCode(PROCESSING_EXCEPTION.statusCode)
                .putArgs("message", message ?: "")
        }

    }
