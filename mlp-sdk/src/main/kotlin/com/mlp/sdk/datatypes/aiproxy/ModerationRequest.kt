package com.mlp.sdk.datatypes.aiproxy

import com.fasterxml.jackson.annotation.JsonProperty

data class ModerationRequest(
    val input: String,

    @JsonProperty(required = false, defaultValue = "text-moderation-latest")
    val model: String = "text-moderation-latest"
)

data class ModerationResult(
    val id: String? = null,

    val model: String,

    val results: List<Moderation>
)


data class Moderation(
    val flagged: Boolean = false,

    val categories: ModerationCategories? = null,

    @JsonProperty("category_scores")
    val categoryScores: ModerationCategoryScores? = null
)

data class ModerationCategories(
    val hate: Boolean = false,

    @JsonProperty("hate/threatening")
    val hateThreatening: Boolean = false,

    @JsonProperty("self-harm")
    val selfHarm: Boolean = false,

    val sexual: Boolean = false,

    @JsonProperty("sexual/minors")
    val sexualMinors: Boolean = false,

    val violence: Boolean = false,

    @JsonProperty("violence/graphic")
    val violenceGraphic: Boolean = false
)

data class ModerationCategoryScores(
    val hate: Double = 0.0,

    @JsonProperty("hate/threatening")
    val hateThreatening: Double = 0.0,

    @JsonProperty("self-harm")
    val selfHarm: Double = 0.0,

    val sexual: Double = 0.0,

    @JsonProperty("sexual/minors")
    val sexualMinors: Double = 0.0,

    val violence: Double = 0.0,

    @JsonProperty("violence/graphic")
    val violenceGraphic: Double = 0.0
)
