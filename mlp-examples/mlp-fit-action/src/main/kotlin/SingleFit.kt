import com.mlp.gate.DatasetInfoProto
import com.mlp.gate.ServiceInfoProto
import com.mlp.sdk.MlpException
import com.mlp.sdk.MlpExecutionContext
import com.mlp.sdk.MlpServiceBase
import com.mlp.sdk.storage.StorageFactory
import com.mlp.sdk.utils.JSON

class SingleFit(
    override val context: MlpExecutionContext
) : MlpServiceBase<FitDatasetData, FitConfigData, PredictRequestData, Unit, PredictResponseData>(
    FIT_DATA_EXAMPLE, FIT_CONFIG_EXAMPLE,
    REQUEST_EXAMPLE, Unit, RESPONSE_EXAMPLE
) {

    private var modelData: FitDatasetData? = null
    private var configData: FitConfigData? = null

    private val storage = StorageFactory.getStorage(context)
    private val predictModelDir = StorageFactory.getDefaultStorageDir(context)

    init {
        loadState()
    }

    override suspend fun fit(
        data: FitDatasetData,
        config: FitConfigData?,
        modelDir: String,
        previousModelDir: String?,
        targetServiceInfo: ServiceInfoProto,
        dataset: DatasetInfoProto
    ) {
        logger.warn("Start training ...")

        storage.saveState(JSON.stringify(data), "$predictModelDir/$MODEL_FILENAME_DATA")
        storage.saveState(JSON.stringify(config ?: FitConfigData(false)), "$predictModelDir/$MODEL_FILENAME_CONFIG")
        logger.info("state saved")

        loadState()
    }

    private fun loadState() {
        val modelDataStr = storage.loadState("$predictModelDir/$MODEL_FILENAME_DATA")
        if (modelDataStr == null) {
            logger.warn("Couldn't find model data $predictModelDir/$MODEL_FILENAME_DATA")
            return
        }
        modelData = JSON.parse(modelDataStr, FitDatasetData::class.java)
        val configDataStr = storage.loadState("$predictModelDir/$MODEL_FILENAME_CONFIG")
        if (configDataStr == null) {
            logger.warn("Couldn't find model conf $predictModelDir/$MODEL_FILENAME_CONFIG")
            return
        }
        configData = JSON.parse(configDataStr, FitConfigData::class.java)
    }

    override suspend fun predict(request: PredictRequestData, config: Unit?): PredictResponseData {
        val model = modelData
        val conf = configData
        if (model == null || conf == null) {
            throw MlpException("model doesn't have saved state")
        }

        val res = model.map[request.text] ?: return PredictResponseData("no entry")
        val res2 = if (conf.upper) res.uppercase() else res.lowercase()
        return PredictResponseData(res2)
    }

    companion object {
        val FIT_DATA_EXAMPLE = FitDatasetData(mapOf("first" to "1"))
        val FIT_CONFIG_EXAMPLE = FitConfigData(true)

        const val MODEL_FILENAME_DATA = "data.json"
        const val MODEL_FILENAME_CONFIG = "config.json"

        val REQUEST_EXAMPLE = PredictRequestData("first")
        val RESPONSE_EXAMPLE = PredictResponseData("1")
    }
}

