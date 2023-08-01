package com.mlp.sdk.datatypes.aiproxy

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.web.multipart.MultipartFile


/**
 * https://platform.openai.com/docs/api-reference/images/create
 */
data class CreateImageRequest(
    val prompt: String,

    val n: Int? = null,

    val size: String? = null,

    @JsonProperty("response_format")
    val responseFormat: String? = null,

    val user: String? = null
)

/**
 * https://platform.openai.com/docs/api-reference/images/create-edit
 */
data class CreateImageEditRequest(
    val image: MultipartFile,

    val mask: MultipartFile?,

    val prompt: String,

    val n: Int? = null,

    val size: String? = null,

    @JsonProperty("response_format")
    val responseFormat: String? = null,

    val user: String? = null
)

/**
 * https://platform.openai.com/docs/api-reference/images/create-variation
 */
data class CreateImageVariationRequest(
    val image: MultipartFile,

    val n: Int? = null,

    val size: String? = null,

    @JsonProperty("response_format")
    val responseFormat: String? = null,

    val user: String? = null
)

data class ImageResult(
    val created: Long? = null,

    val data: List<Image>? = null
)

data class Image(
    val url: String? = null,
    @JsonProperty("b64_json")
    val b64Json: String? = null
)

