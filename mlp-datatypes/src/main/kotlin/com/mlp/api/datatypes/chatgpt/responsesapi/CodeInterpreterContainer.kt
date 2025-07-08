package com.mlp.api.datatypes.chatgpt.responsesapi

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.mlp.api.CodeInterpreterContainerDeserializer
import com.mlp.api.CodeInterpreterContainerSerializer

@JsonSerialize(using = CodeInterpreterContainerSerializer::class)
@JsonDeserialize(using = CodeInterpreterContainerDeserializer::class)
sealed interface CodeInterpreterContainer
