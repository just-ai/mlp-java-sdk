package com.platform.mpl.simple

import com.fasterxml.jackson.databind.ObjectMapper
import com.platform.mpl.sdk.Payload
import com.platform.mpl.sdk.PlatformAction
import com.platform.mpl.sdk.PlatformActionSDK
import com.platform.mpl.sdk.PlatformResponse

fun main() {
    val action = SimpleTestAction()
    val actionSDK = PlatformActionSDK(action)

    actionSDK.start()

    actionSDK.blockUntilShutdown()
}

class SimpleTestAction: PlatformAction() {
    override fun predict(req: Payload): PlatformResponse {
        val objectMapper = ObjectMapper()

        val request = objectMapper.readValue(req.data, SimpleTestActionRequest::class.java)
        return when(request.action) {
            "hello" -> Payload("text/plain", "\"response from action\"")
            else -> throw RuntimeException("actionUnknownException")
        }
    }

    override fun ext(methodName: String, params: Map<String, Payload>): PlatformResponse {
        val data = params["text"]?.data ?: throw RuntimeException("data must be not null")
        val extendedData = when (methodName) {
            "toUpperCase" -> data.uppercase()
            "toLowerCase" -> data.lowercase()
            else -> data
        }
        return Payload("text/plain", extendedData)
    }
}

data class SimpleTestActionRequest(
    val action: String
)