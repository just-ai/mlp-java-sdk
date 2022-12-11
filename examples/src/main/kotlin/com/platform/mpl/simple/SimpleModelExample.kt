package com.platform.mpl.simple

import com.fasterxml.jackson.databind.ObjectMapper
import com.platform.mpl.sdk.Payload
import com.platform.mpl.sdk.PlatformAction
import com.platform.mpl.sdk.PlatformActionSDK
import com.platform.mpl.sdk.PlatformResponse

object SimpleModelExample {

    val objectMapper = ObjectMapper()

    @JvmStatic
    fun main(args: Array<String>) {
        val action = SimpleTestAction()
        val actionSDK = PlatformActionSDK(action)

        actionSDK.start()

        actionSDK.gracefulShutdown()
    }

    class SimpleTestAction: PlatformAction() {
        override fun predict(req: Payload): PlatformResponse {
            val request = objectMapper.readValue(req.data, SimpleTestActionRequest::class.java)
            return when(request.action) {
                "hello" -> Payload("text/plain", "\"response from action\"")
                else -> throw RuntimeException("actionUnknownException")
            }
        }
    }

    data class SimpleTestActionRequest(
        val action: String
    )

}