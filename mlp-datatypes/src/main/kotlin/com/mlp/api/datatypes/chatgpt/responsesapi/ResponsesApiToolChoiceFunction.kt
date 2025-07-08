package com.mlp.api.datatypes.chatgpt.responsesapi

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param type 
 * @param name 
 */
data class ResponsesApiToolChoiceFunction(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true) val type: String,

    @Schema(example = "null", description = "")
    @get:JsonProperty("name") val name: String? = null
): ResponsesApiToolChoice {

}

