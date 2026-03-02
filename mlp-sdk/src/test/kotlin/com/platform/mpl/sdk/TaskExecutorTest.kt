package com.platform.mpl.sdk

import com.mlp.gate.DatasetInfoProto
import com.mlp.gate.ExtendedRequestProto
import com.mlp.gate.FitRequestProto
import com.mlp.gate.PredictRequestProto.getDefaultInstance
import com.mlp.gate.ServiceInfoProto
import com.mlp.sdk.ActionShutdownConfig
import com.mlp.sdk.ConnectorsPool
import com.mlp.sdk.MlpExecutionContext.Companion.systemContext
import com.mlp.sdk.MlpResponse
import com.mlp.sdk.MlpService
import com.mlp.sdk.MlpServiceConfig
import com.mlp.sdk.Payload
import com.mlp.sdk.RequestContext
import com.mlp.sdk.TaskExecutor
import com.mlp.sdk.TimeTracker
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TaskExecutorTest {

    @Test
    fun `should remove job from container after finish and dont block other connectors`() = runBlocking {
        val config = MlpServiceConfig(
            initialGateUrls = listOf(
                "localhost:8080",
                "localhost:8081",
            ),
            connectionToken = "test",
            shutdownConfig = ActionShutdownConfig(
                actionConnectorMs = 0,
                actionConnectorRequestDelayMs = 50
            )
        )

        val mlpService = TestMlpService()
        val taskExecutor = TaskExecutor(mlpService, config, null, systemContext)
        taskExecutor.connectorsPool = ConnectorsPool(config.connectionToken, taskExecutor, config, systemContext)

        val connectorId0 = 0L
        val connectorId1 = 1L

        val requestContext0 = RequestContext(connectorId = connectorId0)
        val requestContext1 = RequestContext(connectorId = connectorId1)

        taskExecutor.predict(getDefaultInstance(), TimeTracker(), requestContext0)
        taskExecutor.predict(getDefaultInstance(), TimeTracker(), requestContext0)

        launch(Dispatchers.Default) {
            delay(100)
            taskExecutor.fit(FitRequestProto.getDefaultInstance(), requestContext1)
        }

        launch(Dispatchers.Default) {
            delay(100)
            taskExecutor.ext(ExtendedRequestProto.getDefaultInstance(), requestContext1)
        }

        taskExecutor.gracefulShutdownAll(connectorId0)

        delay(1000)

        assertEquals(2, mlpService.number.get())
        assertEquals(1, mlpService.extNumber.get())
    }

    class TestMlpService : MlpService() {

        val number = AtomicInteger()
        val extNumber = AtomicInteger()

        override suspend fun predict(req: Payload): MlpResponse {
            delay(100)
            number.incrementAndGet()
            return Payload("type")
        }

        override suspend fun fit(
            train: Payload,
            targets: Payload?,
            config: Payload?,
            modelDir: String,
            previousModelDir: String?,
            targetServiceInfo: ServiceInfoProto,
            dataset: DatasetInfoProto,
        ): MlpResponse {
            delay(450)
            number.incrementAndGet()
            return Payload("type")
        }

        override suspend fun ext(
            methodName: String,
            params: Map<String, Payload>,
        ): MlpResponse {
            delay(450)
            extNumber.incrementAndGet()
            return Payload("type")
        }
    }
}
