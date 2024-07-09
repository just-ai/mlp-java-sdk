package com.mlp.api

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.mlp.api.datatypes.chatgpt.EmbeddingEncodingFormat
import com.mlp.api.datatypes.chatgpt.EmbeddingRequest
import com.mlp.api.datatypes.chatgpt.EmbeddingRequestArrayIntArray
import com.mlp.api.datatypes.chatgpt.EmbeddingRequestIntArray
import com.mlp.api.datatypes.chatgpt.EmbeddingRequestString
import com.mlp.api.datatypes.chatgpt.EmbeddingRequestStringArray
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

object EmbeddingRequestDeserializer : JsonDeserializer<EmbeddingRequest>() {
    override fun deserialize(jsonParser: JsonParser, context: DeserializationContext): EmbeddingRequest {
        val node: JsonNode = jsonParser.codec.readTree(jsonParser)
        val model = requiredParameter(node.get("model")?.asText(), context) { "Missing required parameter \"model\"" }
        val encodingFormatNode = node.get("encoding_format")
        val encodingFormat = encodingFormatNode?.let {
            jsonParser.codec.readValue(
                encodingFormatNode.traverse(jsonParser.codec), EmbeddingEncodingFormat::class.java
            )
        }
        val dimensions = node.get("dimensions")?.asInt()
        val user = node.get("user")?.asText()

        val inputNode = requiredParameter(node.get("input"), context) { "Missing required parameter \"input\"" }
        return if (inputNode.isArray) {
            if (inputNode.isEmpty) {
                return EmbeddingRequestStringArray(emptyList(), model, encodingFormat, dimensions, user)
            }

            val children = inputNode.elements()
            val first = children.next()
            when {
                first.isTextual -> {
                    val stringArrayInput = deserializeInputAsArray(first, children) { it.asText() }
                    EmbeddingRequestStringArray(stringArrayInput, model, encodingFormat, dimensions, user)
                }

                first.isInt -> {
                    val intArrayInput = deserializeInputAsArray(first, children) { it.asInt() }
                    EmbeddingRequestIntArray(intArrayInput, model, encodingFormat, dimensions, user)
                }

                first.isArray -> {
                    val arrayIntArrayInput = deserializeInputAsArray(first, children) { arrayOfInt ->
                        arrayOfInt.map { element ->
                            element.asInt()
                        }
                    }
                    EmbeddingRequestArrayIntArray(arrayIntArrayInput, model, encodingFormat, dimensions, user)
                }

                else -> throw MismatchedInputException.from(
                    context,
                    "Unsupported element type ${first.nodeType} for input parameter"
                )
            }
        } else {
            val stringInput = inputNode.asText()
            EmbeddingRequestString(
                stringInput, model, encodingFormat, dimensions, user
            )
        }
    }

    private inline fun <T> deserializeInputAsArray(
        first: JsonNode, iterator: Iterator<JsonNode>, map: (JsonNode) -> T
    ): List<T> {
        val input = mutableListOf(map(first))
        while (iterator.hasNext()) {
            input.add(map(iterator.next()))
        }
        return input
    }

    @OptIn(ExperimentalContracts::class)
    private inline fun <T> requiredParameter(
        parameter: T?,
        ctxt: DeserializationContext,
        lazyMessage: () -> String
    ): T {
        contract {
            returns() implies (parameter != null)
        }

        if (parameter == null) {
            val message = lazyMessage()
            throw MismatchedInputException.from(ctxt, message)
        } else {
            return parameter
        }
    }
}
