package com.mlp.sdk

import org.slf4j.ILoggerFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getILoggerFactory

/**
 * Holds the context for a particular SDK instance.
 * With this class you can run several different MLP actions at same JVM.
 *
 * @property environment Environment variables, can be overridden.
 * @property loggerFactory Custom logger factory.
 */
data class InstanceContext(
    val environment: Environment = Environment(),
    val loggerFactory: ILoggerFactory = getILoggerFactory(),
)

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

interface WithInstanceContext {

    val context: InstanceContext

    val environment: Environment
        get() = context.environment

    val loggerFactory: ILoggerFactory
        get() = context.loggerFactory

    val logger: Logger
        get() = loggerFactory.getLogger(javaClass.name)
}
