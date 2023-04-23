package fit_action

import com.mlp.sdk.MlpServiceSDK
import com.mlp.sdk.utils.JSON

enum class Mode {
    single,
    multi
}
data class InitConfigData(val mode: Mode = Mode.single)
data class FitDatasetData(val map: Map<String, String>)
data class FitConfigData(val upper: Boolean)
data class PredictRequestData(val text: String)
data class PredictResponseData(val text: String)

fun main() {
    val initConfig = JSON.parse(System.getenv().get("SERVICE_CONFIG") ?: """{"model":"single"}""", InitConfigData::class.java)
    val service =
        when (initConfig.mode) {
            Mode.single -> SingleFit()
            Mode.multi ->
                if (System.getenv()["MLP_STORAGE_DIR"].isNullOrEmpty()) {
                    FitService()
                } else {
                    PredictService()
                }
        }

    val mlp = MlpServiceSDK(service)

    mlp.start()
    mlp.blockUntilShutdown()
}
