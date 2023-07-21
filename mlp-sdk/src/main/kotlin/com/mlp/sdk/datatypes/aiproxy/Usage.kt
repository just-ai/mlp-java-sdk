package com.mlp.sdk.datatypes.aiproxy

import com.fasterxml.jackson.annotation.JsonProperty

data class Usage(
    @JsonProperty("prompt_tokens") val promptTokens: Long,
    @JsonProperty("completion_tokens") val completionTokens: Long,
    @JsonProperty("total_tokens") val totalTokens: Long
)