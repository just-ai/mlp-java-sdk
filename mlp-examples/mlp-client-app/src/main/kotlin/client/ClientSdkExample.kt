package client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.mlp.sdk.MlpClientSDK

val objectMapper = ObjectMapper().apply {
    registerModule(KotlinModule())
}

fun main() {
    val clientSDK = MlpClientSDK()
    clientSDK.init()

    val payload = objectMapper.writeValueAsString("hello")
    val res = clientSDK.predict("just-ai", "test-action", payload)

    println(res)

    clientSDK.shutdown()
}