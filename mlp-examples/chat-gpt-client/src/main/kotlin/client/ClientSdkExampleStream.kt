package client

import com.mlp.sdk.MlpClientSDK
import com.mlp.sdk.MlpExecutionContext.Companion.systemContext
import com.mlp.sdk.Payload
import com.mlp.sdk.datatypes.aiproxy.AiProxyRequest
import com.mlp.sdk.datatypes.aiproxy.AiProxyResponse
import com.mlp.sdk.datatypes.chatgpt.ChatCompletionRequest
import com.mlp.sdk.datatypes.chatgpt.ChatCompletionResult
import com.mlp.sdk.datatypes.chatgpt.ChatCompletionRole
import com.mlp.sdk.datatypes.chatgpt.ChatMessage
import com.mlp.sdk.utils.JSON
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val clientSDK = MlpClientSDK(context = systemContext)

    val request = ChatCompletionRequest(
            messages = listOf(ChatMessage(ChatCompletionRole.user, "Привет"))
        )

    val flow = clientSDK.predictStream("just-ai", "llm-saiga-chatcompletion", Payload(JSON.stringify(request)))
    flow.collect {
        val res = JSON.parse<ChatCompletionResult>(it.partialPredict.data.json)
        println(res)
    }

    clientSDK.shutdown()
}
