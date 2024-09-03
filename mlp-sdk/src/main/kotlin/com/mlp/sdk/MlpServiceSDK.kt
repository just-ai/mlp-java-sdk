package com.mlp.sdk

import com.mlp.gate.ServiceToGateProto
import java.io.File
import java.lang.Runtime.getRuntime
import java.lang.System.currentTimeMillis
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking

class MlpServiceSDK(
    action: MlpService,
    initConfig: MlpServiceConfig? = null,
    dispatcher: CoroutineDispatcher? = null
) : WithExecutionContext, WithState() {

    init {
        if (action is MlpServiceBase<*,*,*,*,*>) {
            action.sdk = this
        }
    }
    /**
     * @param actionProvider Function that provides the MlpService, given an InstanceContext.
     * @param config Optional configuration for the MlpService; defaults to null.
     * @param dispatcher Optional CoroutineDispatcher for coroutine context; defaults to null.
     */
    constructor(
        actionProvider: () -> MlpService,
        config: MlpServiceConfig? = null,
        dispatcher: CoroutineDispatcher? = null
    ): this(actionProvider(), config, dispatcher)

    override val context: MlpExecutionContext = action.context

    val config: MlpServiceConfig = initConfig ?: loadActionConfig(environment = environment)

    @Deprecated("Use accountId instead", ReplaceWith("accountId"))
    val ACCOUNT_ID
        get() = environment.getOrThrow("MLP_ACCOUNT_ID")
    @Deprecated("Use modelId instead", ReplaceWith("modelId"))
    val MODEL_ID
        get() = environment.getOrThrow("MLP_MODEL_ID")

    val accountId = environment["MLP_ACCOUNT_ID"]
    val modelId = environment["MLP_MODEL_ID"]
    val instanceId = environment["MLP_INSTANCE_ID"]

    private val taskExecutor = TaskExecutor(action, config, dispatcher, context)

    fun start() {
        check(state.notStarted) { "SDK already started" }
        state.starting()
        setShutdownHook()

        taskExecutor.connectorsPool =
            ConnectorsPool(config.connectionToken, taskExecutor, config, context)

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
            ConnectorsPool(config.connectionToken, taskExecutor, config, context)
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

const val SDK_VERSION = 1