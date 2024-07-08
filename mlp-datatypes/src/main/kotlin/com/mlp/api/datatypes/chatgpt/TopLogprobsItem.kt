package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param token 
 * @param logprob 
 * @param bytes 
 */
data class TopLogprobsItem(

    @Schema(example = "null", description = "")
    @get:JsonProperty("token") val token: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("logprob") val logprob: java.math.BigDecimal? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("bytes") val bytes: kotlin.collections.List<kotlin.Int>? = null
) {

}

