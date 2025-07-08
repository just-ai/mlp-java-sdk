package com.mlp.api

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.mlp.api.datatypes.chatgpt.responsesapi.CodeInterpreterAutoContainer
import com.mlp.api.datatypes.chatgpt.responsesapi.CodeInterpreterContainer
import com.mlp.api.datatypes.chatgpt.responsesapi.CodeInterpreterIdContainer

object CodeInterpreterContainerSerializer : JsonSerializer<CodeInterpreterContainer>() {

    override fun serialize(
        value: CodeInterpreterContainer,
        gen: JsonGenerator,
        serializers: SerializerProvider
    ) {
        when (value) {
            is CodeInterpreterIdContainer -> gen.writeString(value.value)
            is CodeInterpreterAutoContainer -> {
                gen.writeStartObject()
                gen.writeStringField("type", value.type.name)
                value.fileIds?.let {
                    gen.writeArrayFieldStart("file_ids")
                    it.forEach { id -> gen.writeString(id) }
                    gen.writeEndArray()
                }
                gen.writeEndObject()
            }
        }
    }
}
