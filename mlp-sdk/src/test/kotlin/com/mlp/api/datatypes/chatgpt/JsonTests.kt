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
        assertEquals(ToolChoiceEnum.auto, chatCompletionRequest.toolChoice)
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

    @Test
    fun `should deserialize logprobs chatgpt request and response`() {
        val body = """
            {
              "model": "gpt-4o",
              "messages": [
                {
                  "role": "user",
                  "content": "Hello!"
                }
              ],
              "logprobs": true,
              "top_logprobs": 2
            }
        """.trimIndent()
        lateinit var chatCompletionRequest: ChatCompletionRequest
        assertDoesNotThrow {
            chatCompletionRequest = JSON.parse<ChatCompletionRequest>(body)
        }
        assertEquals(TextChatMessage(ChatRole.user, "Hello!"), chatCompletionRequest.messages.first())
        assertEquals(true, chatCompletionRequest.logprobs)
        assertEquals(2, chatCompletionRequest.topLogprobs)

        val response = """
            {
              "id": "chatcmpl-123",
              "object": "chat.completion",
              "created": 1702685778,
              "model": "gpt-3.5-turbo-0125",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": "Hello! How can I assist you today?"
                  },
                  "logprobs": {
                    "content": [
                      {
                        "token": "Hello",
                        "logprob": -0.31725305,
                        "bytes": [72, 101, 108, 108, 111],
                        "top_logprobs": [
                          {
                            "token": "Hello",
                            "logprob": -0.31725305,
                            "bytes": [72, 101, 108, 108, 111]
                          },
                          {
                            "token": "Hi",
                            "logprob": -1.3190403,
                            "bytes": [72, 105]
                          }
                        ]
                      },
                      {
                        "token": "!",
                        "logprob": -0.02380986,
                        "bytes": [
                          33
                        ],
                        "top_logprobs": [
                          {
                            "token": "!",
                            "logprob": -0.02380986,
                            "bytes": [33]
                          },
                          {
                            "token": " there",
                            "logprob": -3.787621,
                            "bytes": [32, 116, 104, 101, 114, 101]
                          }
                        ]
                      },
                      {
                        "token": " How",
                        "logprob": -0.000054669687,
                        "bytes": [32, 72, 111, 119],
                        "top_logprobs": [
                          {
                            "token": " How",
                            "logprob": -0.000054669687,
                            "bytes": [32, 72, 111, 119]
                          },
                          {
                            "token": "<|end|>",
                            "logprob": -10.953937,
                            "bytes": null
                          }
                        ]
                      },
                      {
                        "token": " can",
                        "logprob": -0.015801601,
                        "bytes": [32, 99, 97, 110],
                        "top_logprobs": [
                          {
                            "token": " can",
                            "logprob": -0.015801601,
                            "bytes": [32, 99, 97, 110]
                          },
                          {
                            "token": " may",
                            "logprob": -4.161023,
                            "bytes": [32, 109, 97, 121]
                          }
                        ]
                      },
                      {
                        "token": " I",
                        "logprob": -3.7697225e-6,
                        "bytes": [
                          32,
                          73
                        ],
                        "top_logprobs": [
                          {
                            "token": " I",
                            "logprob": -3.7697225e-6,
                            "bytes": [32, 73]
                          },
                          {
                            "token": " assist",
                            "logprob": -13.596657,
                            "bytes": [32, 97, 115, 115, 105, 115, 116]
                          }
                        ]
                      },
                      {
                        "token": " assist",
                        "logprob": -0.04571125,
                        "bytes": [32, 97, 115, 115, 105, 115, 116],
                        "top_logprobs": [
                          {
                            "token": " assist",
                            "logprob": -0.04571125,
                            "bytes": [32, 97, 115, 115, 105, 115, 116]
                          },
                          {
                            "token": " help",
                            "logprob": -3.1089056,
                            "bytes": [32, 104, 101, 108, 112]
                          }
                        ]
                      },
                      {
                        "token": " you",
                        "logprob": -5.4385737e-6,
                        "bytes": [32, 121, 111, 117],
                        "top_logprobs": [
                          {
                            "token": " you",
                            "logprob": -5.4385737e-6,
                            "bytes": [32, 121, 111, 117]
                          },
                          {
                            "token": " today",
                            "logprob": -12.807695,
                            "bytes": [32, 116, 111, 100, 97, 121]
                          }
                        ]
                      },
                      {
                        "token": " today",
                        "logprob": -0.0040071653,
                        "bytes": [32, 116, 111, 100, 97, 121],
                        "top_logprobs": [
                          {
                            "token": " today",
                            "logprob": -0.0040071653,
                            "bytes": [32, 116, 111, 100, 97, 121]
                          },
                          {
                            "token": "?",
                            "logprob": -5.5247097,
                            "bytes": [63]
                          }
                        ]
                      },
                      {
                        "token": "?",
                        "logprob": -0.0008108172,
                        "bytes": [63],
                        "top_logprobs": [
                          {
                            "token": "?",
                            "logprob": -0.0008108172,
                            "bytes": [63]
                          },
                          {
                            "token": "?\n",
                            "logprob": -7.184561,
                            "bytes": [63, 10]
                          }
                        ]
                      }
                    ]
                  },
                  "finish_reason": "stop"
                }
              ],
              "usage": {
                "prompt_tokens": 9,
                "completion_tokens": 9,
                "total_tokens": 18
              },
              "system_fingerprint": null
            }
        """.trimIndent()

        lateinit var chatCompletionResponse: ChatCompletionResult
        assertDoesNotThrow {
            chatCompletionResponse = JSON.parse<ChatCompletionResult>(response)
        }
        val choice = chatCompletionResponse.choices.first()
        val message = choice.message
        assertTrue(message is TextChatMessage)
        val messageContent = (message as TextChatMessage).content
        assertEquals(
            "Hello! How can I assist you today?",
            messageContent
        )

        assertNotNull(choice.logprobs)
        assertEquals(9, choice.logprobs?.content?.size)
        assertEquals(-0.31725305, chatCompletionResponse.choices[0].logprobs?.content?.get(0)?.logprob?.toDouble())
    }

    @Test
    fun `should deserialize named function call chatgpt request`() {
        val body = """
            {
              "model": "Qwen/Qwen2-7B-Instruct",
              "messages": [
                {
                  "role": "user",
                  "content": "What's the weather like in Boston today?"
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
              "tool_choice": {
                "type": "function",
                "function": {
                  "name": "get_current_weather"
                }
              }
            }
        """.trimIndent()
        lateinit var chatCompletionRequest: ChatCompletionRequest
        assertDoesNotThrow {
            chatCompletionRequest = JSON.parse<ChatCompletionRequest>(body)
        }
        assertEquals(
            NamedToolChoice(ToolType.function, NamedToolChoiceFunction("get_current_weather")),
            chatCompletionRequest.toolChoice
        )
    }
}
