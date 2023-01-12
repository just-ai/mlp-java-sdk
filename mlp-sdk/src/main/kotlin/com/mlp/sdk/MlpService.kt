package com.mlp.sdk

import com.mlp.gate.ServiceDescriptorProto
import com.mlp.sdk.CommonErrorCode.REQUEST_TYPE_NOT_SUPPORTED

interface MlpService {

    fun getDescriptor(): ServiceDescriptorProto {
        throw NotImplementedError()
    }

    fun predict(req: Payload, config: Payload?): MlpResponse {
        return predict(req)
    }

    fun predict(req: Payload): MlpResponse {
        throw MlpException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "predict"))
    }

    fun fit(train: Payload, targets: Payload, config: Payload?): MlpResponse {
        throw MlpException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "fit"))
    }

    fun ext(methodName: String, params: Map<String, Payload>): MlpResponse {
        throw MlpException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "ext"))
    }

    fun batch(requests: List<Payload>, config: Payload?): List<MlpResponse> {
        throw MlpException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "batch"))
    }
}
