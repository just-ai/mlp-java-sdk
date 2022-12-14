package com.platform.mpl.simple

import com.fasterxml.jackson.databind.ObjectMapper
import com.platform.mpl.sdk.Payload
import com.platform.mpl.sdk.PlatformAction
import com.platform.mpl.sdk.PlatformActionSDK
import com.platform.mpl.sdk.PlatformResponse

const val ACCOUNT = "your_account"
const val TEXT_MODEL = "your_text_model"
const val PUNCTUATION_MODEL = "your_punctuation_model"

fun main() {
    val action = CompositeTestAction()
    val actionSDK = PlatformActionSDK(action)

    actionSDK.start()
    actionSDK.blockUntilShutdown()
}

class CompositeTestAction : PlatformAction() {
    override fun predict(req: Payload): PlatformResponse {
        val objectMapper = ObjectMapper()

        val request = objectMapper.readValue(req.data, CompositeTestActionRequest::class.java)

        val textModelResponse = pipelineClient.predict(ACCOUNT, TEXT_MODEL, Payload("text/plain", request.data))
            .get().predict.data.json
        val punctuationModelResponse =
            pipelineClient.predict(ACCOUNT, PUNCTUATION_MODEL, Payload("text/plain", textModelResponse))
                .get().predict.data.json

        val finalResult = objectMapper.readValue(punctuationModelResponse, PunctuationPayload::class.java)
        return Payload(
            "text/plain",
            finalResult.text
        )
    }
}

data class CompositeTestActionRequest(
    val data: String
)

data class PunctuationPayload(
    var text: String
)