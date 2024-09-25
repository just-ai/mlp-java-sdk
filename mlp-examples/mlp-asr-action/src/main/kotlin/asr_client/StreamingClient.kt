package asr_client

import com.google.protobuf.kotlin.toByteStringUtf8
import com.mlp.gate.AsrRequestProto
import com.mlp.sdk.MlpClientSDK
import com.mlp.sdk.MlpExecutionContext
import com.mlp.sdk.Payload
import com.mlp.sdk.datatypes.asr.common.AsrRequest
import com.mlp.sdk.datatypes.asr.common.RecognitionConfig
import com.mlp.sdk.utils.JSON.asJson
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import java.util.Base64

fun main() = runBlocking {
    val client = MlpClientSDK(context = MlpExecutionContext.systemContext)
    val lang = listOf("ru-RU", "en-EN")
    val flow = flow {
        for (i in 1..10) {
            val p = AsrRequest("$i".toByteStringUtf8(), RecognitionConfig(languageCode = lang.random()))
            emit(p)
        }
    }

    val responseFlow = client.asrPredict("1000001", "501", flow)
    responseFlow.collect {
        println(it)
    }

    val flowProto = flow {
        val config = AsrRequestProto.newBuilder()
            .setConfig(
                com.mlp.gate.RecognitionConfig.newBuilder()
                    .setLanguageCode(lang.random())
            ).build()
        emit(config)
        for (i in 1..10) {
            val asrRequestProto = AsrRequestProto.newBuilder()
                .setAudioContent("$i".toByteStringUtf8())
                .build()
            emit(asrRequestProto)
        }
    }

    val responseProto = client.asrPredictProto("1000001", "501", flowProto)
    responseProto.collect {
        println(it)
    }

    val audioBase64 = Base64.getEncoder().encodeToString("Hello".toByteArray())

    val longRecognition = Payload(data = com.mlp.sdk.datatypes.asr.AsrRequest(audioBase64).asJson)
    val response = client.predict("1000001", "501", longRecognition)
    println(response.data)
}
