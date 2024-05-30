package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.mlp.api.datatypes.chatgpt.ChatCompletionChoice
import com.mlp.api.datatypes.chatgpt.Usage
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param choices 
 * @param model 
 * @param usage 
 */
data class ChatCompletionResult(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("choices", required = true) val choices: kotlin.collections.List<ChatCompletionChoice>,

    @Schema(example = "null", description = "")
    @get:JsonProperty("model") val model: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("usage") val usage: Usage? = null
) {

}

