package fit_action

import com.fasterxml.jackson.databind.ObjectMapper
import com.mlp.gate.ActionDescriptorProto
import com.mlp.sdk.MlpResponse
import com.mlp.sdk.MlpResponseException
import com.mlp.sdk.MlpService
import com.mlp.sdk.MlpServiceSDK
import com.mlp.sdk.Payload
import com.mlp.sdk.storage.StorageFactory

fun main() {
    val action = FitTestAction()
    val actionSDK = MlpServiceSDK(action)

    actionSDK.start()
    actionSDK.blockUntilShutdown()
}

class FitTestAction : MlpService() {

    private val objectMapper = ObjectMapper()
    private val storage = StorageFactory.getStorage()
    private val defaultStorageDir = StorageFactory.getDefaultStorageDir()
    private val modelFileName = "model_fit_result.json"

    private var model: FittedModel? = loadState()

    override fun getDescriptor(): ActionDescriptorProto {
        return ActionDescriptorProto.newBuilder()
            .setName("my-fit")
            .setFittable(true)
            .build()
    }

    override fun fit(
        train: Payload,
        targets: Payload,
        config: Payload?,
        modelDir: String,
        previousModelDir: String?
    ): MlpResponse {
        val trainData = objectMapper.readValue(train.data, TransformerFitTrainData::class.java)
            .texts
        val targetData = objectMapper.readValue(targets.data, TransformerFitTargets::class.java)
            .items_list
            .map { it.items.first() }
            .map { it.value }
        if (trainData.size != targetData.size) {
            return MlpResponseException(
                RuntimeException("Inconsistent data sizes: ${trainData.size} texts and ${targetData.size} items lists")
            )
        }
        val result = processFitData(trainData, targetData)
        storage.saveState(objectMapper.writeValueAsString(result), "$modelDir/$modelFileName")
        this.model = FittedModel(result)

        return Payload(
            dataType = "text/plain",
            data = "ok"
        )
    }

    override fun predict(req: Payload, config: Payload?): MlpResponse {
        val requestData = objectMapper.readValue(req.data, PredictRequestData::class.java)
        return requireNotNull(model).predict(requestData.number)
    }

    private fun loadState(): FittedModel? {
        return try {
            storage.loadState("$defaultStorageDir/$modelFileName")
                ?.let { objectMapper.readValue(it, FitProcessData::class.java) }
                ?.let { FittedModel(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun processFitData(trainData: List<String>, targetData: List<String>): FitProcessData {
        return targetData.mapIndexed { i, data ->
            data to trainData[i]
        }.toMap().let {
            FitProcessData(it)
        }
    }
}

data class PredictRequestData(
    val number: String
)

data class FitProcessData(val processedData: Map<String, String>)
data class TransformerFitTrainData(val texts: List<String>)
data class TransformerFitTargetItem(val value: String)
data class TransformerFitTargetItems(val items: List<TransformerFitTargetItem>)
data class TransformerFitTargets(val items_list: List<TransformerFitTargetItems>, val extra_items_list: List<TransformerFitTargetItems>)

class FittedModel(
    private val data: FitProcessData
) {

    fun predict(number: String): MlpResponse {
        val value = data.processedData[number]
            ?: return MlpResponseException(RuntimeException("No element found"))
        return Payload(value)
    }

}