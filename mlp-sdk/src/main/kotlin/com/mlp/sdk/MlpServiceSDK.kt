package com.mlp.sdk

import com.mlp.gate.ServiceToGateProto
import com.mlp.sdk.utils.WithLogger
import kotlinx.coroutines.runBlocking
import java.io.File
import java.lang.Runtime.getRuntime
import java.lang.System.currentTimeMillis


class MlpServiceSDK(
    action: MlpService,
    val config: MlpServiceConfig = loadActionConfig()
) : WithLogger, WithState() {

    val ACCOUNT_ID = System.getenv("MLP_ACCOUNT_ID")
    val MODEL_ID = System.getenv("MLP_MODEL_ID")

    private val taskExecutor: TaskExecutor = TaskExecutor(action, config)

    fun start() {
        check(state.notStarted) { "SDK already started" }
        state.starting()
        setShutdownHook()

        taskExecutor.connectorsPool =
            ConnectorsPool(config.connectionToken, taskExecutor, config)

        state.active()
        startupProbe()
    }

    fun blockUntilShutdown() {
        check(state.active) { "Action is not started" }
        state.awaitShutdown()
    }

    fun gracefulShutdown() {
        if (!state.active && !state.starting) {
            return
        }

        state.shuttingDown()

        runBlocking {
            taskExecutor.connectorsPool
                .gracefulShutdown()
        }

        taskExecutor.cancelAll()
        state.shutdown()
    }

    fun stop() {
        if (!state.active && !state.starting) {
            return
        }

        state.shuttingDown()

        runBlocking {
            taskExecutor.connectorsPool
                .shutdownNow()
        }

        taskExecutor.cancelAll()
        state.shutdown()
    }

    suspend fun send(connectorId: Long, toGateProto: ServiceToGateProto) {
        taskExecutor.connectorsPool.send(connectorId, toGateProto)
    }

    private fun setShutdownHook() {
        getRuntime().addShutdownHook(Thread {
            logger.info("Shutdown hook started")
            gracefulShutdown()
        })
    }

    private fun startupProbe() {
        File(STARTUP_PROBE_FILE_PATH)
            .writeText("${currentTimeMillis() / 1000}")
    }

    override fun toString() = SDK_COMPONENT_NAME

    companion object {
        const val SDK_COMPONENT_NAME = "MlpServiceSDK"
        const val STARTUP_PROBE_FILE_PATH = "/tmp/startup-probe"
    }
}

data class ModelInfo(
    val accountId: Long,
    val modelId: Long,
    val modelName: String,
)
