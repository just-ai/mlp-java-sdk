package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.mlp.api.datatypes.chatgpt.ChatMessage
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param model 
 * @param messages 
 * @param stream 
 */
data class ChatCompletionRequest(

    @Schema(example = "null", description = "")
    @get:JsonProperty("model") val model: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("messages") val messages: kotlin.collections.List<ChatMessage>? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("stream") val stream: kotlin.Boolean? = null
) {

}

