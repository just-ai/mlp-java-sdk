package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param cachedTokens 
 * @param audioTokens 
 */
data class PromptTokensDetails(

    @Schema(example = "null", description = "")
    @get:JsonProperty("cached_tokens") val cachedTokens: kotlin.Int? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("audio_tokens") val audioTokens: kotlin.Int? = null
) {

}

