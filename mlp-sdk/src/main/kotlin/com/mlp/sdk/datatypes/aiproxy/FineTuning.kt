package com.mlp.sdk.datatypes.aiproxy

import com.fasterxml.jackson.annotation.JsonProperty

data class FineTuningRequest(
    val model: String,
    val jobId: String? = null,
    val authToken: String? = null,
    val datasetId: Long? = null,
    val numberOfEpochs: Int? = null,
    val datasetAccountId: String? = null,
)

data class FineTuningResult(
    val jobId: String? = null,
    val usage: Usage? = null,
    val errorEvents: FineTuningEvents? = null,
    val tunedModelName: String? = null,
    val spentMicroCents: Long? = null
)

data class FineTuningEvents(
    val data: List<FineTuningEvent>
)

data class FineTuningEvent(
    val `object`: String? = null,
    val id: String? = null,
    @JsonProperty("created_at")
    val createdAt: Long? = null,
    val level: String? = null,
    val message: String? = null,
    val type: String? = null,
)
