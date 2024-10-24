package com.platform.mpl.sdk.utils

import com.mlp.sdk.ActionShutdownConfig
import com.mlp.sdk.MlpExecutionContext
import com.mlp.sdk.MlpExecutionContext.Companion.systemContext
import com.mlp.sdk.MlpServiceConfig
import com.mlp.sdk.utils.JobsContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class JobContainerTest {

    private val coroutineScope = CoroutineScope(Executors.newFixedThreadPool(10).asCoroutineDispatcher())

    @Test
    fun `should cancel job`() = runBlocking {
        val number = AtomicInteger()

        val jobsContainer = JobsContainer(config(100, 100), systemContext)

        jobsContainer.launch(1, 1, 10, coroutineScope.launch { increment(number, 150) })

        jobsContainer.cancel(1, 1)

        delay(200)

        assertEquals(0, number.get())
    }

    @Test
    fun `jobs should complete before graceful shutdown finish`() = runBlocking {
        val number = AtomicInteger()

        val jobsContainer = JobsContainer(config(300, 100), systemContext)

        jobsContainer.launch(1, 1, 1, coroutineScope.launch { increment(number) })
        jobsContainer.launch(1, 1, 2, coroutineScope.launch { increment(number) })
        jobsContainer.launch(1, 1, 3, coroutineScope.launch { increment(number, 400) })
        jobsContainer.launch(2, 2, 4, coroutineScope.launch { increment(number) })

        jobsContainer.gracefulShutdownByConnector(1, 1)

        delay(100)

        assertEquals(3, number.get())
    }

    suspend fun JobsContainer.launch(containerId: Long, grpcChannelId: Long, requestId: Long, job: Job, delayMs: Long = 0): Job {
        delay(delayMs)

        put(containerId, requestId, grpcChannelId, job)
        return job
    }

    private fun config(gracefulShutdownAwaitMs: Long, gracefulShutdownActionDelayMs: Long) = MlpServiceConfig(
        initialGateUrls = listOf(),
        connectionToken = "test",
        shutdownConfig = ActionShutdownConfig(
            actionConnectorMs = gracefulShutdownAwaitMs,
            actionConnectorRequestDelayMs = gracefulShutdownActionDelayMs
        )
    )

    private suspend fun increment(number: AtomicInteger, delayMs: Long = 100) {
        delay(delayMs)
        number.incrementAndGet()
    }

}
