package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.mlp.api.datatypes.chatgpt.Embedding
import com.mlp.api.datatypes.chatgpt.EmbeddingResponseUsage
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param &#x60;data&#x60; 
 * @param model 
 * @param &#x60;object&#x60; 
 * @param usage 
 */
data class EmbeddingResponse(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("data", required = true) val `data`: kotlin.collections.List<Embedding>,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("model", required = true) val model: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("object", required = true) val `object`: EmbeddingResponse.`Object`,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("usage", required = true) val usage: EmbeddingResponseUsage
) {

    /**
    * 
    * Values: list
    */
    enum class `Object`(val value: kotlin.String) {

        @JsonProperty("list") list("list")
    }

}

