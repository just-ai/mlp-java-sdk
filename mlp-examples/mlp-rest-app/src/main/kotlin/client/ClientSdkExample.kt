package client

import com.mlp.sdk.MlpExecutionContext.Companion.systemContext
import com.mlp.sdk.MlpRestClient
import com.mlp.sdk.utils.JSON
/*
 Дата классы для запроса и ответа от сервиса platform-vectorizer-ru-test
 */
data class VectorizerRequest(val texts: List<String>)

data class VectorizerEmbeddings(val vector: List<Double>)
data class VectorizerResponse(val embedded_texts: List<VectorizerEmbeddings>)

/*
 * 1. Создаем restClient через который мы будем посылать запрос в platform-vectorizer-ru-test
 * 2. Создаем request, который будет отправлен в platform-vectorizer-ru-test
 * 3. Отправляем request в platform-vectorizer-ru-test и получаем response
 * 4. Парсим response и выводим в консоль
 */
fun main() {
    val restClient = MlpRestClient(
        restUrl = "https://app.caila.io",
        clientToken = "1000062767.11003.hUCRZLEyG7sNhoFP9smDN5lxZyfC8MHGNkfE0CAv",
        context = systemContext
    )

    val request = VectorizerRequest(texts = listOf("Привет"))
    val response = restClient.processApi.predict(
        "just-ai",
        "platform-vectorizer-ru-test",
        JSON.stringify(request),
        null,
        null,
        null
    )

    val result = JSON.parse(response, VectorizerResponse::class.java)
    println(result)
}
