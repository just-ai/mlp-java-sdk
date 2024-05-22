package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param model 
 * @param stream 
 * @param maxTokens 
 * @param temperature 
 */
data class ChatCompletionConfig(

    @Schema(example = "null", description = "")
    @get:JsonProperty("model") val model: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("stream") val stream: kotlin.Boolean? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("max_tokens") val maxTokens: java.math.BigDecimal? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("temperature") val temperature: kotlin.Double? = null
) {

}

