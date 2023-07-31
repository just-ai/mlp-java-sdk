package com.mlp.sdk.datatypes.aiproxy

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * https://platform.openai.com/docs/api-reference/chat/create
 */
data class ChatCompletionRequest(
    val model: String,

    var messages: List<ChatMessage>,

    val temperature: Double? = null,

    @JsonProperty("top_p")
    val topP: Double? = null,

    val n: Int? = null,

    val stream: Boolean? = null,

    val stop: List<String>? = null,

    @JsonProperty("max_tokens")
    val maxTokens: Int? = null,

    @JsonProperty("presence_penalty")
    val presencePenalty: Double? = null,

    @JsonProperty("frequency_penalty")
    val frequencyPenalty: Double? = null,

    @JsonProperty("logit_bias")
    val logitBias: Map<String, Int>? = null,

    val user: String? = null,
)

data class ChatCompletionResult(
    val id: String? = null,

    val `object`: String? = null,

    val created: Long = 0,

    val model: String,

    val choices: List<ChatCompletionChoice>,

    val usage: Usage
)

data class ChatMessage(val role: String, val content: String)

data class ChatCompletionChoice(
    val index: Int,
    @JsonAlias("delta") val message: ChatMessage,
    @JsonProperty("finish_reason") val finishReason: String
)
