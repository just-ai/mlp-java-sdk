package com.mlp.api.datatypes.chatgpt

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param type
 * @param text
 */
data class TextContentPart(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true) override val type: ContentPartType,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("text", required = true) val text: kotlin.String
) : ContentPart
