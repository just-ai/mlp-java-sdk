package com.mlp.sdk

import com.mlp.sdk.MlpServiceConfig.Companion.DEFAULT_THREAD_POOL_SIZE
import com.mlp.sdk.MlpServiceConfig.Companion.GRACEFUL_SHUTDOWN_CONNECTOR_MS
import com.mlp.sdk.MlpServiceConfig.Companion.GRACEFUL_SHUTDOWN_CONNECTOR_REQUEST_DELAY_MS
import com.mlp.sdk.MlpServiceConfig.Companion.GRPC_CONNECT_TIMEOUT_MS
import com.mlp.sdk.MlpServiceConfig.Companion.GRPC_SECURE
import com.mlp.sdk.utils.ConfigHelper

class MlpServiceConfig(
    val initialGateUrls: List<String>,
    val connectionToken: String,
    val clientApiGateUrl: String? = null,
    val threadPoolSize: Int = DEFAULT_THREAD_POOL_SIZE,
    val shutdownConfig: ActionShutdownConfig = ActionShutdownConfig(),
    val grpcConnectTimeoutMs: Long = GRPC_CONNECT_TIMEOUT_MS,
    val pipeFutureTimeoutMs: Long = PIPE_FUTURE_TIMEOUT_MS,
    val pipeFutureScheduleMs: Long = PIPE_FUTURE_SCHEDULE_MS,
    val grpcSecure: Boolean = GRPC_SECURE,
) {
    companion object {
        const val DEFAULT_THREAD_POOL_SIZE: Int = 10
        const val GRACEFUL_SHUTDOWN_CONNECTOR_MS: Long = 20000
        const val GRACEFUL_SHUTDOWN_CONNECTOR_REQUEST_DELAY_MS: Long = 3000
        const val GRPC_CONNECT_TIMEOUT_MS: Long = 10000
        const val PIPE_FUTURE_TIMEOUT_MS: Long = 45000
        const val PIPE_FUTURE_SCHEDULE_MS: Long = 60000
        const val GRPC_SECURE: Boolean = true
    }
}

data class ActionShutdownConfig(
    val actionConnectorMs: Long = GRACEFUL_SHUTDOWN_CONNECTOR_MS,
    val actionConnectorRequestDelayMs: Long = GRACEFUL_SHUTDOWN_CONNECTOR_REQUEST_DELAY_MS
)

fun loadActionConfig(configPath: String? = null): MlpServiceConfig {
    val props = ConfigHelper.loadProperties(configPath)
    return MlpServiceConfig(
        initialGateUrls = props["MLP_URL"]!!.split(",:"),
        connectionToken = props["MLP_TOKEN"]!!,
        clientApiGateUrl = props["MLP_CLIENT_API_GATE_URL"],
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
        pipeFutureTimeoutMs = props["MLP_PIPE_FUTURE_TIMEOUT_MS"]?.toLong()
            ?: GRPC_CONNECT_TIMEOUT_MS,
        grpcSecure = props["MLP_GRPC_SECURE"]?.toBoolean() ?: GRPC_SECURE
    )
}