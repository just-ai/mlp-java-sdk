package com.mlp.sdk.datatypes.asr.common

import java.time.Duration

data class AsrResponse(
    val chunks: List<SpeechRecognitionChunk> = emptyList(),
    val final: Boolean? = null,
    val providerSpecific: String = ""
)

data class SpeechRecognitionChunk(
    val alternatives: List<SpeechRecognitionAlternative> = emptyList(),
    val final: Boolean = false,
    val endOfUtterance: Boolean = false
)

data class SpeechRecognitionAlternative(
    val text: String = "",
    val confidence: Float = 0f,
    val words: List<WordInfo> = emptyList()
)

data class WordInfo(
    val startTime: Duration = Duration.ZERO,
    val endTime: Duration = Duration.ZERO,
    val word: String = "",
    val confidence: Float = 0f
)
