package com.mlp.api.datatypes.chatgpt.responsesapi

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.mlp.api.ResponsesApiRequestInputDeserializer
import com.mlp.api.ResponsesApiRequestInputSerializer

@JsonSerialize(using = ResponsesApiRequestInputSerializer::class)
@JsonDeserialize(using = ResponsesApiRequestInputDeserializer::class)
sealed interface ResponsesApiRequestInput

