package asr_action

import com.google.protobuf.kotlin.toByteStringUtf8
import com.mlp.sdk.MlpExecutionContext
import com.mlp.sdk.MlpExecutionContext.Companion.systemContext
import com.mlp.sdk.MlpPredictServiceBase
import com.mlp.sdk.MlpServiceSDK
import com.mlp.sdk.datatypes.asr.common.AsrRequest
import com.mlp.sdk.datatypes.asr.common.AsrResponse
import com.mlp.sdk.datatypes.asr.common.RecognitionConfig
import com.mlp.sdk.datatypes.asr.common.SpeechRecognitionAlternative
import com.mlp.sdk.datatypes.asr.common.SpeechRecognitionChunk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlin.random.Random


/**
 * @param context - контекст для запуска в JVM процессе инстанса mlp-sdk
 * MlpPredictServiceBase - класс для реализации predict метода
 * predict метод позволяет принимать/отправлять запросы в Caila.io
 */
class AsrAction(
    override val context: MlpExecutionContext
) : MlpPredictServiceBase<AsrRequest, AsrResponse>(REQUEST_EXAMPLE, RESPONSE_EXAMPLE) {
    private val defaultResponses = listOf(
        "hello",
        "goodbye",
        "i am fine",
        "how are you"
    )
    private val russianResponses = listOf(
        "привет",
        "пока",
        "я в порядке",
        "как ты"
    )

    override suspend fun streamPredict(stream: Flow<Pair<AsrRequest, Unit?>>): Flow<AsrResponse?> {
        return stream.map { (req, _) ->
            val text = (if (req.config?.languageCode == "ru-RU") russianResponses else defaultResponses).random()
            AsrResponse(
                chunks = listOf(
                    SpeechRecognitionChunk(
                        alternatives = listOf(
                            SpeechRecognitionAlternative(text, Random.nextFloat())
                        ),
                        final = true
                    )
                ),
                final = true,
                providerSpecific = """{"field": "value"}"""
            )
        }
    }

    /**
     * 1. Из Caila.io через SDK приходит Request
     * 2. Обрабатывается запрос
     * 3. В Ваш сервис в Caila.io отправляется Response
     */
    override fun predict(req: AsrRequest): AsrResponse = runBlocking {
        return@runBlocking streamPredict(flowOf(req to null)).first()!!
    }

    /**
     * Примеры для Дескриптора в Вашем сервисе
     * Они полезны для тестрования
     */
    companion object {
        val REQUEST_EXAMPLE = AsrRequest("hello".toByteStringUtf8(), RecognitionConfig())
        val RESPONSE_EXAMPLE = AsrResponse(
            chunks = listOf(
                SpeechRecognitionChunk(
                    alternatives = listOf(
                        SpeechRecognitionAlternative("hello", 1.0f)
                    ),
                    final = true
                )
            ),
            final = true,
            providerSpecific = """{"field": "value"}"""
        )
    }
}

/**
 * Запуск сервиса
 * systemContext - контекст для запуска в JVM процессе инстанса mlp-sdk
 */
fun main() {
    val actionSDK = MlpServiceSDK({ AsrAction(systemContext) })

    actionSDK.start()
    actionSDK.blockUntilShutdown()
}
