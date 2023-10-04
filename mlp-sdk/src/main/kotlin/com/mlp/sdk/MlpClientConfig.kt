package com.mlp.sdk

import com.mlp.sdk.MlpClientConfig.Companion.CLIENT_PREDICT_RETRYABLE_ERROR_CODES
import com.mlp.sdk.MlpClientConfig.Companion.CLIENT_PREDICT_RETRY_BACKOFF_MS
import com.mlp.sdk.MlpClientConfig.Companion.CLIENT_PREDICT_RETRY_MAX_ATTEMPTS
import com.mlp.sdk.MlpClientConfig.Companion.CLIENT_PREDICT_TIMEOUT_MS
import com.mlp.sdk.MlpClientConfig.Companion.GRACEFUL_SHUTDOWN_CLIENT_MS
import com.mlp.sdk.MlpClientConfig.Companion.GRPC_SECURE
import com.mlp.sdk.MlpClientConfig.Companion.MAX_BACKOFF_SECONDS
import com.mlp.sdk.MlpExecutionContext.Companion.systemContext
import com.mlp.sdk.utils.ConfigHelper

class MlpClientConfig(
        val initialGateUrls: List<String>,
        val restUrl: String?,
        val clientToken: String?,

        val clientPredictTimeoutMs: Long = CLIENT_PREDICT_TIMEOUT_MS,
        val shutdownConfig: ClientShutdownConfig = ClientShutdownConfig(),
        val grpcSecure: Boolean = GRPC_SECURE,
        val maxBackoffSeconds: Long = MAX_BACKOFF_SECONDS,
        val clientApiGateUrl: String? = null,
        val clientPredictRetryConfig: ClientPredictRetryConfig = ClientPredictRetryConfig()
) {

    companion object {
        const val CLIENT_PREDICT_TIMEOUT_MS: Long = 60000
        const val CLIENT_PREDICT_RETRY_MAX_ATTEMPTS: Int = 10
        const val CLIENT_PREDICT_RETRY_BACKOFF_MS: Long = 300
        val CLIENT_PREDICT_RETRYABLE_ERROR_CODES: List<String> = listOf("mlp.gate.pps_limit_exceeded")

        const val GRACEFUL_SHUTDOWN_CLIENT_MS: Long = 10000
        const val GRPC_SECURE: Boolean = true
        const val MAX_BACKOFF_SECONDS: Long = 10L
    }
}

/**
 * Loads properties using the system environments variables.
 */
fun loadClientConfig(configPath: String? = null): MlpClientConfig =
    loadClientConfig(configPath, systemContext.environment)


fun loadClientConfig(configPath: String? = null, environment: Environment): MlpClientConfig {
    val props = ConfigHelper.loadProperties(configPath, environment)
    return MlpClientConfig(
        initialGateUrls = props["MLP_GRPC_HOST"]?.split(",:") ?: error("Missed MLP_GRPC_HOST property"),
        restUrl = props["MLP_REST_URL"] ?: error("Missed MLP_REST_URL property"),
        clientToken = props["MLP_CLIENT_TOKEN"],
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

data class ClientPredictRetryConfig(
    val maxAttempts: Int = CLIENT_PREDICT_RETRY_MAX_ATTEMPTS,
    val backoffMs: Long = CLIENT_PREDICT_RETRY_BACKOFF_MS,
    val retryableErrorCodes: List<String> = CLIENT_PREDICT_RETRYABLE_ERROR_CODES
)

data class ClientShutdownConfig(
    val clientMs: Long = GRACEFUL_SHUTDOWN_CLIENT_MS
)
