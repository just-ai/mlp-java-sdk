package com.mpl.sdk

import com.mpl.gate.ExtendedRequestProto
import com.mpl.gate.FitRequestProto
import com.mpl.gate.PredictRequestProto.getDefaultInstance
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
        val config = MplActionConfig(
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

    object service: MplAction {

        val number = AtomicInteger()
        val extNumber = AtomicInteger()

        override fun predict(req: Payload): MplResponse {
            Thread.sleep(100)
            number.incrementAndGet()
            return Payload("type")
        }

        override fun fit(train: Payload, targets: Payload, config: Payload?): MplResponse {
            Thread.sleep(450)
            number.incrementAndGet()
            return Payload("type")
        }

        override fun ext(methodName: String, params: Map<String, Payload>): MplResponse {
            Thread.sleep(450)
            extNumber.incrementAndGet()
            return Payload("type")
        }
    }
}