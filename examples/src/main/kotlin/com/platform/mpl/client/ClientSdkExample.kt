package com.platform.mpl.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.platform.mpl.sdk.PlatformClientSDK
import com.platform.mpl.sdk.loadClientConfig

val objectMapper = ObjectMapper()
const val ACCOUNT = "your_account"
const val MODEL = "your_model"
const val CONFIG_PATH = "./examples/src/main/config/client.properties"

fun main() {
    val clientSDK = PlatformClientSDK(loadClientConfig(CONFIG_PATH))
    clientSDK.init()

    val dataToPredict = getDataToPredict()
    val results = dataToPredict.map { data ->
        val payload = objectMapper.writeValueAsString(ClientPayloadData(data))
        clientSDK.predict("your_account", "your_model", payload)
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