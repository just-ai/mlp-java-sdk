package com.platform.mpl.sdk

import com.platform.mpl.sdk.PlatformActionConfig.Companion.DEFAULT_THREAD_POOL_SIZE
import com.platform.mpl.sdk.PlatformActionConfig.Companion.GRACEFUL_SHUTDOWN_CONNECTOR_MS
import com.platform.mpl.sdk.PlatformActionConfig.Companion.GRACEFUL_SHUTDOWN_CONNECTOR_REQUEST_DELAY_MS
import com.platform.mpl.sdk.PlatformActionConfig.Companion.GRPC_CONNECT_TIMEOUT_MS
import com.platform.mpl.sdk.PlatformActionConfig.Companion.PIPE_PREDICT_SCHEDULE_MS
import com.platform.mpl.sdk.PlatformActionConfig.Companion.PIPE_PREDICT_TIMEOUT_MS
import com.platform.mpl.sdk.utils.ConfigHelper

class PlatformActionConfig(
    val initialGateUrls: List<String>,
    val connectionToken: String,
    val threadPoolSize: Int = DEFAULT_THREAD_POOL_SIZE,
    val shutdownConfig: ActionShutdownConfig = ActionShutdownConfig(),
    val grpcConnectTimeoutMs: Long = GRPC_CONNECT_TIMEOUT_MS,
    val pipeFutureTimeoutMs: Long = PIPE_PREDICT_TIMEOUT_MS,
    val pipeFutureScheduleMs: Long = PIPE_PREDICT_SCHEDULE_MS,
) {
    companion object {
        const val DEFAULT_THREAD_POOL_SIZE: Int = 10
        const val GRACEFUL_SHUTDOWN_CONNECTOR_MS: Long = 20000
        const val GRACEFUL_SHUTDOWN_CONNECTOR_REQUEST_DELAY_MS: Long = 3000
        const val GRPC_CONNECT_TIMEOUT_MS: Long = 10000
        const val PIPE_PREDICT_TIMEOUT_MS: Long = 45000
        const val PIPE_PREDICT_SCHEDULE_MS: Long = 60000
    }
}

data class ActionShutdownConfig(
    val actionConnectorMs: Long = GRACEFUL_SHUTDOWN_CONNECTOR_MS,
    val actionConnectorRequestDelayMs: Long = GRACEFUL_SHUTDOWN_CONNECTOR_REQUEST_DELAY_MS
)

fun loadActionConfig(configPath: String? = null): PlatformActionConfig {
    val props = ConfigHelper.loadProperties(configPath)
    return PlatformActionConfig(
        initialGateUrls = props["MPL_URL"]!!.split(",:"),
        connectionToken = props["MPL_TOKEN"]!!,
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
        pipeFutureTimeoutMs = props["MPL_PIPE_PREDICT_TIMEOUT_MS"]?.toLong()
            ?: PIPE_PREDICT_TIMEOUT_MS,
        pipeFutureScheduleMs = props["MPL_PIPE_PREDICT_SCHEDULE_MS"]?.toLong()
            ?: PIPE_PREDICT_SCHEDULE_MS,
    )
}