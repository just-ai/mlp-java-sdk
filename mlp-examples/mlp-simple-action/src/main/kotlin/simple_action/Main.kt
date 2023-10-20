package simple_action

import com.mlp.sdk.MlpException
import com.mlp.sdk.MlpExecutionContext
import com.mlp.sdk.MlpExecutionContext.Companion.systemContext
import com.mlp.sdk.MlpPredictServiceBase
import com.mlp.sdk.MlpServiceSDK
import com.mlp.sdk.WithExecutionContext

data class SimpleTestActionRequest(
    val action: String,
    val name: String
)

class SimpleTestAction(
    override val context: MlpExecutionContext
) : MlpPredictServiceBase<SimpleTestActionRequest, String>(REQUEST_EXAMPLE, RESPONSE_EXAMPLE) {

    override fun predict(req: SimpleTestActionRequest): String {
        return when (req.action) {
            "hello" -> "Hello ${req.name}!"
            "envs" -> "Envs: ${context.environment.envsOverride}"
            else -> throw MlpException("actionUnknownException")
        }
    }

    companion object {
        val REQUEST_EXAMPLE = SimpleTestActionRequest("hello", "World")
        val RESPONSE_EXAMPLE = "Hello World!"
    }
}

fun main() {
    val actionSDK = MlpServiceSDK({ SimpleTestAction(systemContext) })

    actionSDK.start()
    actionSDK.blockUntilShutdown()
}
