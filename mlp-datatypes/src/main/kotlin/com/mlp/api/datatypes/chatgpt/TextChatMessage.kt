package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.mlp.api.datatypes.chatgpt.ChatMessage
import com.mlp.api.datatypes.chatgpt.ChatRole
import com.mlp.api.datatypes.chatgpt.ToolCall
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param role
 * @param content
 * @param toolCallId
 * @param name
 * @param toolCalls
 * @param thinking
 * @param reasoning
 */
data class TextChatMessage(

    @Schema(example = "null", description = "")
    @get:JsonProperty("role") override val role: ChatRole? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("content") override val content: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("tool_call_id") override val toolCallId: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("name") override val name: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("tool_calls") override val toolCalls: kotlin.collections.List<ToolCall>? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("thinking") override val thinking: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("reasoning") override val reasoning: kotlin.String? = null
) : ChatMessage(role, content, toolCallId, name, toolCalls, thinking, reasoning){

}

