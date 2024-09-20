package asr_client

import com.google.protobuf.kotlin.toByteStringUtf8
import com.mlp.sdk.MlpClientSDK
import com.mlp.sdk.MlpExecutionContext
import com.mlp.sdk.Payload
import com.mlp.sdk.PayloadWithConfig
import com.mlp.sdk.datatypes.asr.common.AsrRequest
import com.mlp.sdk.datatypes.asr.common.RecognitionConfig
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking

// MLP_CLIENT_TOKEN=1000001.3.S1TU7xwTlZgWA7U3q9vxuDDPmtzH6jYIAkfS0FpS;MLP_GRPC_HOST=gate.caila-3304-asr-streaming.caila-ci-feature.lo.test-ai.net:443
fun main() = runBlocking {
    val client = MlpClientSDK(context = MlpExecutionContext.systemContext)
    val lang = listOf("ru-RU", "en-EN")
    val flow = flow {
        for (i in 1..10) {
            val p = AsrRequest("$i".toByteStringUtf8(), RecognitionConfig(languageCode = lang.random()))
            emit(p)
        }
    }

    val response = client.asrPredict("1000001", "501", flow)
    response.collect {
        println(it)
    }
}