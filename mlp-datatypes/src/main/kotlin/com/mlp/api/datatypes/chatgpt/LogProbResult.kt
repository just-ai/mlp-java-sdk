package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param tokens 
 * @param tokenLogprobs 
 * @param topLogprobs 
 * @param textOffset 
 */
data class LogProbResult(

    @Schema(example = "null", description = "")
    @get:JsonProperty("tokens") val tokens: kotlin.collections.List<kotlin.String>? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("token_logprobs") val tokenLogprobs: kotlin.collections.List<kotlin.Double>? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("top_logprobs") val topLogprobs: kotlin.collections.List<kotlin.collections.Map<kotlin.String, kotlin.Double>>? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("textOffset") val textOffset: kotlin.collections.List<kotlin.Int>? = null
) {

}

