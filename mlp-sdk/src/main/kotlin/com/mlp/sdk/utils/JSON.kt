package com.mlp.sdk.utils

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.mlp.sdk.CommonErrorCode
import com.mlp.sdk.MlpError
import com.mlp.sdk.MlpException

object JSON {

    val mapper = ObjectMapper()
    init {
        mapper.registerModule(JavaTimeModule())
        mapper.registerModule(KotlinModule.Builder().build())

        mapper.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
        mapper.enable(JsonParser.Feature.ALLOW_COMMENTS)
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
    }

    fun parse(json: String) = mapper.readTree(json)

    fun parseObject(json: String) = mapper.readTree(json) as ObjectNode

    fun parseToMap(json: String): Map<String, String> {
        val o = mapper.readTree(json) as ObjectNode
        val ret = HashMap<String, String>()
        o.fieldNames().forEach {
            ret[it] = o.get(it).asText()
        }
        return ret
    }

    fun anyToObject(data: Any): ObjectNode =
            if (data is ObjectNode) {
                data
            } else {
                parseObject(stringify(data))
            }

    inline fun <reified T> parse(json: String):T =
            mapper.readValue(json, T::class.java)

    inline fun <reified T> parseList(json: String):List<T> {
        val array = mapper.readTree(json) as ArrayNode
        return array.map { mapper.treeToValue(it, T::class.java) }
    }

    fun <T> parse(json: String, clazz: Class<T>):T =
            mapper.readValue(json, clazz)

    fun <T> parse(json: String, tr: TypeReference<T>):T =
            mapper.readValue(json, tr)

    inline fun <reified T> parse(json: JsonNode):T =
            mapper.treeToValue(json, T::class.java)

    fun <T> parse(json: JsonNode, clazz: Class<T>):T =
            mapper.treeToValue(json, clazz)

    fun <T> stringify(data: T): String =
            mapper.writeValueAsString(data)

    fun toNode(data: Any): JsonNode =
            mapper.valueToTree(data)

    fun toObject(data: Any): ObjectNode =
            mapper.valueToTree(data)

    fun objectNode() = mapper.createObjectNode()

    fun escapeText(text: String): String {
        val t = toNode(text).toString()
        return t.substring(1, t.length - 1)
    }

    val Any.asJson: String
        get() = mapper.writeValueAsString(this)

    fun <T> JSON.parseOrThrowBadRequestMlpException(json: String, clazz: Class<T>): T = try {
        parse(json, clazz)
    } catch (e: JsonMappingException) {
        throw MlpException(MlpError(CommonErrorCode.BAD_REQUEST, e))
    }
}
