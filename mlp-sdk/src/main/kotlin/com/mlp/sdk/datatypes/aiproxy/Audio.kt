package com.mlp.sdk.datatypes.aiproxy

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.web.multipart.MultipartFile


/**
 * https://platform.openai.com/docs/api-reference/audio/create-transcription
 */
data class CreateTranscriptionRequest(
    @JsonProperty(defaultValue = "whisper-1")
    val model: String = "whisper-1",

    val file: MultipartFile,

    val prompt: String? = null,

    @JsonProperty("response_format")
    val responseFormat: String? = null,

    val temperature: Int? = null,

    val language: String? = null,
)

/**
 * https://platform.openai.com/docs/api-reference/audio/create-translation
 */
data class CreateTranslationRequest(
    @JsonProperty(defaultValue = "whisper-1")
    val model: String = "whisper-1",

    val file: MultipartFile,

    val prompt: String? = null,

    @JsonProperty("response_format")
    val responseFormat: String? = null,

    val temperature: Int? = null
)


data class AudioSubtitlesResult(
    val text: String,
)
