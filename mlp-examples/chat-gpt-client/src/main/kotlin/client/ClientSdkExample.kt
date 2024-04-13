package client

import com.mlp.sdk.MlpClientSDK
import com.mlp.sdk.MlpExecutionContext.Companion.systemContext
import com.mlp.sdk.datatypes.aiproxy.AiProxyRequest
import com.mlp.sdk.datatypes.aiproxy.AiProxyResponse
import com.mlp.sdk.datatypes.chatgpt.ChatCompletionRequest
import com.mlp.sdk.datatypes.chatgpt.ChatCompletionRole
import com.mlp.sdk.datatypes.chatgpt.ChatMessage
import com.mlp.sdk.utils.JSON
import kotlinx.coroutines.runBlocking

/*
 * Предварительно необходимо добавить в env MLP_CLIENT_TOKEN и MLP_REST_URL
 * 1. Получаем  контекст для запуска в JVM процессе инстанса mlp-sdk
 * 2. Создаем JSON
 * 3. Отправляем его в сервис AI-Proxy
 * 4. Полученный ответ выводим в консоль
 */
fun main() = runBlocking {
    val clientSDK = MlpClientSDK(context = systemContext)

    val request = AiProxyRequest(
        chat = ChatCompletionRequest(
            model = "gpt-3.5-turbo",
            messages = listOf(ChatMessage(ChatCompletionRole.user, "Привет")))
        )
    val response = clientSDK.predict("just-ai", "AI-Proxy", JSON.stringify(request))

    val result = JSON.parse<AiProxyResponse>(response)
    println(result)

    clientSDK.shutdown()
}
