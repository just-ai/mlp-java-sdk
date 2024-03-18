package com.mlp.sdk.datatypes.aiproxy

data class UnifiedAiProxyRequest(
    val chat: Any? = null,
    val edit: Any? = null,
    val image: Any? = null,
    val imageEdit: Any? = null,
    val embedding: Any? = null,
    val moderation: Any? = null,
    val completion: Any? = null,
    val fineTuning: FineTuningRequest? = null,
    val audioTranslate: Any? = null,
    val imageVariation: Any? = null,
    val audioTranscribe: Any? = null,
)

data class UnifiedAiProxyResponse(
    val chat: Any? = null,
    val edit: Any? = null,
    val image: Any? = null,
    val imageEdit: Any? = null,
    val embedding: Any? = null,
    val completion: Any? = null,
    val moderation: Any? = null,
    val fineTuning: FineTuningResult? = null,
    val audioTranslate: Any? = null,
    val imageVariation: Any? = null,
    val audioTranscribe: Any? = null,
    val spentMicroCents: Long?
)

