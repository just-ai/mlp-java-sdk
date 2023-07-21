package com.mlp.sdk.datatypes.aiproxy

import com.fasterxml.jackson.annotation.JsonProperty


/**
 * https://platform.openai.com/docs/api-reference/embeddings/create
 */
data class EmbeddingRequest(
    @JsonProperty(defaultValue = "text-embedding-ada-002")
    val model: String,

    val input: List<String>,

    val user: String? = null
) {
    constructor(model: String, input: String, user: String?) : this(model, listOf(input), user)
}

data class EmbeddingResult(
    val model: String,

    val `object`: String? = null,

    val data: List<Embedding>? = null,

    val usage: Usage
)

data class Embedding(
    val `object`: String? = null,
    val embedding: List<Double>? = null,
    val index: Int? = null
)