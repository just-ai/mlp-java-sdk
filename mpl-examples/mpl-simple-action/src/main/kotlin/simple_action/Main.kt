package simple_action

import com.fasterxml.jackson.databind.ObjectMapper
import com.mpl.gate.ActionDescriptorProto
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

class SimpleTestAction : MplAction() {

    private val objectMapper = ObjectMapper()

    override fun getDescriptor(): ActionDescriptorProto {
        return ActionDescriptorProto.newBuilder()
            .setName("simple model")
            .setFittable(false)
            .build()
    }

    override fun predict(req: Payload): MplResponse {
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