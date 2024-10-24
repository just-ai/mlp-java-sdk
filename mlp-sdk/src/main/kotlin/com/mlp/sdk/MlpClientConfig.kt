package com.mlp.sdk

import com.mlp.sdk.MlpClientConfig.Companion.CLIENT_KEEP_ALIVE_TIMEOUT_SECONDS
import com.mlp.sdk.MlpClientConfig.Companion.CLIENT_KEEP_ALIVE_TIME_SECONDS
import com.mlp.sdk.MlpClientConfig.Companion.CLIENT_KEEP_ALIVE_WITHOUT_CALLS
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
    val clientPredictRetryConfig: ClientPredictRetryConfig = ClientPredictRetryConfig(),
    val billingToken: String? = null,

    val keepAliveTimeSeconds: Long = CLIENT_KEEP_ALIVE_TIME_SECONDS,
    val keepAliveTimeoutSeconds: Long = CLIENT_KEEP_ALIVE_TIMEOUT_SECONDS,
    val keepAliveWithoutCalls: Boolean = CLIENT_KEEP_ALIVE_WITHOUT_CALLS,
) {

    companion object {
        const val CLIENT_PREDICT_TIMEOUT_MS: Long = 60000
        const val CLIENT_PREDICT_RETRY_MAX_ATTEMPTS: Int = 10
        const val CLIENT_PREDICT_RETRY_BACKOFF_MS: Long = 300
        val CLIENT_PREDICT_RETRYABLE_ERROR_CODES: List<String> =
            listOf(
                "mlp.gate.pps_limit_exceeded",
                "mlp-action.common.channel-closed-error"
            )

        const val CLIENT_KEEP_ALIVE_TIME_SECONDS: Long = 120
        const val CLIENT_KEEP_ALIVE_TIMEOUT_SECONDS: Long = 60
        const val CLIENT_KEEP_ALIVE_WITHOUT_CALLS: Boolean = true

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
        initialGateUrls = (props["MLP_GRPC_HOSTS"] ?: props["MLP_GRPC_HOST"])?.split(",") ?: error("Missed MLP_GRPC_HOST property"),
        restUrl = props["MLP_REST_URL"],
        clientToken = props["MLP_CLIENT_TOKEN"],
        billingToken = props["MLP_BILLING_TOKEN"],
        clientPredictTimeoutMs = props["MLP_CLIENT_PREDICT_TIMEOUT_MS"]?.toLong()
            ?: CLIENT_PREDICT_TIMEOUT_MS,
        shutdownConfig = ClientShutdownConfig(
            clientMs = props["MLP_GRACEFUL_SHUTDOWN_CLIENT_MS"]?.toLong()
                ?: GRACEFUL_SHUTDOWN_CLIENT_MS
        ),
        grpcSecure = props["MLP_GRPC_SECURE"]?.toBoolean() ?: GRPC_SECURE,
        maxBackoffSeconds = props["MLP_MAX_BACKOFF_SECONDS"]?.toLong() ?: MAX_BACKOFF_SECONDS,
        clientApiGateUrl = props["MLP_REST_URL"],

        keepAliveTimeSeconds = props["MLP_CLIENT_KEEP_ALIVE_TIME_SECONDS"]?.toLong() ?: CLIENT_KEEP_ALIVE_TIME_SECONDS,
        keepAliveTimeoutSeconds = props["MLP_CLIENT_KEEP_ALIVE_TIMEOUT_SECONDS"]?.toLong() ?: CLIENT_KEEP_ALIVE_TIMEOUT_SECONDS,
        keepAliveWithoutCalls = props["MLP_CLIENT_KEEP_ALIVE_WITHOUT_CALLS"]?.toBoolean() ?: CLIENT_KEEP_ALIVE_WITHOUT_CALLS,
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
