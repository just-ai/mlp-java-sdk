package com.mlp.api.datatypes.chatgpt

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param role
 * @param content
 * @param toolCallId
 * @param toolCalls
 */
data class PartsChatMessageContent(
    @Schema(example = "null", description = "")
    @get:JsonProperty("role") override val role: ChatRole,

    @Schema(example = "null", description = "")
    @get:JsonProperty("content") override val content: kotlin.collections.List<ContentPart>? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("name") override val name: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("tool_call_id") override val toolCallId: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("tool_calls") override val toolCalls: kotlin.collections.List<ToolCall>? = null
) : ChatMessage
