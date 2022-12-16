package com.platform.mpl.sdk

import com.platform.mpl.gate.ExtendedRequestProto
import com.platform.mpl.gate.FitRequestProto
import com.platform.mpl.gate.PredictRequestProto.getDefaultInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class ActionTaskExecutorTest {

    @Test
    fun `should remove job from container after finish and dont block other connectors`() = runBlocking {
        val config = PlatformActionConfig(
            initialGateUrls =  listOf(),
            connectionToken =  "test",
            shutdownConfig = ActionShutdownConfig(
                actionConnectorMs = 150,
                actionConnectorRequestDelayMs = 50
            )
        )
        val actionTaskExecutor = ActionTaskExecutor(service, config)

        val connectorId_1 = 1L
        val connectorId_2 = 2L

        actionTaskExecutor.predict(getDefaultInstance(), 1, connectorId_1)
        actionTaskExecutor.predict(getDefaultInstance(), 2, connectorId_1)
        launch(Dispatchers.Default) {
            delay(100)
            actionTaskExecutor.fit(FitRequestProto.getDefaultInstance(), 3, connectorId_1)
        }
        launch(Dispatchers.Default) {
            delay(100)
            actionTaskExecutor.ext(ExtendedRequestProto.getDefaultInstance(), 1, connectorId_2)
        }
        actionTaskExecutor.gracefulShutdownAll(connectorId_1)

        delay(1000)

        assertEquals(2, service.number.get())
        assertEquals(1, service.extNumber.get())
    }

    object service: PlatformAction() {

        val number = AtomicInteger()
        val extNumber = AtomicInteger()

        override fun predict(req: Payload): PlatformResponse {
            Thread.sleep(100)
            number.incrementAndGet()
            return Payload("type")
        }

        override fun fit(train: Payload, targets: Payload, config: Payload?, modelDir: String, previousModelDir: String): PlatformResponse {
            Thread.sleep(450)
            number.incrementAndGet()
            return Payload("type")
        }

        override fun ext(methodName: String, params: Map<String, Payload>): PlatformResponse {
            Thread.sleep(450)
            extNumber.incrementAndGet()
            return Payload("type")
        }
    }
}