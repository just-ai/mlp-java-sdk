package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.mlp.api.datatypes.chatgpt.LogprobContentItem
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param content 
 */
data class Logprobs(

    @Schema(example = "null", description = "")
    @get:JsonProperty("content") val content: kotlin.collections.List<LogprobContentItem>? = null
) {

}

