package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: system,user,assistant,marker,tool
*/
enum class ChatRole(val value: kotlin.String) {

    @JsonProperty("system") system("system"),
    @JsonProperty("user") user("user"),
    @JsonProperty("assistant") assistant("assistant"),
    @JsonProperty("marker") marker("marker"),
    @JsonProperty("tool") tool("tool")
}

