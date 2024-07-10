package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.mlp.api.datatypes.chatgpt.ContentPart
import com.mlp.api.datatypes.chatgpt.ContentPartType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param text 
 */
data class TextContentPart(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("text", required = true) val text: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true) override val type: ContentPartType
) : ContentPart{

}

