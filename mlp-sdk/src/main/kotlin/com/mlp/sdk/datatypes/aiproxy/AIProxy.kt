package com.mlp.sdk.datatypes.aiproxy


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

@Deprecated("Use AiProxyRequest with ChatCompletionRequest")
data class ChatConfig(
    val model: String = "gpt-3.5-turbo",
    val temperature: Double? = null,
    val top_p: Double? = null,
    val n: Int? = null,
    val stop: List<String>? = null,
    val max_tokens: Int? = null,
    val presence_penalty: Double? = null,
    val frequency_penalty: Double? = null,
    val logit_bias: Map<String, Int>? = null,
    val user: String? = null
)


@Deprecated("Use AiProxyRequest with ChatCompletionRequest")
data class ChatRequest(val messages: List<ChatMessage>)

