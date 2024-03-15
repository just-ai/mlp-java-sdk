package com.mlp.sdk

import com.mlp.api.ApiClient
import com.mlp.api.client.*
import com.mlp.gate.*
import com.mlp.sdk.MlpExecutionContext.Companion.systemContext
import com.mlp.sdk.utils.JSON
import kotlinx.coroutines.runBlocking
import org.slf4j.MDC

abstract class MlpServiceBase<F: Any, FC: Any, P: Any, C: Any, R: Any>(
    val fitDataExample: F,
    val fitConfigExample: FC,
    val predictRequestExample: P,
    val predictConfigExample: C,
    val predictResponseExample: R,
): MlpService() {

    lateinit var sdk: MlpServiceSDK

    final override fun getDescriptor(): ServiceDescriptorProto {
        return ServiceDescriptorProto.newBuilder()
            .setName(this.javaClass.simpleName)
            .putMethods("fit", MethodDescriptorProto.newBuilder()
                .putInput("train", ParamDescriptorProto.newBuilder().setType(fitDataExample.javaClass.canonicalName).build())
                .putInput("config", ParamDescriptorProto.newBuilder().setType(fitConfigExample.javaClass.canonicalName).build())
                .build()
            )
            .putMethods("predict", MethodDescriptorProto.newBuilder()
                .putInput("data", ParamDescriptorProto.newBuilder().setType(predictRequestExample.javaClass.canonicalName).build())
                .putInput("config", ParamDescriptorProto.newBuilder().setType(predictConfigExample.javaClass.canonicalName).build())
                .setOutput(ParamDescriptorProto.newBuilder().setType(predictResponseExample.javaClass.canonicalName).build())
                .build()
            )
            .putSchemaFiles("predictRequest-example.json", JSON.stringify(predictRequestExample))
            .putSchemaFiles("predictConfig-example.json", JSON.stringify(predictConfigExample))
            .putSchemaFiles("predictResponse-example.json", JSON.stringify(predictResponseExample))
            .putSchemaFiles("fitData-example.json", JSON.stringify(fitDataExample))
            .putSchemaFiles("fitConfig-example.json", JSON.stringify(fitConfigExample))
            .build()
    }

    final override suspend fun fit(
        train: Payload,
        targets: Payload?,
        config: Payload?,
        modelDir: String,
        previousModelDir: String?,
        targetServiceInfo: ServiceInfoProto,
        dataset: DatasetInfoProto
    ): MlpResponse {
        @Suppress("UNCHECKED_CAST")
        val data =
            if (fitDataExample is Payload) train as F
            else JSON.parse(train.data, fitDataExample.javaClass)
        val config0 = if (config != null) JSON.parse(config.data, fitConfigExample.javaClass) else null

        fit(data, config0, modelDir, previousModelDir, targetServiceInfo, dataset)

        return Payload(
            dataType = "text/plain",
            data = "ok"
        )
    }

    abstract suspend fun fit(data: F, config: FC?, modelDir: String, previousModelDir: String?, targetServiceInfo: ServiceInfoProto,
                     dataset: DatasetInfoProto)

    override suspend fun predict(req: Payload, config: Payload?): MlpResponse {
        // парсим request и config.
        val request = JSON.parse(req.data, predictRequestExample.javaClass) // TODO: handle datatype

        val conf = if (config != null && predictConfigExample !is Unit) {
            JSON.parse(config.data, predictConfigExample.javaClass)
        } else null

        // вызываем predict
        val res = this.predict(request, conf)

        // в зависимости от того, создали ли генератор стрим, возвращаем ответ либо сразу, либо возвращаем пустой ответ
        return if (res != null) {
            Payload(data = JSON.stringify(res), dataType = "json") // TODO: fill datatype
        } else {
            MlpPartialBinaryResponse() // возвращаем пустоту, далее ответы будут публиковаться в стрим-генератор
        }
    }

    fun createGenerator(): ResultGenerator<R> {
        val requestId = MDC.get("gateRequestId").toLong()
        val connectorId = MDC.get("connectorId").toLong()

        return ResultGenerator<R> { resultAndFinish ->
            val builder = ServiceToGateProto.newBuilder()
                .setRequestId(requestId)
                .setPartialPredict(
                    PartialPredictResponseProto.newBuilder()
                        .setFinish(resultAndFinish.last)
                        .setData(
                            PayloadProto.newBuilder()
                                .setJson(JSON.stringify(resultAndFinish.result))
                                .setDataType("json") // TODO: fill reference to data-schema
                        )
                )
            if (resultAndFinish.price != null) {
                builder.putHeaders("Z-custom-billing", resultAndFinish.price.toString())
            }

            sdk.send(connectorId, builder.build())
        }
    }

    data class ResultAndFinish<R>(
        val result: R,
        val price: Long?,
        val last: Boolean,
    )

    class ResultGenerator<R>(
        private val publisher: suspend (ResultAndFinish<R>) -> Unit
    ) {
        suspend fun next(result: R, last: Boolean, price: Long? = null) {
            publisher(ResultAndFinish(result, price, last))
        }
    }

    abstract suspend fun predict(request: P, config: C?): R?

}

