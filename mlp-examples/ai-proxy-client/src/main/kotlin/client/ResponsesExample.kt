package client

import com.mlp.api.datatypes.chatgpt.responsesapi.CodeInterpreterAutoContainer
import com.mlp.api.datatypes.chatgpt.responsesapi.CodeInterpreterContainerType
import com.mlp.api.datatypes.chatgpt.responsesapi.CodeInterpreterResponsesApiTool
import com.mlp.api.datatypes.chatgpt.responsesapi.ComputerCallOutputItemParamInputItem
import com.mlp.api.datatypes.chatgpt.responsesapi.ComputerCallOutputType
import com.mlp.api.datatypes.chatgpt.responsesapi.ComputerResponsesApiToolCallOutputItem
import com.mlp.api.datatypes.chatgpt.responsesapi.ComputerScreenshotImage
import com.mlp.api.datatypes.chatgpt.responsesapi.ComputerUsePreviewResponsesApiTool
import com.mlp.api.datatypes.chatgpt.responsesapi.FileSearchResponsesApiTool
import com.mlp.api.datatypes.chatgpt.responsesapi.FunctionResponsesApiTool
import com.mlp.api.datatypes.chatgpt.responsesapi.ImageDetailLevel
import com.mlp.api.datatypes.chatgpt.responsesapi.ImageGenerationOutputFormat
import com.mlp.api.datatypes.chatgpt.responsesapi.ImageGenerationQuality
import com.mlp.api.datatypes.chatgpt.responsesapi.ImageGenerationResponsesApiTool
import com.mlp.api.datatypes.chatgpt.responsesapi.ImageGenerationSize
import com.mlp.api.datatypes.chatgpt.responsesapi.InputItemType
import com.mlp.api.datatypes.chatgpt.responsesapi.LocalShellResponsesApiTool
import com.mlp.api.datatypes.chatgpt.responsesapi.MessageContentItemsInput
import com.mlp.api.datatypes.chatgpt.responsesapi.MessageContentTextInput
import com.mlp.api.datatypes.chatgpt.responsesapi.MessageInputFileInputItemContent
import com.mlp.api.datatypes.chatgpt.responsesapi.MessageInputImageInputItemContent
import com.mlp.api.datatypes.chatgpt.responsesapi.MessageInputItem
import com.mlp.api.datatypes.chatgpt.responsesapi.MessageInputItemContentType
import com.mlp.api.datatypes.chatgpt.responsesapi.MessageInputTextInputItemContent
import com.mlp.api.datatypes.chatgpt.responsesapi.MessageRole
import com.mlp.api.datatypes.chatgpt.responsesapi.Reasoning
import com.mlp.api.datatypes.chatgpt.responsesapi.ReasoningEffort
import com.mlp.api.datatypes.chatgpt.responsesapi.ReasoningSummaryType
import com.mlp.api.datatypes.chatgpt.responsesapi.ResponsesApiRequest
import com.mlp.api.datatypes.chatgpt.responsesapi.ResponsesApiRequestItemsInput
import com.mlp.api.datatypes.chatgpt.responsesapi.ResponsesApiRequestTextInput
import com.mlp.api.datatypes.chatgpt.responsesapi.ResponsesApiResult
import com.mlp.api.datatypes.chatgpt.responsesapi.ResponsesApiStreamEvent
import com.mlp.api.datatypes.chatgpt.responsesapi.ResponsesApiToolChoiceFunction
import com.mlp.api.datatypes.chatgpt.responsesapi.ResponsesApiToolType
import com.mlp.api.datatypes.chatgpt.responsesapi.UserLocation
import com.mlp.api.datatypes.chatgpt.responsesapi.UserLocationType
import com.mlp.api.datatypes.chatgpt.responsesapi.WebSearchPreviewResponsesApiTool
import com.mlp.sdk.MlpClientSDK
import com.mlp.sdk.MlpExecutionContext.Companion.systemContext
import com.mlp.sdk.Payload
import com.mlp.sdk.datatypes.aiproxy.AiProxyRequest
import com.mlp.sdk.utils.JSON
import java.util.Base64
import kotlinx.coroutines.runBlocking
import org.springframework.util.ResourceUtils

