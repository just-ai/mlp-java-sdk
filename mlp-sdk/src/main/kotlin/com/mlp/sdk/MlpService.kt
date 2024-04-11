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

    open suspend fun predict(req: Payload, config: Payload?): MlpResponse {
        return predict(req)
    }

    open suspend fun predict(req: Payload): MlpResponse {
        throw MlpException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "predict"))
    }

    open suspend fun fit(
        train: Payload, targets: Payload?, config: Payload?, modelDir: String, previousModelDir: String?,
        targetServiceInfo: ServiceInfoProto, dataset: DatasetInfoProto, percentageConsumer: suspend (Int) -> Unit
    ): MlpResponse =
        fit(train, targets, config, modelDir, previousModelDir, targetServiceInfo, dataset)

    open suspend fun fit(
        train: Payload, targets: Payload?, config: Payload?, modelDir: String, previousModelDir: String?,
        targetServiceInfo: ServiceInfoProto, dataset: DatasetInfoProto
    ): MlpResponse {
        throw MlpException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "fit"))
    }

    open suspend fun ext(methodName: String, params: Map<String, Payload>): MlpResponse {
        throw MlpException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "ext"))
    }

    open suspend fun batch(requests: List<Payload>, config: Payload?): List<MlpResponse> {
        throw MlpException(REQUEST_TYPE_NOT_SUPPORTED, mapOf("type" to "batch"))
    }
}
