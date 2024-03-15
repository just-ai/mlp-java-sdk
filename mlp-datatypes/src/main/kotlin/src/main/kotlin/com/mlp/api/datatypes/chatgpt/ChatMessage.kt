package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.mlp.api.datatypes.chatgpt.ChatRole
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param role 
 * @param content 
 */
data class ChatMessage(

    @Schema(example = "null", description = "")
    @get:JsonProperty("role") val role: ChatRole? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("content") val content: kotlin.String? = null
) {

}

