package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.mlp.api.datatypes.chatgpt.FunctionCall
import com.mlp.api.datatypes.chatgpt.ToolType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param id 
 * @param type 
 * @param function 
 */
data class ToolCall(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true) val type: ToolType,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("function", required = true) val function: FunctionCall
) {

}

