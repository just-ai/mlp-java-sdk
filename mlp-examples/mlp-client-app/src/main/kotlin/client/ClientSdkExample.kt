package client

import com.mlp.sdk.MlpClientSDK
import com.mlp.sdk.MlpExecutionContext.Companion.systemContext
import com.mlp.sdk.utils.JSON
import kotlinx.coroutines.runBlocking

/*
 * Предварительно необходимо добавить в env MLP_CLIENT_TOKEN и MLP_REST_URL
 * 1. Получаем  контекст для запуска в JVM процессе инстанса mlp-sdk
 * 2. Создаем JSON
 * 3. Отправляем его в сервис test-action
 * 4. Полученный ответ выводим в консоль
 */
fun main() = runBlocking {
    val clientSDK = MlpClientSDK(context = systemContext)

    val payload = JSON.stringify("hello")
    val res = clientSDK.predict("just-ai", "test-action", payload)

    println()
    println(res)
    println()

    clientSDK.shutdown()
}
