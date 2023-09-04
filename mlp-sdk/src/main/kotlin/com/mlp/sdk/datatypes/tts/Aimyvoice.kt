package com.mlp.sdk.datatypes.tts

data class TtsRequest(
    val text: String,
    val voice: String?,
    val outputAudioSpec: AudioFormatOptions?
) {
    data class AudioFormatOptions(
        val audioEncoding: AudioEncoding?,
        val sampleRateHertz: Int?,
        val chunkSizeKb: Int?
    ) {
        enum class AudioEncoding {
            LINEAR16_PCM
        }
    }
}

data class TtsConfig(
    val encodeBase64: Boolean = true
)

data class TtsResponse(
    val audio_base64: String
)