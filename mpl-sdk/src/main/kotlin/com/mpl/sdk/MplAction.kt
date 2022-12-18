package com.mpl.sdk

import com.mpl.gate.ActionDescriptorProto
import com.mpl.sdk.CommonErrorCode.REQUEST_TYPE_NOT_SUPPORTED

interface MplAction {

    fun getDescriptor(): ActionDescriptorProto {
        throw NotImplementedError()
    }

    fun predict(req: Payload, config: Payload?): MplResponse {
        return predict(req)
    }

    fun predict(req: Payload): MplResponse {
        throw MplException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "predict"))
    }

    fun fit(train: Payload, targets: Payload, config: Payload?): MplResponse {
        throw MplException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "fit"))
    }

    fun ext(methodName: String, params: Map<String, Payload>): MplResponse {
        throw MplException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "ext"))
    }

    fun batch(req: List<BatchPayload>): List<MplResponse> {
        throw MplException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "batch"))
    }
}

data class BatchPayload(
    val request: Payload,
    val config: Payload? = null
)
