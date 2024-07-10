package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: text,image_url
*/
enum class ContentPartType(val value: kotlin.String) {

    @JsonProperty("text") text("text"),
    @JsonProperty("image_url") image_url("image_url")
}

