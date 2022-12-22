package fit_action

import com.fasterxml.jackson.databind.ObjectMapper
import com.mpl.gate.ActionDescriptorProto
import com.mpl.sdk.MplAction
import com.mpl.sdk.MplActionSDK
import com.mpl.sdk.MplResponse
import com.mpl.sdk.MplResponseException
import com.mpl.sdk.Payload
import com.mpl.sdk.storage.StorageFactory
import java.lang.Exception

fun main() {
    val action = FitTestAction()
    val actionSDK = MplActionSDK(action)

    actionSDK.start()
    actionSDK.blockUntilShutdown()
}

class FitTestAction : MplAction() {

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
    ): MplResponse {
        val trainData = objectMapper.readValue(train.data, TransformerFitTrainData::class.java)
            .texts
        val targetData = objectMapper.readValue(targets.data, TransformerFitTargets::class.java)
            .items_list
            .map { it.items.first() }
            .map { it.value }
        if (trainData.size != targetData.size) {
            return MplResponseException(
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

    override fun predict(req: Payload, config: Payload?): MplResponse {
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
