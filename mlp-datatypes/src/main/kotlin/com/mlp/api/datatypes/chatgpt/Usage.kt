package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.mlp.api.datatypes.chatgpt.PromptTokensDetails
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param promptTokens 
 * @param completionTokens 
 * @param totalTokens 
 * @param promptTokensDetails 
 */
data class Usage(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("prompt_tokens", required = true) val promptTokens: kotlin.Int,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("completion_tokens", required = true) val completionTokens: kotlin.Int,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("total_tokens", required = true) val totalTokens: kotlin.Int,

    @Schema(example = "null", description = "")
    @get:JsonProperty("prompt_tokens_details") val promptTokensDetails: PromptTokensDetails? = null
) {

}

