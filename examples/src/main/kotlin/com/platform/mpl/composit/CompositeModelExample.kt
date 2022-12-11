package com.platform.mpl.simple

import com.fasterxml.jackson.databind.ObjectMapper
import com.platform.mpl.gate.ClientResponseProto
import com.platform.mpl.sdk.Payload
import com.platform.mpl.sdk.PlatformAction
import com.platform.mpl.sdk.PlatformActionSDK
import com.platform.mpl.sdk.PlatformResponse

object CompositeModelExample {

    val objectMapper = ObjectMapper()

    @JvmStatic
    fun main(args: Array<String>) {
        val action = CompositeTestAction()
        val actionSDK = PlatformActionSDK(action)

        actionSDK.start()

        actionSDK.gracefulShutdown()
    }

    class CompositeTestAction : PlatformAction() {
        override fun predict(req: Payload): PlatformResponse {
            val request = objectMapper.readValue(req.data, CompositeTestActionRequest::class.java)
            return when (request.action) {
                "pipeline" -> executePipelineRequest(request)
                else -> throw RuntimeException("actionUnknownException")
            }
        }

        private fun executePipelineRequest(request: CompositeTestActionRequest): PlatformResponse {
            val pipelineResponse = pipelineClient.predict(request.targetModel, Payload("text/plain", request.data))
                .get()
            if (pipelineResponse.bodyCase == ClientResponseProto.BodyCase.ERROR) {
                throw RuntimeException("error")
            }
            return Payload(
                dataType = "text/plain",
                data = pipelineResponse.predict.data.json
            )
        }
    }

    data class CompositeTestActionRequest(
        val action: String,
        val targetModel: String,
        val data: String
    )

}