abstract class MlpFitServiceBase<F: Any, FC: Any>(
    fitDataExample: F,
    fitConfigExample: FC,
): MlpServiceBase<F, FC, String, Unit, String>(fitDataExample, fitConfigExample, "", Unit, "") {

    final override suspend fun predict(request: String, config: Unit?): String? {
        throw RuntimeException("Not implemented yet")
    }
}

abstract class MlpPredictServiceBase<P: Any, R: Any>(
    predictRequestExample: P,
    predictResponseExample: R,
): MlpServiceBase<String, String, P, Unit, R>("", "", predictRequestExample, Unit, predictResponseExample) {

    final override suspend fun fit(data: String, config: String?, modelDir: String, previousModelDir: String?, targetServiceInfo: ServiceInfoProto,
                     dataset: DatasetInfoProto) {
        throw RuntimeException("Predict service doesn't support fit method")
    }

    override suspend fun predict(request: P, config: Unit?): R? {
        return predict(request, config)
    }

    abstract fun predict(req: P): R

}

abstract class MlpPredictWithConfigServiceBase<P: Any, C: Any, R: Any>(
    predictRequestExample: P,
    predictConfigExample: C,
    predictResponseExample: R,
): MlpServiceBase<String, String, P, C, R>("", "", predictRequestExample, predictConfigExample, predictResponseExample) {

    final override suspend fun fit(data: String, config: String?, modelDir: String, previousModelDir: String?, targetServiceInfo: ServiceInfoProto,
                           dataset: DatasetInfoProto) {
        throw RuntimeException("Predict service doesn't support fit method")
    }
}

class MlpRestClient (
    restUrl: String? = null,
    clientToken: String? = null,
    billingToken: String? = null,
    override val context: MlpExecutionContext = systemContext
): WithExecutionContext {

    @Deprecated("Use accountId instead")
    val ACCOUNT_ID
        get() = environment.getOrThrow("MLP_ACCOUNT_ID")
    @Deprecated("Use modelId instead")
    val MODEL_ID
        get() = environment.getOrThrow("MLP_MODEL_ID")

    val accountId = environment["MLP_ACCOUNT_ID"]
    val modelId = environment["MLP_MODEL_ID"]

    val apiClient: ApiClient

    val processApi: ProcessEndpointApi
    val modelApi: ModelEndpointApi
    val jobApi: JobEndpointApi
    val datasetApi: DatasetEndpointApi

    init {
        apiClient = ApiClient().apply {
            basePath = restUrl ?: environment.getOrThrow("MLP_REST_URL")
            addDefaultHeader("MLP-API-KEY", clientToken ?: environment.getOrThrow("MLP_CLIENT_TOKEN"))
            billingToken?.let { addDefaultHeader("MLP-BILLING-KEY", it) }
        }

        processApi = ProcessEndpointApi(apiClient)
        modelApi = ModelEndpointApi(apiClient)
        jobApi = JobEndpointApi(apiClient)
        datasetApi = DatasetEndpointApi(apiClient)
    }
}
