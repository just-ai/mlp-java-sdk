package client

import com.mlp.sdk.MlpClientSDK
import com.mlp.sdk.datatypes.aiproxy.ChatCompletionResult
import com.mlp.sdk.datatypes.aiproxy.ChatMessage
import com.mlp.sdk.datatypes.aiproxy.ChatRequest
import com.mlp.sdk.utils.JSON
import kotlinx.coroutines.runBlocking


fun main() = runBlocking {
    val clientSDK = MlpClientSDK()

    val request = ChatRequest(listOf(ChatMessage("user", "Привет")))

    val res = clientSDK.predict("just-ai", "AI-Proxy", JSON.stringify(request))
    val response = JSON.parse<ChatCompletionResult>(res)

    println(response)

    clientSDK.shutdown()
}