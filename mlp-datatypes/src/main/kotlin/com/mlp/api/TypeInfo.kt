package com.mlp.api

import com.mlp.api.datatypes.chatgpt.ChatCompletionRequest
import java.util.concurrent.ConcurrentHashMap

interface TypeInfoBase {
    val canonicalUrl: String
}

object TypeInfo {
    private val nameCache = ConcurrentHashMap<Class<*>, String>()

    fun canonicalName(clazz: Class<*>): String? {
        return nameCache.computeIfAbsent(clazz, this::findCanonicalName).let { if (it == "") null else it }
    }

    inline fun <reified T> canonicalName(): String? {
        return canonicalName(T::class.java)
    }

    private fun findCanonicalName(clazz: Class<*>): String {
        return kotlin.runCatching {
            val c = Class.forName("${clazz.packageName}.type_info")

            val typeInfo = c.declaredFields.find { it.name == "INSTANCE" }!!.get(null) as TypeInfoBase

            typeInfo.canonicalUrl + "#/" + clazz.simpleName
        }.getOrDefault("")
    }
}
