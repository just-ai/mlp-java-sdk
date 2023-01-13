package simple_action

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.mlp.gate.ServiceDescriptorProto
import com.mlp.sdk.MlpResponse
import com.mlp.sdk.MlpService
import com.mlp.sdk.MlpServiceSDK
import com.mlp.sdk.Payload

fun main() {
    val action = SimpleTestAction()
    val actionSDK = MlpServiceSDK(action)

    actionSDK.start()
    actionSDK.blockUntilShutdown()
}

class SimpleTestAction : MlpService() {

    private val objectMapper = ObjectMapper().apply {
        registerModule(KotlinModule())
    }

    override fun getDescriptor(): ServiceDescriptorProto {
        return ServiceDescriptorProto.newBuilder()
            .setName("simple model")
            .setFittable(false)
            .build()
    }

    override fun predict(req: Payload): MlpResponse {
        val request = objectMapper.readValue(req.data, SimpleTestActionRequest::class.java)
        return when (request.action) {
            "hello" -> Payload("text/plain", "\"Hello, ${request.name}\"")
            else -> throw RuntimeException("actionUnknownException")
        }
    }
}

data class SimpleTestActionRequest(
    val action: String,
    val name: String
)