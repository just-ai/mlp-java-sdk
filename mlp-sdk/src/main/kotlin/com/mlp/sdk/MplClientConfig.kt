package com.mlp.sdk

import com.mlp.sdk.MplClientConfig.Companion.CLIENT_PREDICT_TIMEOUT_MS
import com.mlp.sdk.MplClientConfig.Companion.GRACEFUL_SHUTDOWN_CLIENT_MS
import com.mlp.sdk.MplClientConfig.Companion.GRPC_SECURE
import com.mlp.sdk.utils.ConfigHelper

class MplClientConfig(
    val initialGateUrls: List<String>,
    val connectionToken: String,
    val clientPredictTimeoutMs: Long = CLIENT_PREDICT_TIMEOUT_MS,
    val shutdownConfig: ClientShutdownConfig = ClientShutdownConfig(),
    val grpcSecure: Boolean = GRPC_SECURE,
) {

    companion object {
        const val CLIENT_PREDICT_TIMEOUT_MS: Long = 60000
        const val GRACEFUL_SHUTDOWN_CLIENT_MS: Long = 10000
        const val GRPC_SECURE: Boolean = true
    }
}

fun loadClientConfig(configPath: String? = null): MplClientConfig {
    val props = ConfigHelper.loadProperties(configPath)
    return MplClientConfig(
        initialGateUrls = props["MPL_URL"]!!.split(",:"),
        connectionToken = props["MPL_CLIENT_TOKEN"]!!,
        clientPredictTimeoutMs = props["MPL_CLIENT_PREDICT_TIMEOUT_MS"]?.toLong()
            ?: CLIENT_PREDICT_TIMEOUT_MS,
        shutdownConfig = ClientShutdownConfig(
            clientMs = props["MPL_GRACEFUL_SHUTDOWN_CLIENT_MS"]?.toLong()
                ?: GRACEFUL_SHUTDOWN_CLIENT_MS
        ),
        grpcSecure = props["MPL_GRPC_SECURE"]?.toBoolean() ?: GRPC_SECURE
    )
}

data class ClientShutdownConfig(
    val clientMs: Long = GRACEFUL_SHUTDOWN_CLIENT_MS
)
