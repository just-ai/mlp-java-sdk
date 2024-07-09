package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param index 
 * @param embedding 
 */
data class Embedding(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("index", required = true) val index: kotlin.Int,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("embedding", required = true) val embedding: kotlin.collections.List<java.math.BigDecimal>
) {

}

