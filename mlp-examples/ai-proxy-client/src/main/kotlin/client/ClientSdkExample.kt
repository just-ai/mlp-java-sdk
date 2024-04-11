package client

import com.mlp.api.datatypes.chatgpt.ChatCompletionRequest
import com.mlp.api.datatypes.chatgpt.ChatMessage
import com.mlp.api.datatypes.chatgpt.ChatRole
import com.mlp.sdk.MlpClientSDK
import com.mlp.sdk.MlpExecutionContext.Companion.systemContext
import com.mlp.sdk.Payload
import com.mlp.sdk.datatypes.aiproxy.AIProxyChatCompletionRequest
import com.mlp.sdk.datatypes.aiproxy.AiProxyRequest
import com.mlp.sdk.datatypes.aiproxy.AiProxyResponse
import com.mlp.sdk.utils.JSON
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val clientSDK = MlpClientSDK(context = systemContext)

    val request = AiProxyRequest(
        chat = AIProxyChatCompletionRequest(
            model = "gpt-3.5-turbo",
            messages = listOf(ChatMessage(ChatRole.user, "Привет")))
        )

    val res = clientSDK.predict("just-ai", "AI-Proxy", JSON.stringify(request))
    val response = JSON.parse<AiProxyResponse>(res)

    println(response)

    clientSDK.shutdown()
}
