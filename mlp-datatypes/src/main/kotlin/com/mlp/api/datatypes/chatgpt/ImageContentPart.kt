package com.mlp.api.datatypes.chatgpt

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.mlp.api.datatypes.chatgpt.ContentPart
import com.mlp.api.datatypes.chatgpt.ContentPartType
import com.mlp.api.datatypes.chatgpt.ImageContentPartImageUrl
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param imageUrl 
 */
data class ImageContentPart(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true) override val type: ContentPartType,

    @Schema(example = "null", description = "")
    @get:JsonProperty("image_url") val imageUrl: ImageContentPartImageUrl? = null
) : ContentPart{

}

