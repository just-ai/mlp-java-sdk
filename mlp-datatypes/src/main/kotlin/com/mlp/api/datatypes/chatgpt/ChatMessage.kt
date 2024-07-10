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
 */
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.mlp.api.ChatMessageDeserializer::class)
open class ChatMessage(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("role", required = true) open val role: ChatRole,

    @Schema(example = "null", description = "")
    @get:JsonProperty("content") open val content: kotlin.Any? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("tool_call_id") open val toolCallId: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("name") open val name: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("tool_calls") open val toolCalls: kotlin.collections.List<ToolCall>? = null
){

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChatMessage) return false
        if (role != other.role) return false
        if (content != other.content) return false
        if (toolCallId != other.toolCallId) return false
        if (name != other.name) return false
        if (toolCalls != other.toolCalls) return false

        return true
    }

    override fun hashCode(): Int {
        var result = role.hashCode()
        
        result = 31 * result + content.hashCode()
        
        result = 31 * result + toolCallId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + toolCalls.hashCode()
        return result
    }

}

