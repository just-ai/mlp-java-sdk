package com.justai.caila.sdk

import com.justai.caila.sdk.CailaClientConfig.Companion.CLIENT_PREDICT_TIMEOUT_MS
import com.justai.caila.sdk.CailaClientConfig.Companion.GRACEFUL_SHUTDOWN_CLIENT_MS
import com.justai.caila.sdk.utils.ConfigHelper

class CailaClientConfig(
    val initialGateUrls: List<String>,
    val connectionToken: String,
    val clientPredictTimeoutMs: Long = CLIENT_PREDICT_TIMEOUT_MS,
    val shutdownConfig: ClientShutdownConfig = ClientShutdownConfig()
) {

    companion object {
        const val CLIENT_PREDICT_TIMEOUT_MS: Long = 60000
        const val GRACEFUL_SHUTDOWN_CLIENT_MS: Long = 10000
    }
}

fun loadClientConfig(configPath: String? = null): CailaClientConfig {
    val props = ConfigHelper.loadProperties(configPath)
    return CailaClientConfig(
        initialGateUrls = props["CAILA_URL"]!!.split(",:"),
        connectionToken = props["CAILA_CLIENT_TOKEN"]!!,
        clientPredictTimeoutMs = props["CAILA_CLIENT_PREDICT_TIMEOUT_MS"]?.toLong()
            ?: CLIENT_PREDICT_TIMEOUT_MS,
        shutdownConfig = ClientShutdownConfig(
            clientMs = props["CAILA_GRACEFUL_SHUTDOWN_CLIENT_MS"]?.toLong()
                ?: GRACEFUL_SHUTDOWN_CLIENT_MS
        )
    )
}

data class ClientShutdownConfig(
    val clientMs: Long = GRACEFUL_SHUTDOWN_CLIENT_MS
)
