package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: function
*/
enum class ToolType(val value: kotlin.String) {

    @JsonProperty("function") function("function")
}

