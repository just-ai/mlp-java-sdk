package com.platform.mpl.sdk

import com.platform.mpl.gate.ActionToGateProto
import com.platform.mpl.sdk.utils.WithLogger
import kotlinx.coroutines.runBlocking
import java.io.File
import java.lang.Runtime.getRuntime
import java.lang.System.currentTimeMillis

class PlatformActionSDK(
    action: PlatformAction,
    private val config: PlatformActionConfig = loadActionConfig()
) : WithLogger, WithState() {

    private val taskExecutor: ActionTaskExecutor = ActionTaskExecutor(action, config)
    val pipelineClient: PipelineClient = PipelineClient(this, config)

    init {
        action.pipelineClient = pipelineClient
    }

    fun start() {
        check(state.notStarted) { "SDK already started" }
        state.starting()
        setShutdownHook()

        taskExecutor.connectorsPool =
            ConnectorsPool(config.connectionToken, taskExecutor, pipelineClient, config)

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

    fun sendToAnyGate(gateProto: ActionToGateProto) {
        check(state.active) { "Action is not started" }
        runBlocking {
            taskExecutor.connectorsPool
                .sendToAnyGate(gateProto)
        }
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
        const val SDK_COMPONENT_NAME = "PlatformActionSDK"
        const val STARTUP_PROBE_FILE_PATH = "/tmp/startup-probe"
    }
}

