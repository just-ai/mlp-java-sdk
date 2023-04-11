package client

import com.mlp.sdk.MlpRestClient
import com.mlp.sdk.utils.JSON

data class VectorizerRequest(val texts: List<String>)

data class VectorizerEmbeddings(val vector: List<Double>)
data class VectorizerResponse(val embedded_texts: List<VectorizerEmbeddings>)

fun main() {
    val restClient = MlpRestClient(
        restUrl = "https://app.caila.io",
        clientToken = "1000062767.11003.hUCRZLEyG7sNhoFP9smDN5lxZyfC8MHGNkfE0CAv"
    )

    val request = VectorizerRequest(texts = listOf("Привет"))
    val res0 = restClient.processApi.predict("just-ai", "platform-vectorizer-ru-test", JSON.stringify(request), null, null)
    val res = JSON.parse(res0, VectorizerResponse::class.java)
    println(res)
}
