package com.mlp.sdk

import com.mlp.api.ApiClient
import com.mlp.api.client.*
import com.mlp.api.client.model.DatasetInfoWithContentData
import com.mlp.api.client.model.ModelInfoPK
import com.mlp.gate.*
import com.mlp.sdk.utils.JSON
import org.slf4j.LoggerFactory
import java.lang.RuntimeException

abstract class MlpServiceBase<F: Any, FC: Any, P: Any, R: Any>(
    val fitDataExample: F,
    val fitConfigExample: FC,
    val predictRequestExample: P,
    val predictResponseExample: R,
): MlpService() {

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
                .setOutput(ParamDescriptorProto.newBuilder().setType(predictResponseExample.javaClass.canonicalName).build())
                .build()
            )
            .putSchemaFiles("request-example.json", JSON.stringify(predictRequestExample))
            .putSchemaFiles("response-example.json", JSON.stringify(predictResponseExample))
            .putSchemaFiles("fitData-example.json", JSON.stringify(fitDataExample))
            .putSchemaFiles("fitConfig-example.json", JSON.stringify(fitConfigExample))
            .build()
    }

    final override fun fit(
        train: Payload,
        targets: Payload?,
        config: Payload?,
        modelDir: String,
        previousModelDir: String?,
        targetServiceInfo: ServiceInfoProto,
        dataset: DatasetInfoProto
    ): MlpResponse {
        val data =
            if (fitDataExample is Payload) train as F
            else JSON.parse(train.data, fitDataExample.javaClass)
        val config = if (config != null) JSON.parse(config.data, fitConfigExample.javaClass) else null

        fit(data, config, modelDir, previousModelDir, targetServiceInfo, dataset)

        return Payload(
            dataType = "text/plain",
            data = "ok"
        )
    }

    abstract fun fit(data: F, config: FC?, modelDir: String, previousModelDir: String?, targetServiceInfo: ServiceInfoProto,
                     dataset: DatasetInfoProto)

    final override fun predict(req: Payload, config: Payload?): MlpResponse {
        val request = JSON.parse(req.data, predictRequestExample.javaClass)

        val res = this.predict(request)

        return Payload(JSON.stringify(res))
    }

    abstract fun predict(request: P): R

}


abstract class MlpFitServiceBase<F: Any, FC: Any>(
    fitDataExample: F,
    fitConfigExample: FC,
): MlpServiceBase<F, FC, String, String>(fitDataExample, fitConfigExample, "", "") {

    final override fun predict(request: String): String {
        throw RuntimeException("Not implemented yet")
    }
}

abstract class MlpPredictServiceBase<P: Any, R: Any>(
    predictRequestExample: P,
    predictResponseExample: R,
): MlpServiceBase<String, String, P, R>("", "", predictRequestExample, predictResponseExample) {

    final override fun fit(data: String, config: String?, modelDir: String, previousModelDir: String?, targetServiceInfo: ServiceInfoProto,
                     dataset: DatasetInfoProto) {
        throw RuntimeException("Predict service doesn't support fit method")
    }
}

class MlpRestClient(
        restUrl: String = System.getenv("MLP_REST_URL"),
        clientToken: String = System.getenv("MLP_CLIENT_TOKEN")
) {
    val ACCOUNT_ID = System.getenv("MLP_ACCOUNT_ID")
    val log = LoggerFactory.getLogger("MlpRestClient")

    val apiClient: ApiClient

    val processApi: ProcessEndpointApi
    val modelApi: ModelEndpointApi
    val jobApi: JobEndpointApi
    val datasetApi: DatasetEndpointApi

    init {
        apiClient = ApiClient().apply {
            basePath = restUrl
            addDefaultHeader("MLP-API-KEY", clientToken)
        }

        processApi = ProcessEndpointApi(apiClient)
        modelApi = ModelEndpointApi(apiClient)
        jobApi = JobEndpointApi(apiClient)
        datasetApi = DatasetEndpointApi(apiClient)
    }
}