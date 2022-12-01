package com.justai.caila.sdk

import com.justai.caila.gate.ActionDescriptorProto
import com.justai.caila.sdk.CommonErrorCode.REQUEST_TYPE_NOT_SUPPORTED

interface CailaAction {

    fun getDescriptor(): ActionDescriptorProto {
        throw NotImplementedError()
    }

    fun predict(req: Payload, config: Payload?): CailaResponse {
        return predict(req)
    }

    fun predict(req: Payload): CailaResponse {
        throw CailaException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "predict"))
    }

    fun fit(train: Payload, targets: Payload, config: Payload?): CailaResponse {
        throw CailaException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "fit"))
    }

    fun ext(methodName: String, params: Map<String, Payload>): CailaResponse {
        throw CailaException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "ext"))
    }

    fun batch(req: List<Payload>): List<CailaResponse> {
        throw CailaException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "batch"))
    }
}
