package fit_action

import com.mlp.sdk.MlpExecutionContext.Companion.systemContext
import com.mlp.sdk.MlpServiceSDK
import com.mlp.sdk.utils.JSON.parse
import fit_action.Mode.multi
import fit_action.Mode.single

enum class Mode {
    single,
    multi
}
data class InitConfigData(val mode: Mode = single)
data class FitDatasetData(val map: Map<String, String>)
data class FitConfigData(val upper: Boolean)
data class PredictRequestData(val text: String)
data class PredictResponseData(val text: String)

fun main() {
    val initConfig = parse<InitConfigData>(systemContext.environment["SERVICE_CONFIG"] ?: """{"mode":"single"}""")
    val service =
        when (initConfig.mode) {
            single -> SingleFit(systemContext)
            multi ->
                if (systemContext.environment["MLP_STORAGE_DIR"].isNullOrEmpty()) {
                    FitService(systemContext)
                } else {
                    PredictService(systemContext)
                }
        }

    val mlp = MlpServiceSDK(service)

    mlp.start()
    mlp.blockUntilShutdown()
}
