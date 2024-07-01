package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.mlp.api.datatypes.chatgpt.ChatRole
import com.mlp.api.datatypes.chatgpt.ToolCall
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param role 
 * @param content 
 * @param toolCallId 
 * @param toolCalls 
 */
data class ChatMessage(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("role", required = true) val role: ChatRole,

    @Schema(example = "null", description = "")
    @get:JsonProperty("content") val content: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("tool_call_id") val toolCallId: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("tool_calls") val toolCalls: kotlin.collections.List<ToolCall>? = null
) {

}

