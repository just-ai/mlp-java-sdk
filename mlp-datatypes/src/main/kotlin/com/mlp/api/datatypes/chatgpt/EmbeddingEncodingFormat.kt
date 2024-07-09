package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: float,base64
*/
enum class EmbeddingEncodingFormat(val value: kotlin.String) {

    @JsonProperty("float") float("float"),
    @JsonProperty("base64") base64("base64")
}

