package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param arguments 
 * @param name 
 */
data class FunctionCall(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("arguments", required = true) val arguments: kotlin.String,

    @Schema(example = "null", description = "")
    @get:JsonProperty("name") val name: kotlin.String? = null
) {

}

