package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.mlp.api.datatypes.chatgpt.EmbeddingEncodingFormat
import com.mlp.api.datatypes.chatgpt.EmbeddingRequest
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param input 
 */
data class EmbeddingRequestIntArray(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("input", required = true) override val input: kotlin.collections.List<kotlin.Int>,

    @Schema(example = "text-embedding-3-small", required = true, description = "")
    @get:JsonProperty("model", required = true) override val model: kotlin.String,

    @Schema(example = "null", description = "")
    @get:JsonProperty("encoding_format") override val encodingFormat: EmbeddingEncodingFormat? = EmbeddingEncodingFormat.float,

    @Schema(example = "null", description = "")
    @get:JsonProperty("dimensions") override val dimensions: kotlin.Int? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("user") override val user: kotlin.String? = null
) : EmbeddingRequest(model, input, encodingFormat, dimensions, user){

}

