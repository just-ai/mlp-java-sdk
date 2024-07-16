package com.mlp.api

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.mlp.api.datatypes.chatgpt.NamedToolChoice
import com.mlp.api.datatypes.chatgpt.NamedToolChoiceFunction
import com.mlp.api.datatypes.chatgpt.ToolChoice
import com.mlp.api.datatypes.chatgpt.ToolChoiceEnum
import com.mlp.api.datatypes.chatgpt.ToolType

object ToolChoiceDeserializer : JsonDeserializer<ToolChoice>() {
    override fun deserialize(jsonParser: JsonParser, context: DeserializationContext): ToolChoice {
        val node: JsonNode = jsonParser.codec.readTree(jsonParser)
        return when {
            node.isTextual -> {
                ToolChoiceEnum.valueOf(node.asText().lowercase())
            }
            node.isObject -> {
                val type = ToolType.valueOf(node.getRequired(context, "type").asText().lowercase())
                val function = jsonParser.codec.treeToValue(
                    node.getRequired(context, "function"),
                    NamedToolChoiceFunction::class.java
                )
                NamedToolChoice(type, function)
            }

            else -> throw JsonMappingException.from(jsonParser, "Failed to deserialize ToolChoice object")
        }
    }

    private fun JsonNode.getRequired(context: DeserializationContext, propertyName: String): JsonNode {
        return get(propertyName)
            ?: throw MismatchedInputException.from(context, "\"$propertyName\" parameter is missing.")
    }
}
