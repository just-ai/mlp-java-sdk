package com.platform.mpl.sdk

import com.platform.mpl.gate.ActionDescriptorProto
import com.platform.mpl.sdk.CommonErrorCode.REQUEST_TYPE_NOT_SUPPORTED

interface PlatformAction {

    fun getDescriptor(): ActionDescriptorProto {
        throw NotImplementedError()
    }

    fun predict(req: Payload, config: Payload?): PlatformResponse {
        return predict(req)
    }

    fun predict(req: Payload): PlatformResponse {
        throw PlatformException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "predict"))
    }

    fun fit(train: Payload, targets: Payload, config: Payload?): PlatformResponse {
        throw PlatformException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "fit"))
    }

    fun ext(methodName: String, params: Map<String, Payload>): PlatformResponse {
        throw PlatformException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "ext"))
    }

    fun batch(req: List<Payload>): List<PlatformResponse> {
        throw PlatformException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "batch"))
    }
}
