package com.mlp.api

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.mlp.api.datatypes.chatgpt.ChatMessage
import com.mlp.api.datatypes.chatgpt.ChatRole
import com.mlp.api.datatypes.chatgpt.ContentPart
import com.mlp.api.datatypes.chatgpt.PartsChatMessage
import com.mlp.api.datatypes.chatgpt.TextChatMessage
import com.mlp.api.datatypes.chatgpt.ToolCall

object ChatMessageDeserializer : JsonDeserializer<ChatMessage>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): ChatMessage {
        val node: JsonNode = p.codec.readTree(p)
        val roleNode = node.get("role")
        val role = p.codec.readValue(roleNode.traverse(p.codec), ChatRole::class.java)
        val contentNode = node.get("content")

        val name = node.get("name")?.asText()
        val toolCallId = node.get("tool_call_id")?.asText()
        val toolCallsNode = node.get("tool_calls")
        val toolCalls = toolCallsNode?.let {
            p.codec.readValue(
                it.traverse(p.codec),
                object : TypeReference<List<ToolCall>>() {})
        }

        return if (contentNode?.isArray == true) {
            val content =
                p.codec.readValue(contentNode.traverse(p.codec), object : TypeReference<List<ContentPart>>() {})
            PartsChatMessage(role, content, toolCallId, name, toolCalls)
        } else {
            val content = contentNode?.asText()
            TextChatMessage(role, content, toolCallId, name, toolCalls)
        }
    }
}
