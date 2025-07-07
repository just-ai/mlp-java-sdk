package com.mlp.api

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.mlp.api.datatypes.chatgpt.responsesapi.InputItem
import com.mlp.api.datatypes.chatgpt.responsesapi.ResponsesApiRequestInput
import com.mlp.api.datatypes.chatgpt.responsesapi.ResponsesApiRequestItemsInput
import com.mlp.api.datatypes.chatgpt.responsesapi.ResponsesApiRequestTextInput


object ResponsesApiRequestInputDeserializer : JsonDeserializer<ResponsesApiRequestInput>() {

    override fun deserialize(jsonParser: JsonParser, context: DeserializationContext): ResponsesApiRequestInput {
        val node: JsonNode = jsonParser.codec.readTree(jsonParser)

        return when {
            node.isTextual -> {
                ResponsesApiRequestTextInput(node.asText())
            }

            node.isArray -> {
                ResponsesApiRequestItemsInput(
                    node.map { jsonParser.codec.treeToValue(it, InputItem::class.java) }
                )
            }

            else -> throw JsonMappingException.from(jsonParser, "Failed to deserialize ResponseRequestInput object")
        }
    }
}
