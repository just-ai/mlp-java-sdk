package composit_action

import com.fasterxml.jackson.databind.ObjectMapper
import com.mpl.sdk.MplAction
import com.mpl.sdk.MplActionSDK
import com.mpl.sdk.MplResponse
import com.mpl.sdk.Payload

const val ACCOUNT = "your_account"
const val TEXT_MODEL = "your_text_model"
const val PUNCTUATION_MODEL = "your_punctuation_model"

fun main() {
    val action = CompositeTestAction()
    val actionSDK = MplActionSDK(action)

    actionSDK.start()
    actionSDK.blockUntilShutdown()
}

class CompositeTestAction : MplAction() {
    override fun predict(req: Payload): MplResponse {
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