package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: chat_completions,responses,realtime,assistants,batch_processing,fine_tuning,embeddings,image_generation,image_edit,speech_generation,transcription,speech_to_text,moderations,completions
*/
enum class ModelRequestType(val value: kotlin.String) {

    @JsonProperty("chat_completions") chat_completions("chat_completions"),
    @JsonProperty("responses") responses("responses"),
    @JsonProperty("realtime") realtime("realtime"),
    @JsonProperty("assistants") assistants("assistants"),
    @JsonProperty("batch_processing") batch_processing("batch_processing"),
    @JsonProperty("fine_tuning") fine_tuning("fine_tuning"),
    @JsonProperty("embeddings") embeddings("embeddings"),
    @JsonProperty("image_generation") image_generation("image_generation"),
    @JsonProperty("image_edit") image_edit("image_edit"),
    @JsonProperty("speech_generation") speech_generation("speech_generation"),
    @JsonProperty("transcription") transcription("transcription"),
    @JsonProperty("speech_to_text") speech_to_text("speech_to_text"),
    @JsonProperty("moderations") moderations("moderations"),
    @JsonProperty("completions") completions("completions")
}

