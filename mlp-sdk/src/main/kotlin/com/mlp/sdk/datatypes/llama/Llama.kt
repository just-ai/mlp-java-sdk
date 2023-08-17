package com.mlp.sdk.datatypes.llama

data class LlamaConfig(
    val temperature: Float = 0.6F,
    val top_p: Float = 0.9F,
    val max_gen_len: Int? = null,
)

data class LlamaTextRequest(
    val text: String,
)

data class LlamaTextResponse(
    val value: String,
)

enum class LlamaChatRole {
    system,
    user,
    assistant
}

data class LlamaMessage(
    val role: LlamaChatRole,
    val content: String
)

data class LlamaChatRequest(
    val dialog: List<LlamaMessage>
)

data class LlamaChatResponse(
    val message: LlamaMessage
)
