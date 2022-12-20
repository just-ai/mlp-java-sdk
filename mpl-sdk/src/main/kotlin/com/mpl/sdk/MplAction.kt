package com.mpl.sdk

import com.mpl.gate.ActionDescriptorProto
import com.mpl.sdk.CommonErrorCode.REQUEST_TYPE_NOT_SUPPORTED

abstract class MplAction {

    lateinit var pipelineClient: PipelineClient

    open fun getDescriptor(): ActionDescriptorProto {
        throw NotImplementedError()
    }

    open fun predict(req: Payload, config: Payload?): MplResponse {
        return predict(req)
    }

    open fun predict(req: Payload): MplResponse {
        throw MplException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "predict"))
    }

    open fun fit(train: Payload, targets: Payload, config: Payload?, modelDir: String, previousModelDir: String?): MplResponse {
        throw MplException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "fit"))
    }

    open fun ext(methodName: String, params: Map<String, Payload>): MplResponse {
        throw MplException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "ext"))
    }

    open fun batch(req: List<BatchPayload>): List<MplResponse> {
        throw MplException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "batch"))
    }
}

data class BatchPayload(
    val request: Payload,
    val config: Payload? = null
)
