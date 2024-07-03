package com.mlp.api.datatypes.chatgpt

import com.mlp.sdk.utils.JSON
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class JsonTests {
    @Test
    fun `should deserialize simple chatgpt request`() {
        val body = """
        {
            "model": "gpt-4o",
            "messages": [
              {
                "role": "system",
                "content": "You are a helpful assistant."
              },
              {
                "role": "user",
                "content": "Hello!"
              }
            ]
          }
        """.trimIndent()
        lateinit var chatCompletionRequest: ChatCompletionRequest
        assertDoesNotThrow {
            chatCompletionRequest = JSON.parse<ChatCompletionRequest>(body)
        }
        chatCompletionRequest.messages.forEach { message ->
            assertTrue(message is TextChatMessage)
        }
    }


    @Test
    fun `should deserialize function chatgpt request`() {
        val body = """
        {
          "model": "gpt-4-turbo",
          "messages": [
            {
              "role": "user",
              "content": "What's the weather like in Boston today?"
            },
            {
              "tool_call_id": "123456",
              "role": "tool",
              "name": "get_current_time",
              "content": "time_response"
            },
            {
              "role": "assistant",
              "tool_calls": [
                {
                  "id": "call_abc123",
                  "type": "function",
                  "function": {
                    "name": "get_current_weather",
                    "arguments": "{\n\"location\": \"Boston, MA\"\n}"
                  }
                }
              ]
            }
          ],
          "tools": [
            {
              "type": "function",
              "function": {
                "name": "get_current_weather",
                "description": "Get the current weather in a given location",
                "parameters": {
                  "type": "object",
                  "properties": {
                    "location": {
                      "type": "string",
                      "description": "The city and state, e.g. San Francisco, CA"
                    },
                    "unit": {
                      "type": "string",
                      "enum": [
                        "celsius",
                        "fahrenheit"
                      ]
                    }
                  },
                  "required": [
                    "location"
                  ]
                }
              }
            }
          ],
          "tool_choice": "auto"
        }
        """.trimIndent()
        lateinit var chatCompletionRequest: ChatCompletionRequest
        assertDoesNotThrow {
            chatCompletionRequest = JSON.parse<ChatCompletionRequest>(body)
        }
        val userMessage = chatCompletionRequest.messages.first()
        assertTrue(userMessage is TextChatMessage)
        assertEquals("What's the weather like in Boston today?", userMessage.content)

        val toolMessage = chatCompletionRequest.messages[1]
        assertTrue(userMessage is TextChatMessage)
        val expectedToolMessage = TextChatMessage(
            ChatRole.tool,
            toolCallId = "123456",
            toolCalls = null,
            name = "get_current_time",
            content = "time_response"
        )
        assertEquals(expectedToolMessage, toolMessage)

        val assistantMessage = chatCompletionRequest.messages[2]
        assertTrue(userMessage is TextChatMessage)
        val expectedAssistantMessage = TextChatMessage(
            ChatRole.assistant,
            content = null,
            toolCalls = listOf(
                ToolCall(
                    id = "call_abc123",
                    type = ToolType.function,
                    function = FunctionCall(
                        "get_current_weather",
                        "{\n\"location\": \"Boston, MA\"\n}"
                    )
                )
            )
        )
        assertEquals(expectedAssistantMessage, assistantMessage)

        assertNotNull(chatCompletionRequest.tools)
        val tool = chatCompletionRequest.tools!!.first()

        assertEquals(ToolType.function, tool.type)

        val function = tool.function
        assertEquals("get_current_weather", function.name)
        assertEquals("Get the current weather in a given location", function.description)

        val parameters = function.parameters
        assertTrue(parameters is Map<*, *>)
        parameters as Map<*, *>
        assertEquals("object", parameters["type"])

        val properties = parameters["properties"]
        assertTrue(properties is Map<*, *>)
        properties as Map<*, *>

        val location = properties["location"]
        assertTrue(location is Map<*, *>)
        location as Map<*, *>

        assertEquals("string", location["type"])
        assertEquals("The city and state, e.g. San Francisco, CA", location["description"])

        val unit = properties["unit"]
        assertTrue(unit is Map<*, *>)
        unit as Map<*, *>

        assertEquals("string", unit["type"])
        assertEquals(listOf("celsius", "fahrenheit"), unit["enum"])

        assertEquals(listOf("location"), parameters["required"])

        val actualTree = JSON.mapper.readTree(JSON.stringify(chatCompletionRequest))
        val expectedTree = JSON.mapper.readTree(body)
        assertEquals(expectedTree, actualTree)
    }

    @Test
    fun `should deserialize image chatgpt request`() {
        val body = """
                {
                  "model": "gpt-4-turbo",
                  "messages": [
                    {
                      "role": "user",
                      "content": [
                        {
                          "type": "text",
                          "text": "What's in this image?"
                        },
                        {
                          "type": "image_url",
                          "image_url": {
                            "url": "https://example.com/image.jpg"
                          }
                        }
                      ]
                    }
                  ],
                  "max_tokens": 300
                }
        """.trimIndent()

        lateinit var chatCompletionRequest: ChatCompletionRequest
        assertDoesNotThrow {
            chatCompletionRequest = JSON.parse<ChatCompletionRequest>(body)
        }
        val message = chatCompletionRequest.messages.first()
        assertTrue(message is PartsChatMessage)
        val messageContent = (message as PartsChatMessage).content
        assertNotNull(messageContent)

        val textContentPart = messageContent!!.get(0)
        assertTrue(textContentPart is TextContentPart)
        assertEquals("What's in this image?", (textContentPart as TextContentPart).text)

        val imageContentPart = messageContent!!.get(1)
        assertTrue(imageContentPart is ImageContentPart)
        assertEquals(
            ImageContentPartImageUrl("https://example.com/image.jpg"),
            (imageContentPart as ImageContentPart).imageUrl
        )
    }

    @Test
    fun `should deserialize image chatgpt response`() {
        val response = """
            {
              "id": "chatcmpl-123",
              "object": "chat.completion",
              "created": 1677652288,
              "model": "gpt-3.5-turbo-0125",
              "system_fingerprint": "fp_44709d6fcb",
              "choices": [{
                "index": 0,
                "message": {
                  "role": "assistant",
                  "content": "\n\nThis image shows a wooden boardwalk extending through a lush green marshland."
                },
                "logprobs": null,
                "finish_reason": "stop"
              }],
              "usage": {
                "prompt_tokens": 9,
                "completion_tokens": 12,
                "total_tokens": 21
              }
            }
        """.trimIndent()

        lateinit var chatCompletionResponse: ChatCompletionResult
        assertDoesNotThrow {
            chatCompletionResponse = JSON.parse<ChatCompletionResult>(response)
        }
        val message = chatCompletionResponse.choices.first().message
        assertTrue(message is TextChatMessage)
        val messageContent = (message as TextChatMessage).content
        assertEquals(
            "\n\nThis image shows a wooden boardwalk extending through a lush green marshland.",
            messageContent
        )
    }
}
