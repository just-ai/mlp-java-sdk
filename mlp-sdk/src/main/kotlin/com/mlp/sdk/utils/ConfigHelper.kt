package com.mlp.sdk.utils

import com.mlp.sdk.Environment
import com.mlp.sdk.MlpExecutionContext.Companion.systemContext
import java.io.File
import java.io.FileInputStream
import java.lang.Exception
import java.util.*
import kotlin.collections.HashMap

object ConfigHelper {

    /**
     * Loads properties using the system environments variables.
     */
    fun loadProperties(configPath: String? = null): Map<String, String> =
        loadProperties(configPath, systemContext.environment)

    fun loadProperties(configPath: String? = null, environment: Environment): Map<String, String> {
        val p = HashMap<String, String>()
        p.putAll(loadFromPropsFile("./src/main/conf/default.properties"))
        p.putAll(loadFromPropsFile("./src/main/conf/local.properties"))
        p.putAll(loadFromResource("/default.properties"))
        p.putAll(loadFromResource("/local.properties"))
        configPath?.let {
            p.putAll(loadFromPropsFile(it))
        }
        p.putAll(loadFromPropsFile(System.getProperties().getProperty("config")))
        p.putAll(loadFromEnv())
        p.putAll(loadFromSystemProps())
        p.putAll(environment.envsOverride)
        return p
    }

    private fun loadFromPropsFile(file: String?): Map<String, String> {
        val p = Properties()
        // load from development props files
        if (File(file ?: return emptyMap()).exists()) {
            p.load(FileInputStream(file))
        }
        val pp = HashMap<String, String>()
        p.forEach {
            pp[it.key.toString()] = it.value.toString()
        }
        return pp
    }

    private fun loadFromResource(resource: String): Map<String, String> {
        try {
            val p = Properties()
            p.load(this.javaClass.getResourceAsStream(resource))
            val pp = HashMap<String, String>()
            p.forEach {
                pp[it.key.toString()] = it.value.toString()
            }
            return pp
        } catch (e: Exception) {
            return emptyMap()
        }
    }

    private fun loadFromSystemProps(): Map<String, String> {
        val pp = HashMap<String, String>()
        System.getProperties().forEach {
            pp[it.key.toString()] = it.value.toString()
        }
        return pp
    }

    private fun loadFromEnv(): Map<String, String> {
        return System.getenv()
    }
}
