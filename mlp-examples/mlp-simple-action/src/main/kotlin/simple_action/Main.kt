package simple_action

import com.mlp.sdk.MlpException
import com.mlp.sdk.MlpExecutionContext
import com.mlp.sdk.MlpExecutionContext.Companion.systemContext
import com.mlp.sdk.MlpPredictServiceBase
import com.mlp.sdk.MlpServiceSDK
import com.mlp.sdk.WithExecutionContext
/*
 * JSON запроса из Caila.io
 */
data class SimpleTestActionRequest(
    val action: String,
    val name: String
)


/*
 * @param context - контекст для запуска в JVM процессе инстанса mlp-sdk
 * MlpPredictServiceBase - класс для реализации predict метода
 * predict метод позволяет принимать/отправлять запросы в Caila.io
 */
class SimpleTestAction(
    override val context: MlpExecutionContext
) : MlpPredictServiceBase<SimpleTestActionRequest, String>(REQUEST_EXAMPLE, RESPONSE_EXAMPLE) {

    /*
     * 1. Из Caila.io через SDK приходит Request
     * 2. Обрабатывается запрос
     * 3. В Ваш сервис в Caila.io отправляется Response
     */
    override fun predict(req: SimpleTestActionRequest): String {
        return when (req.action) {
            "hello" -> "Hello ${req.name}!"
            "envs" -> "Envs: ${context.environment.envsOverride}"
            else -> throw MlpException("actionUnknownException")
        }
    }

    /*
     * Примеры для Дескриптора в Вашем сервисе
     * Они полезны для тестрования
     */
    companion object {
        val REQUEST_EXAMPLE = SimpleTestActionRequest("hello", "World")
        val RESPONSE_EXAMPLE = "Hello World!"
    }
}

/*
 * Запуск сервиса
 * systemContext - контекст для запуска в JVM процессе инстанса mlp-sdk
 */
fun main() {
    val actionSDK = MlpServiceSDK({ SimpleTestAction(systemContext) })

    actionSDK.start()
    actionSDK.blockUntilShutdown()
}
