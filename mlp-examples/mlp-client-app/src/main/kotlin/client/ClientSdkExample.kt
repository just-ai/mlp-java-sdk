package client

import com.fasterxml.jackson.databind.ObjectMapper
import com.mlp.sdk.MlpClientSDK

val objectMapper = ObjectMapper()
const val ACCOUNT = "your_account"
const val MODEL = "your_model"

fun main() {
    val clientSDK = MlpClientSDK()
    clientSDK.init()

    val dataToPredict = getDataToPredict()
    val results = dataToPredict.map { data ->
        val payload = objectMapper.writeValueAsString(ClientPayloadData(data))
        clientSDK.predict(ACCOUNT, MODEL, payload)
    }

    /* process results */

    clientSDK.shutdown()
}

fun getDataToPredict(): List<String> {
    return listOf("hello", "goodbye")
}

data class ClientPayloadData(
    val data: String
)