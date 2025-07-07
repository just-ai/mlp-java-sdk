package com.mlp.api.datatypes.chatgpt.responsesapi

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class CodeInterpreterAutoContainer(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true) val type: CodeInterpreterContainerType,

    @Schema(example = "null", description = "")
    @get:JsonProperty("file_ids") val fileIds: List<String>? = null
) : CodeInterpreterContainer

