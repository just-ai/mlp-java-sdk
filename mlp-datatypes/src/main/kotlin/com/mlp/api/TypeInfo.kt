package com.mlp.api

import com.mlp.api.datatypes.chatgpt.ChatCompletionRequest
import java.util.concurrent.ConcurrentHashMap

interface TypeInfoBase {
    val canonicalUrl: String
}

object TypeInfo {

    private val nameCache = ConcurrentHashMap<Class<*>, String>()

    fun canonicalName(clazz: Class<*>): String {
        return nameCache.computeIfAbsent(clazz, this::findCanonicalName)
    }

    inline fun <reified T> canonicalName(): String {
        return canonicalName(T::class.java)
    }

    private fun findCanonicalName(clazz: Class<*>): String {
        val c = Class.forName("${clazz.packageName}.type_info")

        val typeInfo = c.declaredFields.find { it.name == "INSTANCE" }!!.get(null) as TypeInfoBase

        return typeInfo.canonicalUrl + "#/" + clazz.simpleName
    }
}
