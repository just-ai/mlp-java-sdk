package com.platform.mpl.sdk.utils

import com.mlp.sdk.ActionShutdownConfig
import com.mlp.sdk.MlpServiceConfig
import com.mlp.sdk.utils.JobsContainer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JobContainerTest {

    private val coroutineScope = CoroutineScope(Executors.newFixedThreadPool(10).asCoroutineDispatcher())

    @Test
    fun `jobs should complete before graceful shutdown finish`() = runBlocking {
        val number = AtomicInteger()

        val jobsContainer = JobsContainer(buildConfig(gracefulShutdownAwaitMs = 300, gracefulShutdownActionDelayMs = 100))

        jobsContainer.launch(1, 1, coroutineScope.launch { increment(number) })
        jobsContainer.launch(1, 1, coroutineScope.launch { increment(number) })
        jobsContainer.launch(1, 1, coroutineScope.launch { increment(number, 400) })
        jobsContainer.launch(2, 2, coroutineScope.launch { increment(number) })

        jobsContainer.gracefulShutdownByConnector(1)

        assertEquals(3, number.get())
    }

    suspend fun JobsContainer.launch(containerId: Long, requestId: Long, job: Job, delayMs: Long = 0): Job {
        delay(delayMs)

        put(containerId, requestId, job)
        return job
    }

    private fun buildConfig(
        gracefulShutdownAwaitMs: Long,
        gracefulShutdownActionDelayMs: Long,
    ) = MlpServiceConfig(
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
