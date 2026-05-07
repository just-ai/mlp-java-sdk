package com.mlp.sdk

import com.mlp.gate.ServiceToGateProto
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OutgoingMessageBufferTest {

    @Test
    fun `should maintain FIFO order`() {
        val buffer = OutgoingMessageBuffer(maxSize = 10)
        val msg1 = ServiceToGateProto.newBuilder().setRequestId(1)
        val msg2 = ServiceToGateProto.newBuilder().setRequestId(2)
        val msg3 = ServiceToGateProto.newBuilder().setRequestId(3)

        buffer.enqueue(msg1)
        buffer.enqueue(msg2)
        buffer.enqueue(msg3)

        val drained = mutableListOf<Long>()
        runBlocking { buffer.drainTo { drained.add(it.requestId) } }

        assertEquals(listOf(1L, 2L, 3L), drained)
    }

    @Test
    fun `should throw when full`() {
        val buffer = OutgoingMessageBuffer(maxSize = 2)
        val msg1 = ServiceToGateProto.newBuilder().setRequestId(1)
        val msg2 = ServiceToGateProto.newBuilder().setRequestId(2)
        val msg3 = ServiceToGateProto.newBuilder().setRequestId(3)

        buffer.enqueue(msg1)
        buffer.enqueue(msg2)
        assertThrows(OutgoingMessageBufferFullException::class.java) {
            buffer.enqueue(msg3)
        }

        val drained = mutableListOf<Long>()
        runBlocking { buffer.drainTo { drained.add(it.requestId) } }
        assertEquals(listOf(1L, 2L), drained)
    }

    @Test
    fun `should clear all messages`() {
        val buffer = OutgoingMessageBuffer()
        buffer.enqueue(ServiceToGateProto.newBuilder().setRequestId(1))
        buffer.clear()
        assertTrue(buffer.isEmpty())
    }

    @Test
    fun `should evict messages older than maxAgeMs on enqueue`() {
        val clock = MutableClock(Instant.parse("2024-01-01T00:00:00Z"))
        val buffer = OutgoingMessageBuffer(maxSize = 10, maxAgeMs = 60_000, clock = clock)

        buffer.enqueue(ServiceToGateProto.newBuilder().setRequestId(1))
        buffer.enqueue(ServiceToGateProto.newBuilder().setRequestId(2))
        clock.advanceMillis(61_000)
        buffer.enqueue(ServiceToGateProto.newBuilder().setRequestId(3))

        val drained = mutableListOf<Long>()
        runBlocking { buffer.drainTo { drained.add(it.requestId) } }
        assertEquals(listOf(3L), drained)
    }

    @Test
    fun `should evict stale messages on drain`() {
        val clock = MutableClock(Instant.parse("2024-01-01T00:00:00Z"))
        val buffer = OutgoingMessageBuffer(maxSize = 10, maxAgeMs = 60_000, clock = clock)

        buffer.enqueue(ServiceToGateProto.newBuilder().setRequestId(1))
        clock.advanceMillis(61_000)

        val drained = mutableListOf<Long>()
        runBlocking { buffer.drainTo { drained.add(it.requestId) } }
        assertEquals(emptyList<Long>(), drained)
        assertTrue(buffer.isEmpty())
    }

    @Test
    fun `should not throw when stale eviction frees space`() {
        val clock = MutableClock(Instant.parse("2024-01-01T00:00:00Z"))
        val buffer = OutgoingMessageBuffer(maxSize = 1, maxAgeMs = 60_000, clock = clock)

        buffer.enqueue(ServiceToGateProto.newBuilder().setRequestId(1))
        clock.advanceMillis(61_000)
        buffer.enqueue(ServiceToGateProto.newBuilder().setRequestId(2))

        val drained = mutableListOf<Long>()
        runBlocking { buffer.drainTo { drained.add(it.requestId) } }
        assertEquals(listOf(2L), drained)
    }

    @Test
    fun `should keep messages on drain when send throws`() {
        val buffer = OutgoingMessageBuffer(maxSize = 10)
        buffer.enqueue(ServiceToGateProto.newBuilder().setRequestId(1))
        buffer.enqueue(ServiceToGateProto.newBuilder().setRequestId(2))

        assertThrows(IllegalStateException::class.java) {
            runBlocking { buffer.drainTo { throw IllegalStateException("stream broken") } }
        }
        assertEquals(2, buffer.size())
    }

    private class MutableClock(private var instant: Instant) : Clock() {
        fun advanceMillis(ms: Long) {
            instant = instant.plusMillis(ms)
        }

        override fun instant(): Instant = instant
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
    }
}
