package com.platform.mpl.sdk

import com.mlp.gate.DatasetInfoProto
import com.mlp.gate.ExtendedRequestProto
import com.mlp.gate.FitRequestProto
import com.mlp.gate.PredictRequestProto.getDefaultInstance
import com.mlp.gate.ServiceInfoProto
import com.mlp.sdk.*
import com.mlp.sdk.MlpExecutionContext.Companion.systemContext
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
        val taskExecutor = TaskExecutor(service, config, null, systemContext)

        val connectorId_1 = 1L
        val connectorId_2 = 2L

        taskExecutor.predict(getDefaultInstance(), 1, connectorId_1, TimeTracker())
        taskExecutor.predict(getDefaultInstance(), 2, connectorId_1, TimeTracker())
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

        override suspend fun predict(req: Payload): MlpResponse {
            delay(100)
            number.incrementAndGet()
            return Payload("type")
        }

        override suspend fun fit(train: Payload, targets: Payload?, config: Payload?, modelDir: String, previousModelDir: String?,
                         targetServiceInfo: ServiceInfoProto,
                         dataset: DatasetInfoProto
        ): MlpResponse {
            delay(450)
            number.incrementAndGet()
            return Payload("type")
        }

        override suspend fun ext(methodName: String, params: Map<String, Payload>): MlpResponse {
            delay(450)
            extNumber.incrementAndGet()
            return Payload("type")
        }
    }
}
