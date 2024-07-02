package com.mlp.api.datatypes.chatgpt

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
    @get:Schema(example = "null")
    val role: ChatRole

    @get:Schema(example = "null", description = "")
    val content: Any?

    @get:Schema(example = "null", description = "")
    val toolCallId: kotlin.String?

    @get:Schema(example = "null", description = "")
    val toolCalls: kotlin.collections.List<ToolCall>?


}

