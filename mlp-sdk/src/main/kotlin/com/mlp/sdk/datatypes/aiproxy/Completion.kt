package com.mlp.sdk.datatypes.aiproxy

import com.fasterxml.jackson.annotation.JsonProperty


/**
 * https://platform.openai.com/docs/api-reference/completions/create
 */
data class CompletionRequest(
    @JsonProperty(defaultValue = "gpt-3.5-turbo")
    val model: String,

    val prompt: String,

    val suffix: String? = null,

    @JsonProperty("max_tokens")
    val maxTokens: Int? = null,

    val temperature: Double? = null,

    @JsonProperty("top_p")
    val topP: Double? = null,

    val n: Int? = null,

    val stream: Boolean? = null,

    val logprobs: Int? = null,

    val echo: Boolean? = null,

    val stop: List<String>? = null,

    @JsonProperty("presence_penalty")
    val presencePenalty: Double? = null,

    @JsonProperty("frequency_penalty")
    val frequencyPenalty: Double? = null,

    @JsonProperty("best_of")
    val bestOf: Int? = null,

    @JsonProperty("logit_bias")
    val logitBias: Map<String, Int>? = null,

    val user: String? = null
)

data class CompletionResult(
    val id: String? = null,

    val `object`: String? = null,

    val created: Long = 0,

    val model: String,

    val choices: List<CompletionChoice>? = null,

    val usage: Usage
)

data class CompletionChoice(
    val text: String? = null,
    val index: Int? = null,
    val logprobs: LogProbResult? = null,
    val finish_reason: String? = null
)

class LogProbResult(
    val tokens: List<String>? = null,
    @JsonProperty("token_logprobs")
    val tokenLogprobs: List<Double>? = null,
    @JsonProperty("top_logprobs")
    val topLogprobs: List<Map<String, Double>>? = null,
    val textOffset: List<Int>? = null
)