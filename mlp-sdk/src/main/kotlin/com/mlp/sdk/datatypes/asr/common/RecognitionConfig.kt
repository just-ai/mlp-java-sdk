package com.mlp.sdk.datatypes.asr.common

data class RecognitionConfig(
    val audioEncoding: AudioEncoding = AudioEncoding.AUDIO_ENCODING_UNSPECIFIED,
    val sampleRateHertz: Long = 0,
    val languageCode: String = "",
    val enableProfanityFilter: Boolean = false,
    val model: String = "",
    val enablePartialResults: Boolean = false,
    val enableSingleUtterance: Boolean = false,
    val audioChannelCount: Long = 0,
    val enableRawResults: Boolean = false,
    val enableLiteratureText: Boolean = false,
    val enableAutomaticPunctuation: Boolean = false,
    val providerSpecific: String = ""
) {
    enum class AudioEncoding {
        AUDIO_ENCODING_UNSPECIFIED,
        LINEAR16_PCM,
        OGG_OPUS,
        MP3,
        MULAW,
        ALAW,
        FLAC
    }
}
