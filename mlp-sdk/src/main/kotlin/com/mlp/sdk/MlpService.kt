package com.mlp.sdk

import com.mlp.gate.DatasetInfoProto
import com.mlp.gate.ServiceDescriptorProto
import com.mlp.gate.ServiceInfoProto
import com.mlp.sdk.CommonErrorCode.REQUEST_TYPE_NOT_SUPPORTED
import com.mlp.sdk.MlpExecutionContext.Companion.systemContext

abstract class MlpService : WithExecutionContext {

    override val context: MlpExecutionContext = systemContext

    open fun getDescriptor(): ServiceDescriptorProto {
        throw NotImplementedError()
    }

    open fun predict(req: Payload, config: Payload?): MlpResponse {
        return predict(req)
    }

    open fun predict(req: Payload): MlpResponse {
        throw MlpException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "predict"))
    }

    open fun fit(
        train: Payload, targets: Payload?, config: Payload?, modelDir: String, previousModelDir: String?,
        targetServiceInfo: ServiceInfoProto,
        dataset: DatasetInfoProto
    ): MlpResponse {
        throw MlpException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "fit"))
    }

    open fun ext(methodName: String, params: Map<String, Payload>): MlpResponse {
        throw MlpException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "ext"))
    }

    open fun batch(requests: List<Payload>, config: Payload?): List<MlpResponse> {
        throw MlpException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "batch"))
    }
}
