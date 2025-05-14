package com.mlp.api.datatypes.chatgpt

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.mlp.api.ToolChoiceDeserializer

@JsonDeserialize(using = ToolChoiceDeserializer::class)
sealed interface ToolChoice
