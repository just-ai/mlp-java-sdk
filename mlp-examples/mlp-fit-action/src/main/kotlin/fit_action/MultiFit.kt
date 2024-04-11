package fit_action

import com.mlp.gate.DatasetInfoProto
import com.mlp.gate.ServiceInfoProto
import com.mlp.sdk.MlpExecutionContext
import com.mlp.sdk.MlpFitServiceBase
import com.mlp.sdk.MlpPredictServiceBase
import com.mlp.sdk.storage.StorageFactory
import com.mlp.sdk.utils.JSON

class FitService(
    override val context: MlpExecutionContext
): MlpFitServiceBase<FitDatasetData, FitConfigData>(FIT_DATA_EXAMPLE, FIT_CONFIG_EXAMPLE) {

    private val storage = StorageFactory.getStorage(context)

    override suspend fun fit(data: FitDatasetData,
                     config: FitConfigData?,
                     modelDir: String,
                     previousModelDir: String?,
                     targetServiceInfo: ServiceInfoProto,
                     dataset: DatasetInfoProto
    ) {
        logger.warn("Start training ...")

        storage.saveState(JSON.stringify(data), "$modelDir/$MODEL_FILENAME_DATA")
        storage.saveState(JSON.stringify(config ?:FitConfigData(false)), "$modelDir/$MODEL_FILENAME_CONFIG")
        logger.info("state saved")
    }

    companion object {
        val FIT_DATA_EXAMPLE = FitDatasetData(mapOf("first" to "1"))
        val FIT_CONFIG_EXAMPLE = FitConfigData(true)

        val MODEL_FILENAME_DATA = "data.json"
        val MODEL_FILENAME_CONFIG = "config.json"
    }
}

class PredictService(
    override val context: MlpExecutionContext
): MlpPredictServiceBase<PredictRequestData, PredictResponseData>(REQUEST_EXAMPLE, RESPONSE_EXAMPLE) {

    private val storage = StorageFactory.getStorage(context)
    private val predictModelDir = StorageFactory.getDefaultStorageDir(context)

    val modelData: FitDatasetData by lazy {
        val modelDataStr = storage.loadState("$predictModelDir/${FitService.MODEL_FILENAME_DATA}")!!
        JSON.parse(modelDataStr, FitDatasetData::class.java)
    }
    val configData: FitConfigData by lazy {
        val configDataStr = storage.loadState("$predictModelDir/${FitService.MODEL_FILENAME_CONFIG}")!!
        JSON.parse(configDataStr, FitConfigData::class.java)
    }

    override fun predict(request: PredictRequestData): PredictResponseData {
        val res = modelData.map[request.text] ?: return PredictResponseData("no entry")
        val res2 = if (configData.upper) res.uppercase() else res.lowercase()
        return PredictResponseData(res2)
    }

    companion object {
        val REQUEST_EXAMPLE = PredictRequestData("first")
        val RESPONSE_EXAMPLE = PredictResponseData("1")
    }
}
