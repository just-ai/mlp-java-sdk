package com.mlp.api.datatypes.chatgpt.responsesapi

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.mlp.api.ResponsesApiToolChoiceDeserializer

@JsonDeserialize(using = ResponsesApiToolChoiceDeserializer::class)
sealed interface ResponsesApiToolChoice
