package fit_action

import com.mlp.gate.DatasetInfoProto
import com.mlp.gate.ServiceInfoProto
import com.mlp.sdk.*
import com.mlp.sdk.storage.StorageFactory
import com.mlp.sdk.utils.JSON
import org.slf4j.LoggerFactory

class SingleFit: MlpServiceBase<FitDatasetData, FitConfigData, PredictRequestData, PredictResponseData>(
    FIT_DATA_EXAMPLE, FIT_CONFIG_EXAMPLE,
    REQUEST_EXAMPLE, RESPONSE_EXAMPLE
) {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val storage = StorageFactory.getStorage()
    private val predictModelDir = StorageFactory.getDefaultStorageDir()

    override fun fit(data: FitDatasetData,
                     config: FitConfigData?,
                     modelDir: String,
                     previousModelDir: String?,
                     targetServiceInfo: ServiceInfoProto,
                     dataset: DatasetInfoProto
    ) {
        log.warn("Start training ...")

        storage.saveState(JSON.stringify(data), "$predictModelDir/$MODEL_FILENAME_DATA")
        storage.saveState(JSON.stringify(config ?:FitConfigData(false)), "$predictModelDir/$MODEL_FILENAME_CONFIG")
        log.info("state saved")

        loadState()
    }

    var modelData: FitDatasetData? = null
    var configData: FitConfigData? = null
    init {
        loadState()
    }

    private fun loadState() {
        val modelDataStr = storage.loadState("$predictModelDir/$MODEL_FILENAME_DATA")!!
        modelData = JSON.parse(modelDataStr, FitDatasetData::class.java)
        val configDataStr = storage.loadState("$predictModelDir/$MODEL_FILENAME_CONFIG")!!
        configData = JSON.parse(configDataStr, FitConfigData::class.java)
    }

    override fun predict(request: PredictRequestData): PredictResponseData {
        val model = modelData
        val config = configData
        if (model == null || config == null) {
            throw MlpException("model doesn't have saved state")
        }

        val res = model.map[request.text] ?: return PredictResponseData("no entry")
        val res2 = if (config.upper) res.uppercase() else res.lowercase()
        return PredictResponseData(res2)
    }

    companion object {
        val FIT_DATA_EXAMPLE = FitDatasetData(mapOf("first" to "1"))
        val FIT_CONFIG_EXAMPLE = FitConfigData(true)

        val MODEL_FILENAME_DATA = "data.json"
        val MODEL_FILENAME_CONFIG = "config.json"

        val REQUEST_EXAMPLE = PredictRequestData("first")
        val RESPONSE_EXAMPLE = PredictResponseData("1")
    }
}