package com.mlp.api.datatypes.chatgpt

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.mlp.api.ChatMessageDeserializer
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param role
 * @param content
 * @param toolCallId
 * @param toolCalls
 */
@JsonDeserialize(using = ChatMessageDeserializer::class)
interface ChatMessage {
    @get:Schema(example = "null", required = true, description = "")
    @get:JsonProperty("role", required = true)
    val role: ChatRole

    @get:Schema(example = "null", description = "")
    @get:JsonProperty("content")
    val content: Any?

    @get:Schema(example = "null", description = "")
    @get:JsonProperty("name") val name: kotlin.String?

    @get:Schema(example = "null", description = "")
    @get:JsonProperty("tool_call_id")
    val toolCallId: kotlin.String?

    @get:Schema(example = "null", description = "")
    @get:JsonProperty("tool_calls")
    val toolCalls: kotlin.collections.List<ToolCall>?
}
