package com.platform.mpl.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.platform.mpl.sdk.PlatformClientSDK
import com.platform.mpl.sdk.loadClientConfig

object ClientSdkExample {

    val objectMapper = ObjectMapper()
    const val ACCOUNT = "your_account"
    const val MODEL = "your_model"
    const val TOKEN = "your_mpl_auth_token"
    const val CONFIG_PATH = "./examples/src/main/config/client.properties"

    @JvmStatic
    fun main(args: Array<String>) {
        val clientSDK = PlatformClientSDK(loadClientConfig(CONFIG_PATH))
        clientSDK.init()

        val payloadData = ClientPayloadData("your_data")

        clientSDK.predict(ACCOUNT, MODEL, objectMapper.writeValueAsString(payloadData))

        clientSDK.predict(ACCOUNT, MODEL, TOKEN, objectMapper.writeValueAsString(payloadData))

        clientSDK.shutdown()
    }

    data class ClientPayloadData(
        val data: String
    )
}