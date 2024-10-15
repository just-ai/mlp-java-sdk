package com.mlp.sdk.datatypes.asr

data class AsrRequestJson(
    val audio_base64: String
)

data class VoskAsrResponse(
    val alternatives: List<AsrResponse>
)

data class AsrResponse(
    val text: String,
    val confidence: Double
)
