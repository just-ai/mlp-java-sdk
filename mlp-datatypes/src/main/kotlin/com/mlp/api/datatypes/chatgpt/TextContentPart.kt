package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param type 
 * @param text 
 */
data class TextContentPart(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true) val type: TextContentPart.Type,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("text", required = true) val text: kotlin.String
) {

    /**
    * 
    * Values: text
    */
    enum class Type(val value: kotlin.String) {

        @JsonProperty("text") text("text")
    }

}

