package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.mlp.api.datatypes.chatgpt.FunctionCall
import com.mlp.api.datatypes.chatgpt.ToolType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param function 
 * @param id 
 * @param type 
 */
data class ToolCall(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("function", required = true) val function: FunctionCall,

    @Schema(example = "null", description = "")
    @get:JsonProperty("id") val id: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("type") val type: ToolType? = null
) {

}

