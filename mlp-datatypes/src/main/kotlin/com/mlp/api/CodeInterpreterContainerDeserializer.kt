package com.mlp.api

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.mlp.api.datatypes.chatgpt.responsesapi.CodeInterpreterAutoContainer
import com.mlp.api.datatypes.chatgpt.responsesapi.CodeInterpreterContainer
import com.mlp.api.datatypes.chatgpt.responsesapi.CodeInterpreterContainerType
import com.mlp.api.datatypes.chatgpt.responsesapi.CodeInterpreterIdContainer

object CodeInterpreterContainerDeserializer : JsonDeserializer<CodeInterpreterContainer>() {
    override fun deserialize(jsonParser: JsonParser, context: DeserializationContext): CodeInterpreterContainer {
        val node: JsonNode = jsonParser.codec.readTree(jsonParser)
        return when {
            node.isTextual -> {
                CodeInterpreterIdContainer(value = node.asText().lowercase())
            }

            node.isObject -> {
                val type = CodeInterpreterContainerType.valueOf(node.getRequired(context, "type").asText())
                val fileIds = node.get("file_ids")?.map { it.asText() }
                CodeInterpreterAutoContainer(type, fileIds)
            }

            else -> throw JsonMappingException.from(jsonParser, "Failed to deserialize ResponseToolChoice object")
        }
    }

    private fun JsonNode.getRequired(context: DeserializationContext, propertyName: String): JsonNode {
        return get(propertyName)
            ?: throw MismatchedInputException.from(context, "\"$propertyName\" parameter is missing.")
    }
}
