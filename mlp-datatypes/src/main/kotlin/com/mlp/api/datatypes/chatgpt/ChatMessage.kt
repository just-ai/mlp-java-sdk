package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonValue
import com.mlp.api.datatypes.chatgpt.ChatRole
import com.mlp.api.datatypes.chatgpt.ContentPart
import com.mlp.api.datatypes.chatgpt.PartsChatMessage
import com.mlp.api.datatypes.chatgpt.TextChatMessage
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
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.mlp.api.ChatMessageDeserializer::class)
open class ChatMessage(

    @Schema(example = "null", description = "")
    @get:JsonProperty("role") open val role: ChatRole? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("content") open val content: kotlin.Any? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("tool_call_id") open val toolCallId: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("name") open val name: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("tool_calls") open val toolCalls: kotlin.collections.List<ToolCall>? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("thinking") open val thinking: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("reasoning") open val reasoning: kotlin.String? = null
){

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChatMessage) return false
        if (role != other.role) return false
        if (content != other.content) return false
        if (toolCallId != other.toolCallId) return false
        if (name != other.name) return false
        if (toolCalls != other.toolCalls) return false
        if (thinking != other.thinking) return false
        if (reasoning != other.reasoning) return false

        return true
    }

    override fun hashCode(): Int {

        var result = role.hashCode()

        result = 31 * result + content.hashCode()
        result = 31 * result + toolCallId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + toolCalls.hashCode()
        result = 31 * result + thinking.hashCode()
        result = 31 * result + reasoning.hashCode()
        return result
    }

    override fun toString(): String {
        return "ChatMessage(role=$role, content=$content, toolCallId=$toolCallId, name=$name, toolCalls=$toolCalls, thinking=$thinking, reasoning=$reasoning)"
    }

    fun copy(
        role: ChatRole? = this.role,
        content: kotlin.Any? = this.content,
        toolCallId: kotlin.String? = this.toolCallId,
        name: kotlin.String? = this.name,
        toolCalls: kotlin.collections.List<ToolCall>? = this.toolCalls,
        thinking: kotlin.String? = this.thinking,
        reasoning: kotlin.String? = this.reasoning
    ): ChatMessage {
        return ChatMessage(
            role,
            content,
            toolCallId,
            name,
            toolCalls,
            thinking,
            reasoning
        )
    }

}

