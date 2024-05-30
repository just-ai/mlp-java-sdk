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

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("role", required = true) val role: ChatRole,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("content", required = true) val content: kotlin.String
) {

}

