package com.mlp.api

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.mlp.api.datatypes.chatgpt.responsesapi.ResponsesApiToolChoice
import com.mlp.api.datatypes.chatgpt.responsesapi.ResponsesApiToolChoiceEnum
import com.mlp.api.datatypes.chatgpt.responsesapi.ResponsesApiToolChoiceFunction

object ResponsesApiToolChoiceDeserializer : JsonDeserializer<ResponsesApiToolChoice>() {
    override fun deserialize(jsonParser: JsonParser, context: DeserializationContext): ResponsesApiToolChoice {
        val node: JsonNode = jsonParser.codec.readTree(jsonParser)
        return when {
            node.isTextual -> {
                ResponsesApiToolChoiceEnum.valueOf(node.asText().lowercase())
            }

            node.isObject -> {
                val type = node.getRequired(context, "type").asText()
                val name = node.get("name")?.asText()
                ResponsesApiToolChoiceFunction(type, name)
            }

            else -> throw JsonMappingException.from(jsonParser, "Failed to deserialize ResponseToolChoice object")
        }
    }

    private fun JsonNode.getRequired(context: DeserializationContext, propertyName: String): JsonNode {
        return get(propertyName)
            ?: throw MismatchedInputException.from(context, "\"$propertyName\" parameter is missing.")
    }
}
