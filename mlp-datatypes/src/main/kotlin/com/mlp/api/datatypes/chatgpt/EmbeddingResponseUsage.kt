package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param promptTokens 
 * @param totalTokens 
 */
data class EmbeddingResponseUsage(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("prompt_tokens", required = true) val promptTokens: kotlin.Int,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("total_tokens", required = true) val totalTokens: kotlin.Int
) {

}

