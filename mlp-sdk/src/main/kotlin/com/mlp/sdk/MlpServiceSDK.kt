package com.mlp.sdk

import com.mlp.gate.ServiceToGateProto
import com.mlp.sdk.ConnectorsPool.Companion.getConnectorsPool
import com.mlp.sdk.TaskExecutor.Companion.getTaskExecutor
import com.mlp.sdk.storage.StorageFactory
import kotlinx.coroutines.runBlocking
import java.io.File
import java.lang.Runtime.getRuntime
import java.lang.System.currentTimeMillis
import kotlinx.coroutines.CoroutineDispatcher

class MlpServiceSDK(
    action: MlpService,
    initConfig: MlpServiceConfig? = null,
    override val context: InstanceContext = InstanceContext(),

    dispatcher: CoroutineDispatcher? = null
) : WithInstanceContext, WithState() {

    constructor(
        actionProvider: (InstanceContext) -> MlpService,
        config: MlpServiceConfig? = null,
        context: InstanceContext = InstanceContext(),
        dispatcher: CoroutineDispatcher? = null
    ): this(actionProvider(context), config, context, dispatcher)

    val config: MlpServiceConfig = initConfig ?: loadActionConfig(environment = environment)

    @Deprecated("Use accountId instead")
    val ACCOUNT_ID = environment["MLP_ACCOUNT_ID"]
    @Deprecated("Use modelId instead")
    val MODEL_ID = environment["MLP_MODEL_ID"]

    val accountId = environment["MLP_ACCOUNT_ID"]
    val modelId = environment["MLP_MODEL_ID"]
    val instanceId = environment["MLP_INSTANCE_ID"]

    private val taskExecutor: TaskExecutor = getTaskExecutor(action, config, dispatcher)

    val storageFactory = StorageFactory(environment)

    fun start() {
        check(state.notStarted) { "SDK already started" }
        state.starting()
        setShutdownHook()

        taskExecutor.connectorsPool =
            getConnectorsPool(config.connectionToken, taskExecutor, config)

        state.active()
        startupProbe()
    }

    fun blockUntilShutdown() {
        check(state.active) { "Action is not started" }
        state.awaitShutdown()
    }

    fun shutdownConnectorsPool() = runBlocking {
        taskExecutor.connectorsPool.gracefulShutdown()
    }

    fun getConnectorsPoolState() =
        taskExecutor.connectorsPool.state

    fun startConnectorsPool() {
        check(state.active) { "Action is not started" }
        check(taskExecutor.connectorsPool.state.isShutdownTypeState()) { "Connectors pool already started or starting" }

        taskExecutor.connectorsPool =
            getConnectorsPool(config.connectionToken, taskExecutor, config)
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
