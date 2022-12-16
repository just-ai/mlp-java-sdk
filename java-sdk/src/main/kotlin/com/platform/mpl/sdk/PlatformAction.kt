package com.platform.mpl.sdk

import com.platform.mpl.gate.ActionDescriptorProto
import com.platform.mpl.sdk.CommonErrorCode.REQUEST_TYPE_NOT_SUPPORTED

abstract class PlatformAction {

    lateinit var pipelineClient: PipelineClient

    open fun getDescriptor(): ActionDescriptorProto {
        throw NotImplementedError()
    }

    open fun predict(req: Payload, config: Payload?): PlatformResponse {
        return predict(req)
    }

    open fun predict(req: Payload): PlatformResponse {
        throw PlatformException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "predict"))
    }

    open fun fit(train: Payload, targets: Payload, config: Payload?, modelDir: String, previousModelDir: String): PlatformResponse {
        throw PlatformException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "fit"))
    }

    open fun ext(methodName: String, params: Map<String, Payload>): PlatformResponse {
        throw PlatformException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "ext"))
    }

    open fun batch(req: List<Payload>): List<PlatformResponse> {
        throw PlatformException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "batch"))
    }
}
