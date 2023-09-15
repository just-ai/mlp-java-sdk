package com.mlp.sdk.datatypes.aiproxy

import com.mlp.sdk.datatypes.chatgpt.ChatCompletionRequest
import com.mlp.sdk.datatypes.chatgpt.ChatCompletionResult


data class AiProxyRequest(
    val chat: ChatCompletionRequest? = null,
    val completion: CompletionRequest? = null,
    val edit: EditRequest? = null,
    val embedding: EmbeddingRequest? = null,
    val moderation: ModerationRequest? = null,
    val audioTranscribe: CreateTranscriptionRequest? = null,
    val audioTranslate: CreateTranslationRequest? = null,
    val image: CreateImageRequest? = null,
    val imageEdit: CreateImageEditRequest? = null,
    val imageVariation: CreateImageVariationRequest? = null
)

data class AiProxyResponse(
    val chat: ChatCompletionResult? = null,
    val completion: CompletionResult? = null,
    val edit: EditResult? = null,
    val embedding: EmbeddingResult? = null,
    val moderation: ModerationResult? = null,
    val audioTranscribe: SttResult? = null,
    val audioTranslate: SttResult? = null,
    val image: ImageResult? = null,
    val imageEdit: ImageResult? = null,
    val imageVariation: ImageResult? = null
)

