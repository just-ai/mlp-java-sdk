package client

import com.mlp.sdk.MlpClientSDK
import com.mlp.sdk.MlpExecutionContext.Companion.systemContext
import com.mlp.sdk.Payload
import com.mlp.sdk.datatypes.aiproxy.AiProxyRequest
import com.mlp.sdk.datatypes.aiproxy.AiProxyResponse
import com.mlp.sdk.datatypes.chatgpt.*
import com.mlp.sdk.utils.JSON
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val clientSDK = MlpClientSDK(context = systemContext)

    val request = ChatCompletionSimpleRequest(
            stream = true,
            messages = listOf(ChatMessage(ChatCompletionRole.user, "Привет"))
        )

    val flow = clientSDK.predictStream("1000002", "test-action", Payload(JSON.stringify(request)))
    flow.collect {
        val json = it.partialPredict.data.json
        val res = JSON.parse<ChatCompletionResult>(json)
        println(res)
    }

    clientSDK.shutdown()
}
