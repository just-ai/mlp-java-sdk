package com.mlp.sdk

import com.mlp.sdk.MlpExecutionContext.Companion.systemContext
import com.mlp.sdk.MlpServiceConfig.Companion.DEFAULT_THREAD_POOL_SIZE
import com.mlp.sdk.MlpServiceConfig.Companion.GRACEFUL_SHUTDOWN_CONNECTOR_MS
import com.mlp.sdk.MlpServiceConfig.Companion.GRACEFUL_SHUTDOWN_CONNECTOR_REQUEST_DELAY_MS
import com.mlp.sdk.MlpServiceConfig.Companion.GRPC_CONNECT_TIMEOUT_MS
import com.mlp.sdk.MlpServiceConfig.Companion.GRPC_SECURE
import com.mlp.sdk.utils.ConfigHelper

data class MlpServiceConfig(
    val initialGateUrls: List<String>,
    val connectionToken: String,

    val threadPoolSize: Int = DEFAULT_THREAD_POOL_SIZE,
    val shutdownConfig: ActionShutdownConfig = ActionShutdownConfig(),
    val grpcConnectTimeoutMs: Long = GRPC_CONNECT_TIMEOUT_MS,
    val grpcSecure: Boolean = GRPC_SECURE,
    val clientApiAuthToken: String? = null,
) {
    companion object {
        const val DEFAULT_THREAD_POOL_SIZE: Int = 10
        const val GRACEFUL_SHUTDOWN_CONNECTOR_MS: Long = 20000
        @Deprecated("Delete after 26 July")
        const val GRACEFUL_SHUTDOWN_CONNECTOR_REQUEST_DELAY_MS: Long = 3000
        const val GRPC_CONNECT_TIMEOUT_MS: Long = 10000
        const val GRPC_SECURE: Boolean = true
    }
}

data class ActionShutdownConfig(
    val actionConnectorMs: Long = GRACEFUL_SHUTDOWN_CONNECTOR_MS,
    @Deprecated("Delete after 26 July")
    val actionConnectorRequestDelayMs: Long = GRACEFUL_SHUTDOWN_CONNECTOR_REQUEST_DELAY_MS
)

/**
 * Loads properties using the system environments variables.
 */
fun loadActionConfig(configPath: String? = null): MlpServiceConfig =
    loadActionConfig(configPath, systemContext.environment)

fun loadActionConfig(configPath: String? = null, environment: Environment): MlpServiceConfig {
    val props = ConfigHelper.loadProperties(configPath, environment)
    return MlpServiceConfig(
        initialGateUrls = props["MLP_GRPC_HOST"]!!.split(",:"),
        connectionToken = props["MLP_SERVICE_TOKEN"]!!,
        threadPoolSize = props["MLP_THREAD_POOL_SIZE"]?.toInt()
            ?: DEFAULT_THREAD_POOL_SIZE,
        shutdownConfig = ActionShutdownConfig(
            actionConnectorMs = props["MLP_GRACEFUL_SHUTDOWN_CONNECTOR_MS"]?.toLong()
                ?: GRACEFUL_SHUTDOWN_CONNECTOR_MS,
            actionConnectorRequestDelayMs = props["MLP_GRACEFUL_SHUTDOWN_CONNECTOR_REQUEST_DELAY_MS"]?.toLong()
                ?: GRACEFUL_SHUTDOWN_CONNECTOR_REQUEST_DELAY_MS
        ),
        grpcConnectTimeoutMs = props["MLP_GRPC_CONNECT_TIMEOUT_MS"]?.toLong()
            ?: GRPC_CONNECT_TIMEOUT_MS,
        grpcSecure = props["MLP_GRPC_SECURE"]?.toBoolean() ?: GRPC_SECURE,
        clientApiAuthToken = props["MLP_CLIENT_TOKEN"]
    )
}
