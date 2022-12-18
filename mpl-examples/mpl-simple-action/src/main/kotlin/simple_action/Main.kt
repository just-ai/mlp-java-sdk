package simple_action

import com.fasterxml.jackson.databind.ObjectMapper
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
    override fun predict(req: Payload): MplResponse {
        val objectMapper = ObjectMapper()

        val request = objectMapper.readValue(req.data, SimpleTestActionRequest::class.java)
        return when(request.action) {
            "hello" -> Payload("text/plain", "\"response from action\"")
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
    val action: String
)