package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.mlp.api.datatypes.chatgpt.ChatMessage
import com.mlp.api.datatypes.chatgpt.Tool
import com.mlp.api.datatypes.chatgpt.ToolChoice
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param messages 
 * @param model 
 * @param stream 
 * @param tools 
 * @param toolChoice 
 * @param logprobs 
 * @param topLogprobs 
 */
data class ChatCompletionRequest(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("messages", required = true) val messages: kotlin.collections.List<ChatMessage>,

    @Schema(example = "null", description = "")
    @get:JsonProperty("model") val model: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("stream") val stream: kotlin.Boolean? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("tools") val tools: kotlin.collections.List<Tool>? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("tool_choice") val toolChoice: ToolChoice? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("logprobs") val logprobs: kotlin.Boolean? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("top_logprobs") val topLogprobs: kotlin.Int? = null
) {

}

