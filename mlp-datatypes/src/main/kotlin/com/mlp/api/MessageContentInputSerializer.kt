package com.mlp.api

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.mlp.api.datatypes.chatgpt.responsesapi.MessageContentInput
import com.mlp.api.datatypes.chatgpt.responsesapi.MessageContentItemsInput
import com.mlp.api.datatypes.chatgpt.responsesapi.MessageContentTextInput

object MessageContentInputSerializer : JsonSerializer<MessageContentInput>() {

    override fun serialize(
        value: MessageContentInput,
        gen: JsonGenerator,
        serializers: SerializerProvider,
    ) {
        when (value) {
            is MessageContentTextInput -> gen.writeString(value.value)
            is MessageContentItemsInput -> {
                gen.writeStartArray()
                for (item in value.items) {
                    serializers.defaultSerializeValue(item, gen)
                }
                gen.writeEndArray()
            }
        }
    }
}
