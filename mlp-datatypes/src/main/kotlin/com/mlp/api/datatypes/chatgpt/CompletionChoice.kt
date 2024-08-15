package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.mlp.api.datatypes.chatgpt.LogProbResult
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param text 
 * @param index 
 * @param logprobs 
 * @param finishReason 
 */
data class CompletionChoice(

    @Schema(example = "null", description = "")
    @get:JsonProperty("text") val text: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("index") val index: kotlin.Int? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("logprobs") val logprobs: LogProbResult? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("finish_reason") val finishReason: kotlin.String? = null
) {

}