private val clientSDK = MlpClientSDK(context = systemContext)

fun main() = runBlocking {

    runTextMessage()
    runTextMessageStream()
    runMultipleTextMessages()
    runImageInput()
    runPythonScript()
    runWebSearch()
    runFunctionCall()
    runReasoning()
    runComputerUse()
    runLocalShell()
    runGenerateImage()
    runFileSearch()

    clientSDK.shutdown()
}

private suspend fun runTextMessage() {
    processRequest(
        request = ResponsesApiRequest(
            model = "gpt-4o",
            input = ResponsesApiRequestTextInput("Hi!")
        )
    )
}

private suspend fun runTextMessageStream() {
    val request = AiProxyRequest(
        responsesApi = ResponsesApiRequest(
            model = "gpt-4o",
            input = ResponsesApiRequestTextInput("Hi!"),
            stream = true,
        )
    )
    val flow = clientSDK.predictStream("just-ai", "openai-proxy", Payload(JSON.stringify(request)))
    flow.collect {
        val json = it.partialPredict.data.json
        val res = JSON.parse<ResponsesApiStreamEvent>(json)
        println(res)
    }
}

private suspend fun runMultipleTextMessages() {
    processRequest(
        request = ResponsesApiRequest(
            model = "gpt-4o",
            input = ResponsesApiRequestItemsInput(
                items = listOf(
                    MessageInputItem(
                        type = InputItemType.message,
                        role = MessageRole.user,
                        content = MessageContentTextInput("Count to 3. On 3 write END"),
                    ),
                    MessageInputItem(
                        type = InputItemType.message,
                        role = MessageRole.assistant,
                        content = MessageContentTextInput("1")
                    ),
                    MessageInputItem(
                        type = InputItemType.message,
                        role = MessageRole.user,
                        content = MessageContentTextInput("2")
                    ),
                )
            )
        )
    )
}

private suspend fun runWebSearch() {
    processRequest(
        request = ResponsesApiRequest(
            model = "gpt-4o",
            input = ResponsesApiRequestTextInput("last local news"),
            tools = listOf(
                WebSearchPreviewResponsesApiTool(
                    type = ResponsesApiToolType.web_search_preview,
                    userLocation = UserLocation(
                        type = UserLocationType.approximate,
                        country = "IT",
                        city = "Bologna",
                    ),
                    searchContextSize = WebSearchPreviewResponsesApiTool.SearchContextSize.low,
                )
            ),
            toolChoice = ResponsesApiToolChoiceFunction(
                type = ResponsesApiToolType.web_search_preview.value,
            )
        )
    )
}

private suspend fun runImageInput() {
    processRequest(
        request = ResponsesApiRequest(
            model = "gpt-4o",
            input = ResponsesApiRequestItemsInput(
                items = listOf(
                    MessageInputItem(
                        type = InputItemType.message,
                        role = MessageRole.user,
                        content = MessageContentTextInput("what is in this image?")
                    ),
                    MessageInputItem(
                        type = InputItemType.message,
                        role = MessageRole.user,
                        content = MessageContentItemsInput(
                            items = listOf(
                                MessageInputImageInputItemContent(
                                    type = MessageInputItemContentType.input_image,
                                    imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/dd/Gfp-wisconsin-madison-the-nature-boardwalk.jpg/2560px-Gfp-wisconsin-madison-the-nature-boardwalk.jpg",
                                    detail = ImageDetailLevel.auto,
                                )
                            ),
                        )
                    ),
                )
            )
        )
    )
}

