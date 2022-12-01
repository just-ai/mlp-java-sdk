package com.platform.mpl.sdk

import com.platform.mpl.sdk.PlatformClientConfig.Companion.CLIENT_PREDICT_TIMEOUT_MS
import com.platform.mpl.sdk.PlatformClientConfig.Companion.GRACEFUL_SHUTDOWN_CLIENT_MS
import com.platform.mpl.sdk.utils.ConfigHelper

class PlatformClientConfig(
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

fun loadClientConfig(configPath: String? = null): PlatformClientConfig {
    val props = ConfigHelper.loadProperties(configPath)
    return PlatformClientConfig(
        initialGateUrls = props["MPL_URL"]!!.split(",:"),
        connectionToken = props["MPL_CLIENT_TOKEN"]!!,
        clientPredictTimeoutMs = props["MPL_CLIENT_PREDICT_TIMEOUT_MS"]?.toLong()
            ?: CLIENT_PREDICT_TIMEOUT_MS,
        shutdownConfig = ClientShutdownConfig(
            clientMs = props["MPL_GRACEFUL_SHUTDOWN_CLIENT_MS"]?.toLong()
                ?: GRACEFUL_SHUTDOWN_CLIENT_MS
        )
    )
}

data class ClientShutdownConfig(
    val clientMs: Long = GRACEFUL_SHUTDOWN_CLIENT_MS
)
