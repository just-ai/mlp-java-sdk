package com.mlp.api.datatypes.chatgpt.responsesapi

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param &#x60;value&#x60; 
 */
data class ResponsesApiRequestTextInput(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("value", required = true) val `value`: String
): ResponsesApiRequestInput {

}

