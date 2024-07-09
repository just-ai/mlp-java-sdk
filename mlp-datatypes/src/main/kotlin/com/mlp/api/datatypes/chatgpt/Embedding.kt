package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param index 
 * @param embedding 
 * @param &#x60;object&#x60; 
 */
data class Embedding(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("index", required = true) val index: kotlin.Int,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("embedding", required = true) val embedding: kotlin.collections.List<java.math.BigDecimal>,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("object", required = true) val `object`: Embedding.`Object` = `Object`.embedding
) {

    /**
    * 
    * Values: embedding
    */
    enum class `Object`(val value: kotlin.String) {

        @JsonProperty("embedding") embedding("embedding")
    }

}

