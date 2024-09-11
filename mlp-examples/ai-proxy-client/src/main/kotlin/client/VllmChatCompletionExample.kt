package client

import com.mlp.api.datatypes.chatgpt.ChatCompletionRequest
import com.mlp.api.datatypes.chatgpt.ChatCompletionResult
import com.mlp.api.datatypes.chatgpt.ChatMessage
import com.mlp.api.datatypes.chatgpt.ChatRole
import com.mlp.api.datatypes.chatgpt.Function
import com.mlp.api.datatypes.chatgpt.NamedToolChoice
import com.mlp.api.datatypes.chatgpt.NamedToolChoiceFunction
import com.mlp.api.datatypes.chatgpt.TextChatMessage
import com.mlp.api.datatypes.chatgpt.Tool
import com.mlp.api.datatypes.chatgpt.ToolType
import com.mlp.sdk.MlpClientSDK
import com.mlp.sdk.MlpExecutionContext
import com.mlp.sdk.utils.JSON
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // 1
    val clientSDK = MlpClientSDK(context = MlpExecutionContext.systemContext)

    // 2
    val messages = mutableListOf<ChatMessage>(TextChatMessage(ChatRole.user, "Какая погода в Санкт-Петербурге?"))

    val request = ChatCompletionRequest(
        model = "Qwen/Qwen2-7B-Instruct",
        messages = messages,
        tools = listOf(
            Tool(
                ToolType.function,
                Function(
                    "getCurrentWeather",
                    "Получить текущую погоду в конкретном месте",
                    getCurrentWeatherParameters
                )
            )
        ),
        toolChoice = NamedToolChoice( // vllm поддерживает только именованные функции. wait for https://github.com/vllm-project/vllm/pull/3237
            ToolType.function,
            NamedToolChoiceFunction("getCurrentWeather")
        )
    )
    // 3
    val response = clientSDK.predict(
        "just-ai",
        "vllm-qwen2-7b",
        JSON.stringify(request)
    )

    // 4
    val result = JSON.parse<ChatCompletionResult>(response)
    println("Model's response:")
    println(result)

    val responseMessage = checkNotNull(result.choices.first().message)

    messages.add(responseMessage)

    // 5
    if (responseMessage.toolCalls?.isNotEmpty() == true) {
        for (toolCall in responseMessage.toolCalls!!) {
            if (toolCall.function.name == "getCurrentWeather") {
                // 6
                val functionArgs = JSON.parseObject(toolCall.function.arguments)
                println("Function arguments: $functionArgs")
                val weatherResponse = getCurrentWeather(
                    location = checkNotNull(functionArgs.get("location")?.asText()),
                    unit = functionArgs.get("unit")?.asText() ?: "celsius"
                )
                // 7
                messages.add(
                    TextChatMessage(
                        role = ChatRole.tool,
                        toolCallId = toolCall.id,
                        content = JSON.stringify(weatherResponse)
                    )
                )
            }
        }
    } else {
        println("No tool calls were made by the model.")
    }

    // 7
    val newRequest = ChatCompletionRequest(
        model = "Qwen/Qwen2-7B-Instruct",
        messages = messages
    )
    println("New request:")
    println(newRequest)

    // 8
    val finalResponse = clientSDK.predict(
        "just-ai",
        "vllm-qwen2-7b",
        JSON.stringify(newRequest)
    )

    println("Final Response:")
    println(JSON.parse<ChatCompletionResult>(finalResponse))

    clientSDK.shutdown()
}
