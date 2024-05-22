package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.mlp.api.datatypes.chatgpt.ChatMessage
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param messages 
 * @param model 
 * @param stream 
 */
data class ChatCompletionRequest(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("messages", required = true) val messages: kotlin.collections.List<ChatMessage>,

    @Schema(example = "null", description = "")
    @get:JsonProperty("model") val model: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("stream") val stream: kotlin.Boolean? = null
) {

}

