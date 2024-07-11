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
import com.mlp.sdk.MlpExecutionContext.Companion.systemContext
import com.mlp.sdk.Payload
import com.mlp.sdk.datatypes.aiproxy.AiProxyRequest
import com.mlp.sdk.datatypes.aiproxy.AiProxyResponse
import com.mlp.sdk.utils.JSON
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val clientSDK = MlpClientSDK(context = systemContext)

    val messages = mutableListOf<ChatMessage>(TextChatMessage(ChatRole.user, "Какая погода в Санкт-Петербурге?"))

    val request = AiProxyRequest(
        chat = ChatCompletionRequest(
            model = "gpt-3.5-turbo",
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
            toolChoice = NamedToolChoice( // vllm поддерживает только именованные функции
                ToolType.function,
                NamedToolChoiceFunction("getCurrentWeather")
            )
        )
    )
    val flow = clientSDK.predictStream(
        "just-ai",
        "openai-proxy",
        Payload(JSON.stringify(request))
    )

    flow.collect {
        val json = it.predict.data.json
        val aiProxyResponse = JSON.parse<AiProxyResponse>(json)
        val result = JSON.parse<ChatCompletionResult>(JSON.anyToObject(checkNotNull(aiProxyResponse.chat)))
        println("Model's response:")
        println(result)

        val responseMessage = checkNotNull(result.choices.first().message)

        messages.add(responseMessage)

        if (responseMessage.toolCalls?.isNotEmpty() == true) {
            for (toolCall in responseMessage.toolCalls!!) {
                if (toolCall.function.name == "getCurrentWeather") {
                    val functionArgs = JSON.parseObject(toolCall.function.arguments)
                    println("Function arguments: $functionArgs")
                    val weatherResponse = getCurrentWeather(
                        location = checkNotNull(functionArgs.get("location")?.asText()),
                        unit = functionArgs.get("unit")?.asText() ?: "celsius"
                    )
                    messages.add(
                        TextChatMessage(
                            role = ChatRole.tool,
                            name = "getCurrentWeather",
                            // toolCallId = toolCall.id, // vllm error
                            content = JSON.stringify(weatherResponse)
                        )
                    )
                }
            }
        } else {
            println("No tool calls were made by the model.")
        }

        val newRequest = AiProxyRequest(
            chat = ChatCompletionRequest(
                model = "gpt-3.5-turbo",
                messages = messages
            )
        )
        println("New request:")
        println(newRequest)

        val finalResponseFlow = clientSDK.predictStream(
            "just-ai",
            "openai-proxy",
            Payload(JSON.stringify(newRequest))
        )
        println("Final Response:")
        finalResponseFlow.collect {
            val json = it.predict.data.json
            val res = JSON.parse<AiProxyResponse>(json)
            println(res)
        }
    }

    clientSDK.shutdown()
}
