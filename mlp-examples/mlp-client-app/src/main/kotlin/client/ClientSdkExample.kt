package client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.mlp.sdk.MlpClientSDK
import kotlinx.coroutines.runBlocking

val objectMapper = ObjectMapper().apply {
    registerModule(KotlinModule())
}

fun main() = runBlocking {
    val clientSDK = MlpClientSDK()

    val payload = objectMapper.writeValueAsString("hello")
    val res = clientSDK.predict("just-ai", "test-action", payload)

    println(res)

    clientSDK.shutdown()
}