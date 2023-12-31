package com.mlp.sdk.datatypes.chatgpt

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * This data class represents all possible config options for chat-gpt request.
 * This object could be posted as predict config and could be used in predict method to simplify requests.
 * Meaning of each field is identical to ChatCompletionRequest.
 * Values in ChatCompletionRequest have higher priority.
 */
data class ChatCompletionConfig(
    val model: String? = null,
    val temperature: Double? = null,
    @JsonProperty("top_p")
    val topP: Double? = null,
    val n: Int? = null,
    val stop: List<String>? = null,
    @JsonProperty("max_tokens")
    val maxTokens: Int? = null,
    @JsonProperty("presence_penalty")
    val presencePenalty: Double? = null,
    @JsonProperty("frequency_penalty")
    val frequencyPenalty: Double? = null,
    @JsonProperty("logit_bias")
    val logitBias: Map<String, Int>? = null,
    val user: String? = null,

    /**
     * System prompt could be specified in config. It will be placed on the first place in messages array.
     * If messages already has message with role=system then this value will be skipped
     */
    @JsonProperty("system_prompt")
    val systemPrompt: String? = null
)

/**
 * Constants for 'role' field
 */
enum class ChatCompletionRole {
    system,
    user,
    assistant,
}

/**
 * Simplified form of ChatGPT request without any additional options
 */
data class ChatCompletionSimpleRequest(
    val messages: List<ChatMessage>,
)

data class ChatCompletionRequest(
    /**
     * ID of the model to use. See the model endpoint compatibility table for details on which models work with the Chat API.
     */
    val model: String? = null,

    /**
     * A list of messages comprising the conversation so far
     */
    val messages: List<ChatMessage>,

    /**
     * What sampling temperature to use, between 0 and 2.
     * Higher values like 0.8 will make the output more random, while lower values like 0.2 will make it more focused and deterministic.
     * We generally recommend altering this or top_p but not both.
     */
    val temperature: Double? = null,

    /**
     * An alternative to sampling with temperature, called nucleus sampling, where the model considers the results of the tokens with top_p probability mass.
     * So 0.1 means only the tokens comprising the top 10% probability mass are considered.
     * We generally recommend altering this or temperature but not both.
     */
    @JsonProperty("top_p")
    val topP: Double? = null,

    /**
     * How many chat completion choices to generate for each input message.
     */
    val n: Int? = null,

    /**
     * If set, partial message deltas will be sent, like in ChatGPT. Tokens will be sent as data-only server-sent events as they become available, with the stream terminated by a data: [DONE] message. Example Python code.
     */
    val stream: Boolean? = null,

    /**
     * Up to 4 sequences where the API will stop generating further tokens.
     */
    val stop: List<String>? = null,

    /**
     * The maximum number of tokens to generate in the chat completion.
     * The total length of input tokens and generated tokens is limited by the model's context length.
     */
    @JsonProperty("max_tokens")
    val maxTokens: Int? = null,

    /**
     * Number between -2.0 and 2.0. Positive values penalize new tokens based on whether they appear in the text so far, increasing the model's likelihood to talk about new topics.
     */
    @JsonProperty("presence_penalty")
    val presencePenalty: Double? = null,

    /**
     * Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing frequency in the text so far, decreasing the model's likelihood to repeat the same line verbatim.
     */
    @JsonProperty("frequency_penalty")
    val frequencyPenalty: Double? = null,

    /**
     * Modify the likelihood of specified tokens appearing in the completion.
     * Accepts a json object that maps tokens (specified by their token ID in the tokenizer) to an associated bias value from -100 to 100.
     * Mathematically, the bias is added to the logits generated by the model prior to sampling.
     * The exact effect will vary per model, but values between -1 and 1 should decrease or increase likelihood of selection; values like -100 or 100 should result in a ban or exclusive selection of the relevant token.
     */
    @JsonProperty("logit_bias")
    val logitBias: Map<String, Int>? = null,

    /**
     * A unique identifier representing your end-user, which can help OpenAI to monitor and detect abuse. Learn more.
     */
    val user: String? = null,
)


data class ChatCompletionResult(
    /**
     * A unique identifier for the chat completion.
     */
    val id: String? = null,

    /**
     * The object type, which is always chat.completion.
     */
    val `object`: String? = null,

    /**
     * The Unix timestamp (in seconds) of when the chat completion was created.
     */
    val created: Long = 0,

    /**
     * The model used for the chat completion.
     */
    val model: String,

    /**
     * A list of chat completion choices. Can be more than one if n is greater than 1.
     */
    val choices: List<ChatCompletionChoice>,

    /**
     * Usage statistics for the completion request.
     */
    val usage: Usage? = null
)

data class Usage(
    /**
     * Number of tokens in the prompt.
     */
    @JsonProperty("prompt_tokens") val promptTokens: Long,
    /**
     * Number of tokens in the generated completion.
     */
    @JsonProperty("completion_tokens") val completionTokens: Long,
    /**
     * Total number of tokens used in the request (prompt + completion).
     */
    @JsonProperty("total_tokens") val totalTokens: Long
)

data class ChatMessage(
    /**
     * The role of the author of this message.
     */
    val role: ChatCompletionRole,
    /**
     * The contents of the message.
     */
    val content: String
)

enum class ChatCompletionChoiceFinishReason {
    stop,
    length
}

data class ChatCompletionChoice(
    /**
     * The index of the choice in the list of choices.
     */
    val index: Int,
    /**
     * A chat completion message generated by the model.
     */
    val message: ChatMessage,
    /**
     * The reason the model stopped generating tokens.
     * This will be stop if the model hit a natural stop point or a provided stop sequence, length if the maximum number of tokens specified in the request was reached, or function_call if the model called a function.
     */
    @JsonProperty("finish_reason") val finishReason: ChatCompletionChoiceFinishReason? = null
)

data class ChatCompletionChunk(
    /**
     * A unique identifier for the chat completion chunk.
     */
    val id: String? = null,

    /**
     * The object type, which is always chat.completion.chunk.
     */
    val `object`: String? = null,

    /**
     * The Unix timestamp (in seconds) of when the chat completion chunk was created.
     */
    val created: Long = 0,

    /**
     * The model used for the chat completion.
     */
    val model: String,

    /**
     * A list of chat completion choices. Can be more than one if n is greater than 1.
     */
    val choices: List<ChatCompletionChoice>,
)

data class ChatCompletionChunkChoice(
    /**
     * The index of the choice in the list of choices.
     */
    val index: Int,
    /**
     * A chat completion delta generated by streamed model responses.
     */
    val delta: ChatMessage,
    /**
     * The reason the model stopped generating tokens.
     * This will be stop if the model hit a natural stop point or a provided stop sequence, length if the maximum number of tokens specified in the request was reached, or function_call if the model called a function.
     */
    @JsonProperty("finish_reason") val finishReason: String
)

