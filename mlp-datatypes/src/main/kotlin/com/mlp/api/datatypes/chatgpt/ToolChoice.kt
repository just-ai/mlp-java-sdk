package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: none,auto,required
*/
enum class ToolChoice(val value: kotlin.String) {

    @JsonProperty("none") none("none"),
    @JsonProperty("auto") auto("auto"),
    @JsonProperty("required") required("required")
}

