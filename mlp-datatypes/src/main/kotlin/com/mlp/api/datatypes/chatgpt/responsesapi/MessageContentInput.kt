package com.mlp.api.datatypes.chatgpt.responsesapi

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.mlp.api.MessageContentInputDeserializer
import com.mlp.api.MessageContentInputSerializer

@JsonSerialize(using = MessageContentInputSerializer::class)
@JsonDeserialize(using = MessageContentInputDeserializer::class)
sealed interface MessageContentInput

