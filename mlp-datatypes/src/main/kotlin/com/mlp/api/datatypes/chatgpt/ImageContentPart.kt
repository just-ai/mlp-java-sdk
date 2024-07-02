package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.mlp.api.datatypes.chatgpt.ImageContentPartImageUrl
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param type 
 * @param imageUrl 
 */
data class ImageContentPart(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true) val type: ImageContentPart.Type,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("image_url", required = true) val imageUrl: ImageContentPartImageUrl
) {

    /**
    * 
    * Values: text
    */
    enum class Type(val value: kotlin.String) {

        @JsonProperty("text") text("text")
    }

}

