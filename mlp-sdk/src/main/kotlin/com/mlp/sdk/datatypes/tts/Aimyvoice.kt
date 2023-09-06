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
    val voice: String? = null,
    val outputAudioSpec: TtsRequest.AudioFormatOptions? = null,
    val encodeBase64: Boolean = true
)

data class TtsResponse(
    val audio_base64: String
)

data class TtsDictionary(
    val dictionary: List<TtsDictionaryEntry>
)

data class TtsDictionaryEntry(val original: String, val replacement: String)
