package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.mlp.api.datatypes.chatgpt.ChatMessage
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param index 
 * @param message 
 * @param delta 
 * @param finishReason 
 */
data class ChatCompletionChoice(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("index", required = true) val index: kotlin.Int,

    @Schema(example = "null", description = "")
    @get:JsonProperty("message") val message: ChatMessage? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("delta") val delta: ChatMessage? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("finish_reason") val finishReason: ChatCompletionChoice.FinishReason? = null
) {

    /**
    * 
    * Values: stop,length
    */
    enum class FinishReason(val value: kotlin.String) {

        @JsonProperty("stop") stop("stop"),
        @JsonProperty("length") length("length")
    }

}

