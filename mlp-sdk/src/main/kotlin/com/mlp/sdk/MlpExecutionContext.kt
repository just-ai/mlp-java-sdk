package com.mlp.sdk

import org.slf4j.ILoggerFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getILoggerFactory

/**
 * Keeps the settings for a specific SDK instance. This class lets you
 * run different MLP actions in the same JVM.
 *
 * @property environment Access to environment variables; you can change them if needed.
 * @property loggerFactory Use your own logger factory to handle logging.
 */
data class MlpExecutionContext(
    val environment: Environment = Environment(),
    val loggerFactory: ILoggerFactory = getILoggerFactory(),
) {
    companion object {
        val systemContext = MlpExecutionContext()
    }
}

class Environment(
    val envsOverride: Map<String, String> = emptyMap()
) {

    operator fun get(name: String): String? =
        envsOverride[name]
            ?: System.getenv(name)

    fun getOrThrow(name: String): String =
        get(name)
            ?: error("$name is missing from the environment")
}

interface WithExecutionContext {

    val context: MlpExecutionContext

    val environment: Environment
        get() = context.environment

    val loggerFactory: ILoggerFactory
        get() = context.loggerFactory

    val logger: Logger
        get() = loggerFactory.getLogger(javaClass.name)
}
