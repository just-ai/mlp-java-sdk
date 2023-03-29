package com.mlp.sdk

import com.mlp.sdk.MlpClientConfig.Companion.CLIENT_PREDICT_TIMEOUT_MS
import com.mlp.sdk.MlpClientConfig.Companion.GRACEFUL_SHUTDOWN_CLIENT_MS
import com.mlp.sdk.MlpClientConfig.Companion.GRPC_SECURE
import com.mlp.sdk.MlpClientConfig.Companion.MAX_BACKOFF_SECONDS
import com.mlp.sdk.utils.ConfigHelper

class MlpClientConfig(
    val initialGateUrls: List<String>,
    val connectionToken: String,
    val clientPredictTimeoutMs: Long = CLIENT_PREDICT_TIMEOUT_MS,
    val shutdownConfig: ClientShutdownConfig = ClientShutdownConfig(),
    val grpcSecure: Boolean = GRPC_SECURE,
    val maxBackoffSeconds: Long = MAX_BACKOFF_SECONDS,
    val clientApiGateUrl: String? = null,
) {

    companion object {
        const val CLIENT_PREDICT_TIMEOUT_MS: Long = 60000
        const val GRACEFUL_SHUTDOWN_CLIENT_MS: Long = 10000
        const val GRPC_SECURE: Boolean = true
        const val MAX_BACKOFF_SECONDS: Long = 10L
    }
}

fun loadClientConfig(configPath: String? = null): MlpClientConfig {
    val props = ConfigHelper.loadProperties(configPath)
    return MlpClientConfig(
        initialGateUrls = props["MLP_GRPC_HOST"]!!.split(",:"),
        connectionToken = props["MLP_CLIENT_TOKEN"]!!,
        clientPredictTimeoutMs = props["MLP_CLIENT_PREDICT_TIMEOUT_MS"]?.toLong()
            ?: CLIENT_PREDICT_TIMEOUT_MS,
        shutdownConfig = ClientShutdownConfig(
            clientMs = props["MLP_GRACEFUL_SHUTDOWN_CLIENT_MS"]?.toLong()
                ?: GRACEFUL_SHUTDOWN_CLIENT_MS
        ),
        grpcSecure = props["MLP_GRPC_SECURE"]?.toBoolean() ?: GRPC_SECURE,
        maxBackoffSeconds = props["MLP_MAX_BACKOFF_SECONDS"]?.toLong() ?: MAX_BACKOFF_SECONDS,
        clientApiGateUrl = props["MLP_REST_URL"]
    )
}

data class ClientShutdownConfig(
    val clientMs: Long = GRACEFUL_SHUTDOWN_CLIENT_MS
)
