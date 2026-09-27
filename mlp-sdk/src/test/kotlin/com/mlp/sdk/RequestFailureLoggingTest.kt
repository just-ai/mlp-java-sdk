package com.mlp.sdk

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.mlp.gate.SimpleStatusProto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class RequestFailureLoggingTest {

    private fun capture(error: Throwable): List<ILoggingEvent> {
        val logger = LoggerFactory.getLogger("test-request-failure-${System.nanoTime()}") as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
        try {
            logRequestFailure(logger, "predict", error)
        } finally {
            logger.detachAppender(appender)
        }
        return appender.list.toList()
    }

    private fun mlpException(status: SimpleStatusProto) = MlpException(
        object : MlpErrorCode {
            override val code = "test.code"
            override val message = "test message"
            override val status = status
        }
    )

    @Test
    fun `client error status is a warning without stack trace`() {
        listOf(SimpleStatusProto.BAD_REQUEST, SimpleStatusProto.NOT_FOUND, SimpleStatusProto.TOO_MANY_REQUESTS).forEach { status ->
            val events = capture(mlpException(status))

            assertEquals(listOf(Level.WARN), events.map { it.level }, "status $status")
            assertTrue(events.single().formattedMessage.contains("test.code"), events.single().formattedMessage)
            assertNull(events.single().throwableProxy)
        }
    }

    @Test
    fun `request id is kept in the message when known`() {
        val logger = LoggerFactory.getLogger("test-request-failure-id-${System.nanoTime()}") as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
        logRequestFailure(logger, "stream predict", IllegalStateException("boom"), 42L)
        logger.detachAppender(appender)

        assertEquals("Error while processing stream predict request 42", appender.list.single().formattedMessage)
    }

    @Test
    fun `server error status stays an error`() {
        val events = capture(mlpException(SimpleStatusProto.INTERNAL_SERVER_ERROR))

        assertEquals(listOf(Level.ERROR), events.map { it.level })
    }

    @Test
    fun `unexpected exception stays an error with stack trace`() {
        val events = capture(IllegalStateException("boom"))

        assertEquals(listOf(Level.ERROR), events.map { it.level })
        assertEquals("Error while processing predict request", events.single().formattedMessage)
        assertEquals(IllegalStateException::class.java.name, events.single().throwableProxy.className)
    }
}