private suspend fun runPythonScript() {
    val fileData = buildDataUrl(
        mimeType = "text/x-python",
        content = """
            import random
            print(random.randint(0,9))
        """.trimIndent().toByteArray()
    )

    processRequest(
        request = ResponsesApiRequest(
            model = "gpt-4o",
            input = ResponsesApiRequestItemsInput(
                items = listOf(
                    MessageInputItem(
                        type = InputItemType.message,
                        role = MessageRole.user,
                        content = MessageContentTextInput("run this python script")
                    ),
                    MessageInputItem(
                        type = InputItemType.message,
                        role = MessageRole.user,
                        content = MessageContentItemsInput(
                            items = listOf(
                                MessageInputFileInputItemContent(
                                    type = MessageInputItemContentType.input_file,
                                    fileData = fileData,
                                    filename = "script.py",
                                )
                            )
                        ),
                    ),
                )
            ),
            tools = listOf(
                CodeInterpreterResponsesApiTool(
                    type = ResponsesApiToolType.code_interpreter,
                    container = CodeInterpreterAutoContainer(
                        type = CodeInterpreterContainerType.auto,
                    )
                )
            )
        )
    )
}

private suspend fun runFileSearch() {
    processRequest(
        request = ResponsesApiRequest(
            model = "gpt-4o",
            input = ResponsesApiRequestTextInput("What are the attributes of an ancient brown dragon?"),
            tools = listOf(
                FileSearchResponsesApiTool(
                    type = ResponsesApiToolType.file_search,
                    vectorStoreIds = listOf("vs_1234567890"),
                    maxNumResults = 3,
                )
            )
        )
    )
}

private suspend fun runFunctionCall() {
    val parameters = JSON.parse(
        """
        {
          "type": "object",
          "properties": {
            "location": {
              "type": "string",
              "description": "The city and state, e.g. San Francisco, CA"
            },
            "unit": {
              "type": "string",
              "enum": ["celsius", "fahrenheit"]
            }
          },
          "required": ["location", "unit"],
          "additionalProperties": false
        }
        """.trimIndent()
    )
    processRequest(
        request = ResponsesApiRequest(
            model = "gpt-4o",
            input = ResponsesApiRequestTextInput("What is the weather like in Boston today?"),
            tools = listOf(
                FunctionResponsesApiTool(
                    type = ResponsesApiToolType.function,
                    name = "get_current_weather",
                    parameters = parameters,
                    strict = true,
                ),
            ),
            toolChoice = ResponsesApiToolChoiceFunction(
                type = ResponsesApiToolType.function.value,
                name = "get_current_weather",
            )
        )
    )
}

private suspend fun runReasoning() {
    processRequest(
        request = ResponsesApiRequest(
            model = "o3",
            input = ResponsesApiRequestTextInput("I have \$20,000 in my savings account, where I receive a 4% profit per year and payments twice a year. Can you please tell me how long it will take for me to become a millionaire? Also, can you please explain the math step by step as if you were explaining it to an uneducated person?"),
            reasoning = Reasoning(
                effort = ReasoningEffort.low,
                summary = ReasoningSummaryType.detailed,
            )
        )
    )
}

private suspend fun runComputerUse() {
    var result = processRequest(
        request = ResponsesApiRequest(
            model = "computer-use-preview",
            input = ResponsesApiRequestItemsInput(
                items = listOf(
                    MessageInputItem(
                        type = InputItemType.message,
                        role = MessageRole.user,
                        content = MessageContentTextInput("Search 'cat images' on google."),
                    )
                ),
            ),
            truncation = ResponsesApiRequest.Truncation.auto,
            tools = listOf(
                ComputerUsePreviewResponsesApiTool(
                    type = ResponsesApiToolType.computer_use_preview,
                    environment = ComputerUsePreviewResponsesApiTool.Environment.browser,
                    displayWidth = 1024,
                    displayHeight = 768,
                ),
            ),
            reasoning = Reasoning(
                summary = ReasoningSummaryType.concise,
            ),
        )
    )

    val screenshots = listOf("1.jpg", "2.jpg", "3.jpg").map { fileName ->
        buildDataUrl(
            mimeType = "image/jpeg",
            content = ResourceUtils.getFile("classpath:images/computer_use/$fileName").readBytes(),
        )
    }

    for (screenshot in screenshots) {
        result = runComputerUseStep(
            screenshot = screenshot,
            previousResult = result,
        )
    }
}

