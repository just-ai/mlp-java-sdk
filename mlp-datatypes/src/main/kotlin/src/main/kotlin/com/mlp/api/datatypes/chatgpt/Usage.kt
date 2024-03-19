package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param promptTokens 
 * @param completionTokens 
 * @param totalTokens 
 */
data class Usage(

    @Schema(example = "null", description = "")
    @get:JsonProperty("prompt_tokens") val promptTokens: kotlin.Int? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("completion_tokens") val completionTokens: kotlin.Int? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("total_tokens") val totalTokens: kotlin.Int? = null
) {

}

