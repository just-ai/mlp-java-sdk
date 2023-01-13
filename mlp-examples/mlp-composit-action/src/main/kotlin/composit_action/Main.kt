package composit_action

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.mlp.gate.ActionDescriptorProto
import com.mlp.sdk.MlpResponse
import com.mlp.sdk.MlpService
import com.mlp.sdk.MlpServiceSDK
import com.mlp.sdk.Payload

const val ACCOUNT = "your_account"
const val GRAMMAR_MODEL = "your_text_model"
const val PUNCTUATION_MODEL = "your_punctuation_model"

fun main() {
    val action = CompositeTestAction()
    val actionSDK = MlpServiceSDK(action)

    actionSDK.start()
    actionSDK.blockUntilShutdown()
}

class CompositeTestAction : MlpService() {

    private val objectMapper = ObjectMapper().apply {
        registerModule(KotlinModule())
    }

    override fun getDescriptor(): ActionDescriptorProto {
        return ActionDescriptorProto.newBuilder()
            .setName("my-composit")
            .setFittable(true)
            .build()
    }

    override fun predict(req: Payload): MlpResponse {
        val request = objectMapper.readValue(req.data, CompositeTestActionRequest::class.java)

        val textModelResponse = pipelineClient.predict(ACCOUNT, GRAMMAR_MODEL, Payload("text/plain", request.data))
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