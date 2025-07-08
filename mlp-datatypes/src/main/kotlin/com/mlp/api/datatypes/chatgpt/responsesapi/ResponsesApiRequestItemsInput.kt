package com.mlp.api.datatypes.chatgpt.responsesapi

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param items 
 */
data class ResponsesApiRequestItemsInput(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("items", required = true) val items: List<InputItem>
): ResponsesApiRequestInput {

}

