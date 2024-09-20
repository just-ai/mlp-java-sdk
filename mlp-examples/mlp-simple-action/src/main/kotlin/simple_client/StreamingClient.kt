package simple_client

import com.mlp.sdk.MlpClientSDK
import com.mlp.sdk.MlpExecutionContext
import com.mlp.sdk.Payload
import com.mlp.sdk.PayloadWithConfig
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking

// MLP_CLIENT_TOKEN=1000001.3.S1TU7xwTlZgWA7U3q9vxuDDPmtzH6jYIAkfS0FpS;MLP_GRPC_HOST=gate.caila-3304-asr-streaming.caila-ci-feature.lo.test-ai.net:443
fun main() = runBlocking {
    val client = MlpClientSDK(context = MlpExecutionContext.systemContext)
    val flow = flow {
        for (i in 1..10) {
            val p = PayloadWithConfig(Payload(data = """{"action": "hello", "name": "Viktor - $i"}"""), null)
            emit(p)
        }
    }

    val response = client.streamPredict("1000001", "23", flow)
    response.collect {
        println(it)
    }
}