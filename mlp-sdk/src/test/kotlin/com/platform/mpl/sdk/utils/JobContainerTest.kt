package com.mlp.sdk.utils

import com.mlp.sdk.ActionShutdownConfig
import com.mlp.sdk.MlpServiceConfig
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

        val jobsContainer = JobsContainer(config(100, 100))

        jobsContainer.launch(1, 10, coroutineScope.launch { increment(number, 150) })

        jobsContainer.cancel(1)

        delay(200)

        assertEquals(0, number.get())
    }

    @Test
    fun `jobs should complete before graceful shutdown finish`() = runBlocking {
        val number = AtomicInteger()

        val jobsContainer = JobsContainer(config(300, 100))

        jobsContainer.launch(1, 1, coroutineScope.launch { increment(number) })
        jobsContainer.launch(1, 2, coroutineScope.launch { increment(number) })
        jobsContainer.launch(1, 3, coroutineScope.launch { increment(number, 400) })
        jobsContainer.launch(2, 4, coroutineScope.launch { increment(number) })

        jobsContainer.gracefulShutdownByConnector(1)

        delay(100)

        assertEquals(3, number.get())
    }

    suspend fun JobsContainer.launch(containerId: Long, requestId: Long, job: Job, delayMs: Long = 0): Job {
        delay(delayMs)

        put(containerId, requestId, job)
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