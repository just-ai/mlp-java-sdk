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
import com.mlp.api.datatypes.chatgpt.ToolChoiceEnum
import com.mlp.api.datatypes.chatgpt.ToolType
import com.mlp.sdk.MlpClientSDK
import com.mlp.sdk.MlpExecutionContext.Companion.systemContext
import com.mlp.sdk.datatypes.aiproxy.AiProxyRequest
import com.mlp.sdk.datatypes.aiproxy.AiProxyResponse
import com.mlp.sdk.utils.JSON
import kotlinx.coroutines.runBlocking

/*
 * Предварительно необходимо добавить в env MLP_CLIENT_TOKEN и MLP_REST_URL
 * 1. Получаем контекст для запуска в JVM процессе инстанса mlp-sdk
 * 2. Создаем JSON
 * 3. Отправляем его в сервис AI-Proxy
 * 4. Получаем ответ
 * 5. Если в ответе приходит намерение сделать вызов внешней функции
 * 6. Tо делаем вызов
 * 7. И формируем новый запрос
 * 8. Отправляем новый запрос
 */
fun main() = runBlocking {
    // 1
    val clientSDK = MlpClientSDK(context = systemContext)

    // 2
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
            toolChoice = ToolChoiceEnum.auto
        )
    )
    // 3
    val response = clientSDK.predict(
        "just-ai",
        "openai-proxy",
        JSON.stringify(request)
    )

    // 4
    val aiProxyResponse = JSON.parse<AiProxyResponse>(response)
    val result = JSON.parse<ChatCompletionResult>(JSON.anyToObject(checkNotNull(aiProxyResponse.chat)))
    println("Model's response:")
    println(result)

    val responseMessage = checkNotNull(result.choices.first().message)

    messages.add(TextChatMessage(responseMessage.role, "", responseMessage.toolCallId, responseMessage.name, responseMessage.toolCalls, ))

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
    val newRequest = AiProxyRequest(
        chat = ChatCompletionRequest(
            model = "gpt-3.5-turbo",
            messages = messages
        )
    )
    println("New request:")
    println(newRequest)

    // 8
    val finalResponse = clientSDK.predict(
        "just-ai",
        "openai-proxy",
        JSON.stringify(newRequest)
    )

    val finalAiProxyResponse = JSON.parse<AiProxyResponse>(finalResponse)
    val finalChatCompletionResult = JSON.parse<ChatCompletionResult>(JSON.anyToObject(checkNotNull(finalAiProxyResponse.chat)))

    println("Final Response:")
    println(finalChatCompletionResult)

    clientSDK.shutdown()
}

internal val getCurrentWeatherParameters = JSON.mapper.createObjectNode().apply {
    put("type", "object")
    putObject("properties").apply {
        putObject("location")
            .put("type", "string")
            .put("description", "Город, например, Москва")

        putObject("unit")
            .put("type", "string")
            .putArray("enum").add("celsius").add("fahrenheit")
    }
    putArray("required").add("location")
}

private val weatherMap = mapOf(
    "saint petersburg" to 284.15,
    "санкт-петербург" to 284.15
)

fun getCurrentWeather(location: String, unit: String): WeatherResponse {
    val temperatureInKelvin = weatherMap[location.lowercase().trim()]
        ?: return WeatherResponse(location, "unknown", unit)
    val temperatureString = when (unit.lowercase()) {
        "fahrenheit" -> temperatureInKelvin.toFahrenheitString()
        else -> temperatureInKelvin.toCelsiusString()
    }
    return WeatherResponse(location, temperatureString, unit)
}

fun Double.toFahrenheitString() = ((this - 273.15) * 9 / 5 + 32).toString()

fun Double.toCelsiusString() = (this - 273.15).toString()

data class WeatherResponse(val location: String, val temperature: String, val unit: String)
