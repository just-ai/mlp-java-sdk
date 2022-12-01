package com.justai.caila.sdk

import com.justai.caila.sdk.CailaActionConfig.Companion.DEFAULT_THREAD_POOL_SIZE
import com.justai.caila.sdk.CailaActionConfig.Companion.GRACEFUL_SHUTDOWN_CONNECTOR_MS
import com.justai.caila.sdk.CailaActionConfig.Companion.GRACEFUL_SHUTDOWN_CONNECTOR_REQUEST_DELAY_MS
import com.justai.caila.sdk.CailaActionConfig.Companion.GRPC_CONNECT_TIMEOUT_MS
import com.justai.caila.sdk.utils.ConfigHelper

class CailaActionConfig(
    val initialGateUrls: List<String>,
    val connectionToken: String,
    val threadPoolSize: Int = DEFAULT_THREAD_POOL_SIZE,
    val shutdownConfig: ActionShutdownConfig = ActionShutdownConfig(),
    val grpcConnectTimeoutMs: Long = GRPC_CONNECT_TIMEOUT_MS,
    val pipeFutureTimeoutMs: Long = PIPE_FUTURE_TIMEOUT_MS,
    val pipeFutureScheduleMs: Long = PIPE_FUTURE_SCHEDULE_MS,
) {
    companion object {
        const val DEFAULT_THREAD_POOL_SIZE: Int = 10
        const val GRACEFUL_SHUTDOWN_CONNECTOR_MS: Long = 20000
        const val GRACEFUL_SHUTDOWN_CONNECTOR_REQUEST_DELAY_MS: Long = 3000
        const val GRPC_CONNECT_TIMEOUT_MS: Long = 10000
        const val PIPE_FUTURE_TIMEOUT_MS: Long = 45000
        const val PIPE_FUTURE_SCHEDULE_MS: Long = 60000
    }
}

data class ActionShutdownConfig(
    val actionConnectorMs: Long = GRACEFUL_SHUTDOWN_CONNECTOR_MS,
    val actionConnectorRequestDelayMs: Long = GRACEFUL_SHUTDOWN_CONNECTOR_REQUEST_DELAY_MS
)

fun loadActionConfig(configPath: String? = null): CailaActionConfig {
    val props = ConfigHelper.loadProperties(configPath)
    return CailaActionConfig(
        initialGateUrls = props["CAILA_URL"]!!.split(",:"),
        connectionToken = props["CAILA_TOKEN"]!!,
        threadPoolSize = props["CAILA_THREAD_POOL_SIZE"]?.toInt()
            ?: DEFAULT_THREAD_POOL_SIZE,
        shutdownConfig = ActionShutdownConfig(
            actionConnectorMs = props["CAILA_GRACEFUL_SHUTDOWN_CONNECTOR_MS"]?.toLong()
                ?: GRACEFUL_SHUTDOWN_CONNECTOR_MS,
            actionConnectorRequestDelayMs = props["CAILA_GRACEFUL_SHUTDOWN_CONNECTOR_REQUEST_DELAY_MS"]?.toLong()
                ?: GRACEFUL_SHUTDOWN_CONNECTOR_REQUEST_DELAY_MS
        ),
        grpcConnectTimeoutMs = props["CAILA_GRPC_CONNECT_TIMEOUT_MS"]?.toLong()
            ?: GRPC_CONNECT_TIMEOUT_MS,
        pipeFutureTimeoutMs = props["CAILA_PIPE_FUTURE_TIMEOUT_MS"]?.toLong()
            ?: GRPC_CONNECT_TIMEOUT_MS
    )
}