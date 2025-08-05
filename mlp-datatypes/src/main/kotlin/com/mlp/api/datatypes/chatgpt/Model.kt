package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.mlp.api.datatypes.chatgpt.ModelRequestType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param id 
 * @param created 
 * @param ownedBy 
 * @param supportedRequestTypes 
 */
data class Model(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("created", required = true) val created: kotlin.Int,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("owned_by", required = true) val ownedBy: kotlin.String,

    @Schema(example = "null", description = "")
    @get:JsonProperty("supported_request_types") val supportedRequestTypes: kotlin.collections.List<ModelRequestType>? = null
) {

}

