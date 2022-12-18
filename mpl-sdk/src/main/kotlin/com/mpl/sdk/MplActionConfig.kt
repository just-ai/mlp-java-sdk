package com.mpl.sdk

import com.mpl.sdk.MplActionConfig.Companion.DEFAULT_THREAD_POOL_SIZE
import com.mpl.sdk.MplActionConfig.Companion.GRACEFUL_SHUTDOWN_CONNECTOR_MS
import com.mpl.sdk.MplActionConfig.Companion.GRACEFUL_SHUTDOWN_CONNECTOR_REQUEST_DELAY_MS
import com.mpl.sdk.MplActionConfig.Companion.GRPC_CONNECT_TIMEOUT_MS
import com.mpl.sdk.MplActionConfig.Companion.GRPC_SECURE
import com.mpl.sdk.utils.ConfigHelper

class MplActionConfig(
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

fun loadActionConfig(configPath: String? = null): MplActionConfig {
    val props = ConfigHelper.loadProperties(configPath)
    return MplActionConfig(
        initialGateUrls = props["MPL_URL"]!!.split(",:"),
        connectionToken = props["MPL_TOKEN"]!!,
        clientApiGateUrl = props["MPL_CLIENT_API_GATE_URL"],
        threadPoolSize = props["MPL_THREAD_POOL_SIZE"]?.toInt()
            ?: DEFAULT_THREAD_POOL_SIZE,
        shutdownConfig = ActionShutdownConfig(
            actionConnectorMs = props["MPL_GRACEFUL_SHUTDOWN_CONNECTOR_MS"]?.toLong()
                ?: GRACEFUL_SHUTDOWN_CONNECTOR_MS,
            actionConnectorRequestDelayMs = props["MPL_GRACEFUL_SHUTDOWN_CONNECTOR_REQUEST_DELAY_MS"]?.toLong()
                ?: GRACEFUL_SHUTDOWN_CONNECTOR_REQUEST_DELAY_MS
        ),
        grpcConnectTimeoutMs = props["MPL_GRPC_CONNECT_TIMEOUT_MS"]?.toLong()
            ?: GRPC_CONNECT_TIMEOUT_MS,
        pipeFutureTimeoutMs = props["MPL_PIPE_FUTURE_TIMEOUT_MS"]?.toLong()
            ?: GRPC_CONNECT_TIMEOUT_MS,
        grpcSecure = props["MPL_GRPC_SECURE"]?.toBoolean() ?: GRPC_SECURE
    )
}