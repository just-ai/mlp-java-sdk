package com.mlp.sdk.datatypes.aiproxy

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.web.multipart.MultipartFile
import java.time.Duration


/**
 * https://platform.openai.com/docs/api-reference/audio/create-transcription
 */
data class CreateTranscriptionRequest(
    @JsonProperty(defaultValue = "whisper-1")
    val model: String = "whisper-1",

    val file: ByteArray,

    val prompt: String? = null,

    @JsonProperty("response_format")
    val responseFormat: String? = null,

    val temperature: Double? = null,

    val language: String? = null,
)

/**
 * https://platform.openai.com/docs/api-reference/audio/create-translation
 */
data class CreateTranslationRequest(
    @JsonProperty(defaultValue = "whisper-1")
    val model: String = "whisper-1",

    val file: ByteArray,

    val prompt: String? = null,

    @JsonProperty("response_format")
    val responseFormat: String? = null,

    val temperature: Double? = null
)

data class AudioSubtitlesResult(
    val text: String,
    val task: String? = null,
    val language: String? = null,
    val duration: Duration? = null,
    val segments: List<AudioSegment>? = null
)

data class AudioSegment(
    val id: Long? = null,
    val seek: Long? = null,
    val start: Double? = null,
    val end: Double? = null,
    val text: String? = null,
    val tokens: List<Int>? = null,
    val temperature: Double? = null,
    @JsonProperty("avg_logprob")
    val avgLogProb: Double? = null,
    @JsonProperty("compression_ratio")
    val compressionRatio: Double? = null,
    @JsonProperty("no_speech_prob")
    val noSpeechProb: Double? = null,
)
