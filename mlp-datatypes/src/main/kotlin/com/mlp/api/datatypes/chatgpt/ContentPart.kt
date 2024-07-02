package com.mlp.api.datatypes.chatgpt

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param type
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(value = ImageContentPart::class, name = "image_url"),
    JsonSubTypes.Type(value = TextContentPart::class, name = "text")
)
interface ContentPart {
    @get:Schema(example = "null")
    val type: ContentPartType
}

