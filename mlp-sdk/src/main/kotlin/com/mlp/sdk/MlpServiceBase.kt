package com.mlp.sdk

import com.fasterxml.jackson.databind.JsonMappingException
import com.mlp.api.ApiClient
import com.mlp.api.TypeInfo
import com.mlp.api.client.DatasetEndpointApi
import com.mlp.api.client.JobEndpointApi
import com.mlp.api.client.ModelEndpointApi
import com.mlp.api.client.ProcessEndpointApi
import com.mlp.gate.DatasetInfoProto
import com.mlp.gate.MethodDescriptorProto
import com.mlp.gate.ParamDescriptorProto
import com.mlp.gate.PartialPredictResponseProto
import com.mlp.gate.PayloadProto
import com.mlp.gate.ServiceDescriptorProto
import com.mlp.gate.ServiceInfoProto
import com.mlp.gate.ServiceToGateProto
import com.mlp.sdk.MlpExecutionContext.Companion.systemContext
import com.mlp.sdk.datatypes.asr.common.AsrRequest
import com.mlp.sdk.datatypes.asr.common.RecognitionConfig
import com.mlp.sdk.utils.JSON
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.transform
import org.slf4j.MDC

abstract class MlpServiceBase<F : Any, FC : Any, P : Any, C : Any, R : Any>(
    val fitDataExample: F,
    val fitConfigExample: FC,
    val predictRequestExample: P,
    val predictConfigExample: C,
    val predictResponseExample: R,
) : MlpService() {

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
            else JSON.parseOrThrowBadRequestMlpException(train.data, fitDataExample.javaClass)
        val config0 = if (config != null)
            JSON.parseOrThrowBadRequestMlpException(config.data, fitConfigExample.javaClass)
        else
            null

        fit(data, config0, modelDir, previousModelDir, targetServiceInfo, dataset)

        return Payload(
            dataType = "text/plain",
            data = "ok"
        )
    }

    abstract suspend fun fit(
        data: F,
        config: FC?,
        modelDir: String,
        previousModelDir: String?,
        targetServiceInfo: ServiceInfoProto,
        dataset: DatasetInfoProto
    )

    override suspend fun predict(req: Payload, config: Payload?): MlpResponse {
        // парсим request и config.
        val request =
            JSON.parseOrThrowBadRequestMlpException(req.data, predictRequestExample.javaClass) // TODO: handle datatype

        val conf = if (config != null && predictConfigExample !is Unit) {
            JSON.parseOrThrowBadRequestMlpException(config.data, predictConfigExample.javaClass)
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

    override suspend fun streamPredictRaw(stream: Flow<PayloadWithConfig>): Flow<StreamPayloadInterface> {
        val pToCFlow = stream.map { req ->
            val request = when (req.payload.dataType) {
                AsrRequest.ASR_DATATYPE -> {
                    if (predictRequestExample !is AsrRequest) {
                        throw MlpException(
                            MlpError(
                                CommonErrorCode.BAD_REQUEST,
                                "message" to "Unsupported datatype ${AsrRequest.ASR_DATATYPE}."
                            )
                        )
                    }

                    val audioContent = (req.payload as? ProtobufPayload)?.data
                        ?: throw MlpException(
                            MlpError(
                                CommonErrorCode.BAD_REQUEST,
                                "message" to "Unsupported payload body for asr datatype. Use protobuf body instead of json."
                            )
                        )
                    val config = req.config?.let { JSON.parseOrThrowBadRequestMlpException(it.data, RecognitionConfig::class.java) }
                    @Suppress("UNCHECKED_CAST")
                    AsrRequest(audioContent, config) as P
                }

                else -> JSON.parseOrThrowBadRequestMlpException(req.payload.stringData(), predictRequestExample.javaClass)
            }

            val config = req.config

            val conf = if (config != null && predictConfigExample !is Unit) {
                JSON.parseOrThrowBadRequestMlpException(config.data, predictConfigExample.javaClass)
            } else null
            request to conf
        }

        var lastResponse: R? = null
        return this.streamPredict(pToCFlow).transform {
            lastResponse?.let { lr ->
                emit(StreamPayloadInterface(Payload(data = JSON.stringify(lr), dataType = TypeInfo.canonicalName(lr.javaClass)), false))
            }
            lastResponse = it
        }.onCompletion {
            val dataType = lastResponse?.let { lr -> TypeInfo.canonicalName(lr.javaClass) } ?: "json"
            emit(StreamPayloadInterface(Payload(data = JSON.stringify(lastResponse), dataType = dataType), true))
        }
    }

    fun createGenerator(): ResultGenerator<R> {
        return com.mlp.sdk.createGenerator<R>(sdk)
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

    open suspend fun streamPredict(stream: Flow<Pair<P, C?>>): Flow<R?> {
        return stream.map { predict(it.first, it.second) }
    }

    private fun <T> JSON.parseOrThrowBadRequestMlpException(json: String, clazz: Class<T>): T = try {
        parse(json, clazz)
    } catch (e: JsonMappingException) {
        logger.error("Failed to parse json into {}", clazz, e)
        throw MlpException(MlpError(CommonErrorCode.BAD_REQUEST, e))
    }

}

fun <R : Any> createGenerator(sdk: MlpServiceSDK): MlpServiceBase.ResultGenerator<R> {
    val requestId = MDC.get("gateRequestId").toLong()
    val connectorId = MDC.get("connectorId").toLong()

    return MlpServiceBase.ResultGenerator { resultAndFinish ->
        val payload = when (resultAndFinish.result) {
            is PayloadProto -> resultAndFinish.result
            is Payload -> PayloadProto.newBuilder()
                .setJson(resultAndFinish.result.data)
                .setDataType(resultAndFinish.result.dataType).build()

            is RawPayload -> PayloadProto.newBuilder()
                .setJson(resultAndFinish.result.data)
                .setDataType(resultAndFinish.result.dataType).build()

            else -> PayloadProto.newBuilder()
                .setJson(JSON.stringify(resultAndFinish.result))
                .setDataType(TypeInfo.canonicalName(resultAndFinish.result.javaClass)).build()
        }

        val builder = ServiceToGateProto.newBuilder()
            .setRequestId(requestId)
            .setPartialPredict(
                PartialPredictResponseProto.newBuilder()
                    .setFinish(resultAndFinish.last)
                    .setData(
                        payload
                    )
            )

        val billingUnits = resultAndFinish.price ?: BillingUnitsThreadLocal.getUnits()
        if (billingUnits != null) {
            builder.putHeaders("Z-custom-billing", billingUnits.toString())
        }

        sdk.send(connectorId, builder.build())
    }
}


abstract class MlpFitServiceBase<F : Any, FC : Any>(
    fitDataExample: F,
    fitConfigExample: FC,
) : MlpServiceBase<F, FC, String, Unit, String>(fitDataExample, fitConfigExample, "", Unit, "") {

    final override suspend fun predict(request: String, config: Unit?): String? {
        throw RuntimeException("Not implemented yet")
    }

    final override suspend fun streamPredict(stream: Flow<Pair<String, Unit?>>): Flow<String?> {
        throw RuntimeException("Not implemented yet")
    }
}

abstract class MlpPredictServiceBase<P : Any, R : Any>(
    predictRequestExample: P,
    predictResponseExample: R,
) : MlpServiceBase<String, String, P, Unit, R>("", "", predictRequestExample, Unit, predictResponseExample) {

    final override suspend fun fit(
        data: String,
        config: String?,
        modelDir: String,
        previousModelDir: String?,
        targetServiceInfo: ServiceInfoProto,
        dataset: DatasetInfoProto
    ) {
        throw RuntimeException("Predict service doesn't support fit method")
    }

    override suspend fun predict(request: P, config: Unit?): R? {
        return predict(request)
    }

    abstract fun predict(req: P): R

}

abstract class MlpPredictWithConfigServiceBase<P : Any, C : Any, R : Any>(
    predictRequestExample: P,
    predictConfigExample: C,
    predictResponseExample: R,
): MlpServiceBase<String, String, P, C, R>("", "", predictRequestExample, predictConfigExample, predictResponseExample) {

    final override suspend fun fit(
        data: String,
        config: String?,
        modelDir: String,
        previousModelDir: String?,
        targetServiceInfo: ServiceInfoProto,
        dataset: DatasetInfoProto
    ) {
        throw RuntimeException("Predict service doesn't support fit method")
    }
}

class MlpRestClient(
    restUrl: String? = null,
    clientToken: String? = null,
    billingToken: String? = null,
    override val context: MlpExecutionContext = systemContext
) : WithExecutionContext {

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