private suspend fun runComputerUseStep(
    screenshot: String,
    previousResult: ResponsesApiResult,
) = processRequest(
    request = ResponsesApiRequest(
        model = "computer-use-preview",
        previousResponseId = previousResult.id,
        input = ResponsesApiRequestItemsInput(
            items = listOf(
                ComputerCallOutputItemParamInputItem(
                    type = InputItemType.computer_call_output,
                    callId = previousResult.output.filterIsInstance<ComputerResponsesApiToolCallOutputItem>()
                        .first().callId,
                    output = ComputerScreenshotImage(
                        type = ComputerCallOutputType.computer_screenshot,
                        imageUrl = screenshot,
                    ),
                )
            ),
        ),
        truncation = ResponsesApiRequest.Truncation.auto,
        tools = listOf(
            ComputerUsePreviewResponsesApiTool(
                type = ResponsesApiToolType.computer_use_preview,
                environment = ComputerUsePreviewResponsesApiTool.Environment.browser,
                displayWidth = 1024,
                displayHeight = 768,
            ),
        ),
        reasoning = Reasoning(
            summary = ReasoningSummaryType.concise,
        ),
    )
)

private suspend fun runLocalShell() {
    processRequest(
        request = ResponsesApiRequest(
            model = "codex-mini-latest",
            input = ResponsesApiRequestItemsInput(
                items = listOf(
                    MessageInputItem(
                        type = InputItemType.message,
                        role = MessageRole.user,
                        content = MessageContentItemsInput(
                            items = listOf(
                                MessageInputTextInputItemContent(
                                    type = MessageInputItemContentType.input_text,
                                    text = "find the last 10 rows in file data.csv that contain string 'cat'",
                                )
                            ),
                        )
                    )
                ),
            ),
            tools = listOf(
                LocalShellResponsesApiTool(
                    type = ResponsesApiToolType.local_shell,
                ),
            ),
            toolChoice = ResponsesApiToolChoiceFunction(
                type = ResponsesApiToolType.local_shell.value,
            ),
        )
    )
}

private suspend fun runGenerateImage() {
    processRequest(
        request = ResponsesApiRequest(
            model = "gpt-4o",
            input = ResponsesApiRequestItemsInput(
                items = listOf(
                    MessageInputItem(
                        type = InputItemType.message,
                        role = MessageRole.user,
                        content = MessageContentTextInput("generate an image of a tree"),
                    )
                ),
            ),
            tools = listOf(
                ImageGenerationResponsesApiTool(
                    type = ResponsesApiToolType.image_generation,
                    propertySize = ImageGenerationSize._1024x1024,
                    quality = ImageGenerationQuality.low,
                    outputFormat = ImageGenerationOutputFormat.webp,
                ),
            ),
        )
    )
}

private fun buildDataUrl(mimeType: String, content: ByteArray): String {
    val base64Content = Base64.getEncoder().encode(content).toString(Charsets.UTF_8)
    return "data:$mimeType;base64,$base64Content"
}

private suspend fun processRequest(request: Any): ResponsesApiResult {
    val response = clientSDK.predict("just-ai", "openai-proxy", JSON.stringify(AiProxyRequest(responsesApi = request)))

    val aiProxyResponse = JSON.parseObject(response)

    println(JSON.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(aiProxyResponse))

    return JSON.parse(aiProxyResponse["responsesApi"], ResponsesApiResult::class.java)
}
