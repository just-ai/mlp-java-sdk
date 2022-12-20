package simple_action

import com.fasterxml.jackson.databind.ObjectMapper
import com.mpl.gate.ActionDescriptorProto
import com.mpl.gate.MethodDescriptorProto
import com.mpl.gate.ParamDescriptorProto
import com.mpl.sdk.MplAction
import com.mpl.sdk.MplActionSDK
import com.mpl.sdk.MplResponse
import com.mpl.sdk.Payload

fun main() {
    val action = SimpleTestAction()
    val actionSDK = MplActionSDK(action)

    actionSDK.start()
    actionSDK.blockUntilShutdown()
}

class SimpleTestAction: MplAction() {

    private val objectMapper = ObjectMapper()

    override fun getDescriptor(): ActionDescriptorProto {
        return ActionDescriptorProto.newBuilder()
            .setName("simple model")
            .setFittable(false)
            .putAllMethods(
                mapOf(
                    "toUpperCase" to MethodDescriptorProto.newBuilder()
                        .putInput("text", ParamDescriptorProto.newBuilder().setType("String").build())
                        .build(),
                    "toLowerCase" to MethodDescriptorProto.newBuilder()
                        .putInput("text", ParamDescriptorProto.newBuilder().setType("String").build())
                        .build(),
                )
            ).build()
    }

    override fun predict(req: Payload): MplResponse {
        val request = objectMapper.readValue(req.data, SimpleTestActionRequest::class.java)
        return when(request.action) {
            "hello" -> Payload("text/plain", "\"Hello, ${request.name}\"")
            else -> throw RuntimeException("actionUnknownException")
        }
    }

    override fun ext(methodName: String, params: Map<String, Payload>): MplResponse {
        val data = params["text"]?.data ?: throw RuntimeException("data must be not null")
        val extendedData = when (methodName) {
            "toUpperCase" -> data.uppercase()
            "toLowerCase" -> data.lowercase()
            else -> data
        }
        return Payload("text/plain", extendedData)
    }
}

data class SimpleTestActionRequest(
    val action: String,
    val name: String
)