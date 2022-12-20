package fit_action

import com.fasterxml.jackson.databind.ObjectMapper
import com.mpl.sdk.MplAction
import com.mpl.sdk.MplActionSDK
import com.mpl.sdk.MplResponse
import com.mpl.sdk.MplResponseException
import com.mpl.sdk.Payload
import com.mpl.sdk.utils.S3Factory
import io.minio.GetObjectArgs
import io.minio.PutObjectArgs
import java.io.ByteArrayInputStream

fun main() {
    val action = FitTestAction()
    val actionSDK = MplActionSDK(action)

    actionSDK.start()
    actionSDK.blockUntilShutdown()
}

class FitTestAction: MplAction() {

    private val objectMapper = ObjectMapper()
    private val minioClient = S3Factory.createMinioClient()
    private val bucketName = S3Factory.getPlatformBucket()
    private val defaultStorageDir = S3Factory.getDefaultStorageDir()
    private val modelFileName = "model_file_name"

    private var model: FittedModel? = loadState()
    private lateinit var modelFilePath: String

    override fun fit(train: Payload, targets: Payload, config: Payload?, modelDir: String, previousModelDir: String?): MplResponse {

        val trainData = objectMapper.readValue(train.data, TrainData::class.java)
        val targetData = objectMapper.readValue(targets.data, TargetData::class.java)
        val configData = objectMapper.readValue(config?.data, ConfigData::class.java)

        val previousDirIsBlanked = previousModelDir?.isNotBlank() == true
        val storageDir = if (previousDirIsBlanked && modelDir.isNotBlank()) {
            throw RuntimeException("You try load another state, for this task it is unacceptable.")
        } else if (previousDirIsBlanked && modelDir.isBlank()) {
            previousModelDir
        } else if(previousModelDir.isNullOrEmpty() && modelDir.isNotBlank()) {
            modelDir
        } else {
            defaultStorageDir
        }

        if (trainData.texts.size != targetData.items.size) {
            return MplResponseException(
                RuntimeException("Inconsistent data sizes: ${trainData.texts.size} texts and ${targetData.items.size} items lists")
            )
        }

        val result = processFitData(trainData, targetData, configData)

        saveState(result, storageDir)

        return Payload(
            dataType = "text/plain",
            data = "ok"
        )
    }

    override fun predict(req: Payload, config: Payload?): MplResponse {
        val requestData = objectMapper.readValue(req.data, PredictRequestData::class.java)
        return requireNotNull(model).predict(requestData.itemId, requestData.text)
    }

    private fun saveState(content: FitProcessData, storageDir: String?) {
        val jsonResult = objectMapper.writeValueAsString(content)
        val inputStream = ByteArrayInputStream(jsonResult.toByteArray())
        val modelFilePath = "$storageDir/$modelFileName"
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .`object`(modelFilePath)
                .stream(inputStream, inputStream.available().toLong(), -1)
                .build()
        )
        this.modelFilePath = modelFilePath
        this.model = FittedModel(content)
    }

    private fun loadState(): FittedModel? {
        val modelResponseBytes = minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(bucketName)
                .`object`("$defaultStorageDir/$modelFilePath")
                .build()
        ).readAllBytes()
        if (modelResponseBytes.isEmpty()) {
            return null
        }
        val modelData = objectMapper.readValue(modelResponseBytes, FitProcessData::class.java)

        return FittedModel(modelData)
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