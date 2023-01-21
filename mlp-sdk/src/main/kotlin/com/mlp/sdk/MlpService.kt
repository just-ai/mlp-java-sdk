package com.mlp.sdk

import com.mlp.gate.ServiceDescriptorProto
import com.mlp.sdk.CommonErrorCode.REQUEST_TYPE_NOT_SUPPORTED

abstract class MlpService {

    lateinit var pipelineClient: PipelineClient

    open fun getDescriptor(): ServiceDescriptorProto {
        throw NotImplementedError()
    }

    open fun predict(req: Payload, config: Payload?): MlpResponse {
        return predict(req)
    }

    open fun predict(req: Payload): MlpResponse {
        throw MlpException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "predict"))
    }

    open fun fit(train: Payload, targets: Payload?, config: Payload?, modelDir: String, previousModelDir: String?): MlpResponse {
        throw MlpException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "fit"))
    }

    open fun ext(methodName: String, params: Map<String, Payload>): MlpResponse {
        throw MlpException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "ext"))
    }

    open fun batch(requests: List<Payload>, config: Payload?): List<MlpResponse> {
        throw MlpException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "batch"))
    }
}
