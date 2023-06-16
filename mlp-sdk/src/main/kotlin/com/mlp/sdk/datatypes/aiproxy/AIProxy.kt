package com.mlp.sdk.datatypes.aiproxy

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty


data class ChatConfig(
//    val model: String = "gpt-3.5-turbo",
//    val chatId: String? = null,
    val temperature: Double? = null,
    val top_p: Double? = null,
    val n: Int? = null,
    val stop: List<String>? = null,
    val max_tokens: Int? = null,
    val presence_penalty: Double? = null,
    val frequency_penalty: Double? = null,
    val logit_bias: Map<String, Int>? = null,
    val user: String? = null
)

data class ChatMessage(val role: String, val content: String)

data class ChatRequest(val messages: List<ChatMessage>)

data class Usage(
    @JsonProperty("prompt_tokens") val promptTokens: Long,
    @JsonProperty("completion_tokens") val completionTokens: Long,
    @JsonProperty("total_tokens") val totalTokens: Long
)

data class ChatCompletionChoice(
    val index: Int,
    @JsonAlias("delta") val message: ChatMessage,
    @JsonProperty("finish_reason") val finishReason: String
)

data class ChatCompletionResult(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<ChatCompletionChoice>,
    val usage: Usage
)
