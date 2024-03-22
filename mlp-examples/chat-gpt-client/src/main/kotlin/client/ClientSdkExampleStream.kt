package client

import com.mlp.api.datatypes.chatgpt.ChatCompletionRequest
import com.mlp.api.datatypes.chatgpt.ChatCompletionResult
import com.mlp.api.datatypes.chatgpt.ChatMessage
import com.mlp.api.datatypes.chatgpt.ChatRole
import com.mlp.sdk.MlpClientSDK
import com.mlp.sdk.MlpExecutionContext.Companion.systemContext
import com.mlp.sdk.Payload
import com.mlp.sdk.utils.JSON
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val clientSDK = MlpClientSDK(context = systemContext)

    val request = ChatCompletionRequest(
            stream = true,
            messages = listOf(ChatMessage(ChatRole.user, "Привет"))
        )

    val flow = clientSDK.predictStream("1000002", "test-action", Payload(JSON.stringify(request)))
    flow.collect {
        val json = it.partialPredict.data.json
        val res = JSON.parse<ChatCompletionResult>(json)
        println(res)
    }

    clientSDK.shutdown()
}
