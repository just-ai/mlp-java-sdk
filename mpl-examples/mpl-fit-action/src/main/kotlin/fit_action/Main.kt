package fit_action

import com.fasterxml.jackson.databind.ObjectMapper
import com.mpl.sdk.MplAction
import com.mpl.sdk.MplActionSDK
import com.mpl.sdk.MplResponse
import com.mpl.sdk.MplResponseException
import com.mpl.sdk.Payload
import com.mpl.sdk.utils.S3Factory

fun main() {
    val action = FitTestAction()
    val actionSDK = MplActionSDK(action)

    actionSDK.start()
    actionSDK.blockUntilShutdown()
}

class FitTestAction : MplAction() {

    private val objectMapper = ObjectMapper()
    private val minioClient = S3Factory.getS3Service()
    private val defaultStorageDir = S3Factory.getDefaultStorageDir()
    private val modelFileName = "model_fit_result.json"

    private var model: FittedModel? = loadState()

    override fun fit(
        train: Payload,
        targets: Payload,
        config: Payload?,
        modelDir: String,
        previousModelDir: String?
    ): MplResponse {
        val trainData = objectMapper.readValue(train.data, TrainData::class.java)
        val targetData = objectMapper.readValue(targets.data, TargetData::class.java)
        val configData = objectMapper.readValue(config?.data, ConfigData::class.java)
        if (trainData.texts.size != targetData.items.size) {
            return MplResponseException(
                RuntimeException("Inconsistent data sizes: ${trainData.texts.size} texts and ${targetData.items.size} items lists")
            )
        }
        val result = processFitData(trainData, targetData, configData)
        minioClient.saveState(objectMapper.writeValueAsString(result), "$modelDir/$modelFileName")

        this.model = FittedModel(result)

        return Payload(
            dataType = "text/plain",
            data = "ok"
        )
    }

    override fun predict(req: Payload, config: Payload?): MplResponse {
        val requestData = objectMapper.readValue(req.data, PredictRequestData::class.java)
        return requireNotNull(model).predict(requestData.itemId, requestData.text)
    }

    private fun loadState(): FittedModel? {
        return minioClient.loadState("$defaultStorageDir/$modelFileName")
            ?.let { objectMapper.readValue(it, FitProcessData::class.java) }
            ?.let { FittedModel(it) }
    }

    private fun processFitData(trainData: TrainData?, targetData: TargetData?, config: ConfigData): FitProcessData {
        return trainData!!.texts.mapIndexed { i, data ->
            data to targetData!!.items[i]
        }.toMap().let {
            FitProcessData(it)
        }
    }
}

data class PredictRequestData(
    val itemId: String,
    val text: String
)

data class FitProcessData(
    val processedData: Map<String, Item?>
)

data class TrainData(
    val texts: List<String>
)

data class TargetData(
    val items: List<Item>
)

data class ConfigData(
    val accountId: String,
    val modelName: String,
    val apiToken: String,
    val batchSize: Int,
    val searchIndex: String
)

data class Item(
    val value: List<String>
)