package simple_action

import com.mlp.sdk.*

data class SimpleTestActionRequest(
    val action: String,
    val name: String
)
class SimpleTestAction : MlpPredictServiceBase<SimpleTestActionRequest, String>(REQUEST_EXAMPLE, RESPONSE_EXAMPLE) {

    override fun predict(req: SimpleTestActionRequest): String {
        return when (req.action) {
            "hello" -> "Hello ${req.name}!"
            else -> throw MlpException("actionUnknownException")
        }
    }

    companion object {
        val REQUEST_EXAMPLE = SimpleTestActionRequest("hello", "World")
        val RESPONSE_EXAMPLE = "Hello World!"
    }

}

fun main() {
    val action = SimpleTestAction()
    val actionSDK = MlpServiceSDK(action)

    actionSDK.start()
    actionSDK.blockUntilShutdown()
}
