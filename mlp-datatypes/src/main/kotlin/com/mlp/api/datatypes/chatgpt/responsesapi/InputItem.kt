package com.mlp.api.datatypes.chatgpt.responsesapi

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonValue
import com.mlp.api.datatypes.chatgpt.responsesapi.InputItemType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param type 
 */

// manually added defaultImpl
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true, defaultImpl = MessageInputItem::class)
@JsonSubTypes(
      JsonSubTypes.Type(value = CodeInterpreterCallInputItem::class, name = "code_interpreter_call"),
      JsonSubTypes.Type(value = ComputerResponsesApiToolCallInputItem::class, name = "computer_call"),
      JsonSubTypes.Type(value = ComputerCallOutputItemParamInputItem::class, name = "computer_call_output"),
      JsonSubTypes.Type(value = FileSearchResponsesApiToolCallInputItem::class, name = "file_search_call"),
      JsonSubTypes.Type(value = FunctionResponsesApiToolCallInputItem::class, name = "function_call"),
      JsonSubTypes.Type(value = FunctionCallOutputItemParamInputItem::class, name = "function_call_output"),
      JsonSubTypes.Type(value = ImageGenerationCallInputItem::class, name = "image_generation_call"),
      JsonSubTypes.Type(value = ItemReferenceParamInputItem::class, name = "item_reference"),
      JsonSubTypes.Type(value = LocalShellInputItem::class, name = "local_shell_call"),
      JsonSubTypes.Type(value = LocalShellOutputInputItem::class, name = "local_shell_call_output"),
      JsonSubTypes.Type(value = MessageInputItem::class, name = "message"),
      JsonSubTypes.Type(value = ReasoningItemInputItem::class, name = "reasoning"),
      JsonSubTypes.Type(value = WebSearchResponsesApiToolCallInputItem::class, name = "web_search_call")
)

interface InputItem{
            @get:Schema(example = "null", required = true   , description = "")
    val type: InputItemType

}

