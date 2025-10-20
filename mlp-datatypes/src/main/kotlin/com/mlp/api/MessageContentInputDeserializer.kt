package com.mlp.api

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.mlp.api.datatypes.chatgpt.responsesapi.MessageContentInput
import com.mlp.api.datatypes.chatgpt.responsesapi.MessageContentInputItem
import com.mlp.api.datatypes.chatgpt.responsesapi.MessageContentItemsInput
import com.mlp.api.datatypes.chatgpt.responsesapi.MessageContentTextInput


object MessageContentInputDeserializer : JsonDeserializer<MessageContentInput>() {

    override fun deserialize(jsonParser: JsonParser, context: DeserializationContext): MessageContentInput {
        val node: JsonNode = jsonParser.codec.readTree(jsonParser)

        return when {
            node.isTextual -> {
                MessageContentTextInput(node.asText())
            }

            node.isArray -> {
                MessageContentItemsInput(
                    node.map { jsonParser.codec.treeToValue(it, MessageContentInputItem::class.java) }
                )
            }

            else -> throw JsonMappingException.from(jsonParser, "Failed to deserialize MessageContentInput object")
        }
    }
}
