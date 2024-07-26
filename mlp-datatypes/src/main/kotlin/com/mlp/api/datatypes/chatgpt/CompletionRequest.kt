package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param model 
 * @param prompt 
 * @param suffix 
 * @param maxTokens 
 * @param temperature 
 * @param topP 
 * @param n 
 * @param stream 
 * @param logprobs 
 * @param echo 
 * @param stop 
 * @param presencePenalty 
 * @param frequencyPenalty 
 * @param bestOf 
 * @param logitBias 
 * @param user 
 */
data class CompletionRequest(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("model", required = true) val model: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("prompt", required = true) val prompt: kotlin.String,

    @Schema(example = "null", description = "")
    @get:JsonProperty("suffix") val suffix: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("max_tokens") val maxTokens: kotlin.Int? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("temperature") val temperature: kotlin.Double? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("top_p") val topP: kotlin.Double? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("n") val n: kotlin.Int? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("stream") val stream: kotlin.Boolean? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("logprobs") val logprobs: kotlin.Int? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("echo") val echo: kotlin.Boolean? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("stop") val stop: kotlin.collections.List<kotlin.String>? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("presence_penalty") val presencePenalty: kotlin.Double? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("frequency_penalty") val frequencyPenalty: kotlin.Double? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("best_of") val bestOf: kotlin.Int? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("logit_bias") val logitBias: kotlin.collections.Map<kotlin.String, kotlin.Int>? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("user") val user: kotlin.String? = null
) {

}

