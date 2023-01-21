package com.platform.mpl.sdk

import com.mlp.gate.ExtendedRequestProto
import com.mlp.gate.FitRequestProto
import com.mlp.gate.PredictRequestProto.getDefaultInstance
import com.mlp.sdk.ActionShutdownConfig
import com.mlp.sdk.MlpResponse
import com.mlp.sdk.MlpService
import com.mlp.sdk.MlpServiceConfig
import com.mlp.sdk.Payload
import com.mlp.sdk.TaskExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class TaskExecutorTest {

    @Test
    fun `should remove job from container after finish and dont block other connectors`() = runBlocking {
        val config = MlpServiceConfig(
            initialGateUrls =  listOf(),
            connectionToken =  "test",
            shutdownConfig = ActionShutdownConfig(
                actionConnectorMs = 150,
                actionConnectorRequestDelayMs = 50
            )
        )
        val taskExecutor = TaskExecutor(service, config)

        val connectorId_1 = 1L
        val connectorId_2 = 2L

        taskExecutor.predict(getDefaultInstance(), 1, connectorId_1)
        taskExecutor.predict(getDefaultInstance(), 2, connectorId_1)
        launch(Dispatchers.Default) {
            delay(100)
            taskExecutor.fit(FitRequestProto.getDefaultInstance(), 3, connectorId_1)
        }
        launch(Dispatchers.Default) {
            delay(100)
            taskExecutor.ext(ExtendedRequestProto.getDefaultInstance(), 1, connectorId_2)
        }
        taskExecutor.gracefulShutdownAll(connectorId_1)

        delay(1000)

        assertEquals(2, service.number.get())
        assertEquals(1, service.extNumber.get())
    }

    object service: MlpService() {

        val number = AtomicInteger()
        val extNumber = AtomicInteger()

        override fun predict(req: Payload): MlpResponse {
            Thread.sleep(100)
            number.incrementAndGet()
            return Payload("type")
        }

        override fun fit(train: Payload, targets: Payload?, config: Payload?, modelDir: String, previousModelDir: String?): MlpResponse {
            Thread.sleep(450)
            number.incrementAndGet()
            return Payload("type")
        }

        override fun ext(methodName: String, params: Map<String, Payload>): MlpResponse {
            Thread.sleep(450)
            extNumber.incrementAndGet()
            return Payload("type")
        }
    }
}