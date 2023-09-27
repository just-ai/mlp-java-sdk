package com.mlp.sdk.datatypes.chat

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

data class ChatRequest(
    val clientId: String,
    val input: String
)

enum class ReplyType {
    text,
    buttons,
    image,
    audio
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,  include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(value = [
    JsonSubTypes.Type(value = TextReply::class, name = "text"),
    JsonSubTypes.Type(value = ButtonsReply::class, name = "buttons"),
    JsonSubTypes.Type(value = ImageReply::class, name = "image"),
    JsonSubTypes.Type(value = AudioReply::class, name = "audio"),
])
abstract class ChatReply(
    val type: ReplyType
)
data class TextReply(val text: String): ChatReply(ReplyType.text)
data class ButtonsReply(val buttons: List<String>): ChatReply(ReplyType.buttons)
data class ImageReply(val imageUrl: String): ChatReply(ReplyType.image)
data class AudioReply(val audioUrl: String): ChatReply(ReplyType.audio)

data class ChatResponse(val replies: List<ChatReply>)
