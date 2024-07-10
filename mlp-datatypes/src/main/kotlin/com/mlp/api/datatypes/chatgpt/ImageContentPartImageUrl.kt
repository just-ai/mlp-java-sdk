package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param url 
 * @param detail 
 */
data class ImageContentPartImageUrl(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("url", required = true) val url: kotlin.String,

    @Schema(example = "null", description = "")
    @get:JsonProperty("detail") val detail: kotlin.String? = null
) {

}

