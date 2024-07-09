package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.mlp.api.datatypes.chatgpt.CompletionChoice
import com.mlp.api.datatypes.chatgpt.Usage
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param created 
 * @param model 
 * @param usage 
 * @param id 
 * @param &#x60;object&#x60; 
 * @param choices 
 */
data class CompletionResult(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("created", required = true) val created: kotlin.Long,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("model", required = true) val model: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("usage", required = true) val usage: Usage,

    @Schema(example = "null", description = "")
    @get:JsonProperty("id") val id: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("object") val `object`: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("choices") val choices: kotlin.collections.List<CompletionChoice>? = null
) {

}

