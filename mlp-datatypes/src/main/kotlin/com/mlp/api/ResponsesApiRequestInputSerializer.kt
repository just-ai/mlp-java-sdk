package com.mlp.api

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.mlp.api.datatypes.chatgpt.responsesapi.ResponsesApiRequestInput
import com.mlp.api.datatypes.chatgpt.responsesapi.ResponsesApiRequestItemsInput
import com.mlp.api.datatypes.chatgpt.responsesapi.ResponsesApiRequestTextInput

object ResponsesApiRequestInputSerializer : JsonSerializer<ResponsesApiRequestInput>() {

    override fun serialize(
        value: ResponsesApiRequestInput,
        gen: JsonGenerator,
        serializers: SerializerProvider
    ) {
        when (value) {
            is ResponsesApiRequestTextInput -> gen.writeString(value.value)
            is ResponsesApiRequestItemsInput -> {
                gen.writeStartArray()
                for (item in value.items) {
                    serializers.defaultSerializeValue(item, gen)
                }
                gen.writeEndArray()
            }
        }
    }
}
