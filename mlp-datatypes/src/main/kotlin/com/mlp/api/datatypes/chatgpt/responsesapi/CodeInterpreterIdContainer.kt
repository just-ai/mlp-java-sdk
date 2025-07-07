package com.mlp.api.datatypes.chatgpt.responsesapi

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class CodeInterpreterIdContainer(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("value", required = true) val `value`: String
) : CodeInterpreterContainer

