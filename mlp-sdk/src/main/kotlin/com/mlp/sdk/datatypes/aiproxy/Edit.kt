package com.mlp.sdk.datatypes.aiproxy

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * https://platform.openai.com/docs/api-reference/edits/create
 */
data class EditRequest(
    @JsonProperty(defaultValue = "text-davinci-edit-001")
    val model: String,

    val input: String? = null,

    val instruction: String,

    val n: Int? = null,

    val temperature: Double? = null,

    @JsonProperty("top_p")
    val topP: Double? = null
)

data class EditResult(
    val `object`: String? = null,

    val created: Long = 0,

    val choices: List<EditChoice>? = null,

    val usage: Usage
)

data class EditChoice(
    val text: String? = null,
    val index: Int? = null
)
