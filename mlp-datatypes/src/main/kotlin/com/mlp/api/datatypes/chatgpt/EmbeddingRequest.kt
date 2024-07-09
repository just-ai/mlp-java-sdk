package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.mlp.api.datatypes.chatgpt.EmbeddingEncodingFormat
import com.mlp.api.datatypes.chatgpt.EmbeddingRequestArrayIntArray
import com.mlp.api.datatypes.chatgpt.EmbeddingRequestIntArray
import com.mlp.api.datatypes.chatgpt.EmbeddingRequestString
import com.mlp.api.datatypes.chatgpt.EmbeddingRequestStringArray
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param model 
 * @param input 
 * @param encodingFormat 
 * @param dimensions 
 * @param user 
 */@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.mlp.api.EmbeddingRequestDeserializer::class)
open class EmbeddingRequest(

    @Schema(example = "text-embedding-3-small", required = true, description = "")
    @get:JsonProperty("model", required = true) open val model: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("input", required = true) open val input: kotlin.Any,

    @Schema(example = "null", description = "")
    @get:JsonProperty("encoding_format") open val encodingFormat: EmbeddingEncodingFormat? = EmbeddingEncodingFormat.float,

    @Schema(example = "null", description = "")
    @get:JsonProperty("dimensions") open val dimensions: kotlin.Int? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("user") open val user: kotlin.String? = null
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EmbeddingRequest) return false
        if (model != other.model) return false
        if (input != other.input) return false
        if (encodingFormat != other.encodingFormat) return false
        if (dimensions != other.dimensions) return false
        if (user != other.user) return false

        return true
    }

    override fun hashCode(): Int {
        var result = model.hashCode()
        
        result = 31 * result + input.hashCode()
        result = 31 * result + encodingFormat.hashCode()
        
        result = 31 * result + dimensions.hashCode()
        result = 31 * result + user.hashCode()
        return result
    }

    override fun toString(): String {
        return "EmbeddingRequest(model=$model, input=$input, encodingFormat=$encodingFormat, dimensions=$dimensions, user=$user)"
    }

    fun copy(
        model: kotlin.String = this.model,
        input: kotlin.Any = this.input,
        encodingFormat: EmbeddingEncodingFormat? = this.encodingFormat,
        dimensions: kotlin.Int? = this.dimensions,
        user: kotlin.String? = this.user
    ): EmbeddingRequest {
        return EmbeddingRequest(
            model,
            input,
            encodingFormat,
            dimensions,
            user
        )
    }

}

