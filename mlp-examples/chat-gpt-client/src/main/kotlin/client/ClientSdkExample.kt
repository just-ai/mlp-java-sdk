package client

import com.mlp.sdk.MlpClientSDK
import com.mlp.sdk.datatypes.aiproxy.AiProxyRequest
import com.mlp.sdk.datatypes.aiproxy.AiProxyResponse
import com.mlp.sdk.datatypes.chatgpt.*
import com.mlp.sdk.utils.JSON
import kotlinx.coroutines.runBlocking


fun main() = runBlocking {
    val clientSDK = MlpClientSDK()

    val request = AiProxyRequest(
        chat = ChatCompletionRequest(
            model = "gpt-3.5-turbo",
            messages = listOf(ChatMessage(ChatCompletionRole.user, "Привет")))
        )

    val res = clientSDK.predict("just-ai", "AI-Proxy", JSON.stringify(request))
    val response = JSON.parse<AiProxyResponse>(res)

    println(response)

    clientSDK.shutdown()
}