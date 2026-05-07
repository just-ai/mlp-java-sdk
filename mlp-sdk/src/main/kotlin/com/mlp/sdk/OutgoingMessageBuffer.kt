package com.mlp.sdk

import com.mlp.gate.ServiceToGateProto
import com.mlp.sdk.utils.WithLogger
import java.time.Clock
import java.time.Instant
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Bounded FIFO buffer for outgoing ServiceToGateProto messages owned by [Connector].
 *
 * Holds messages that could not be delivered while the gRPC stream is broken.
 * After reconnect, the buffer is drained into the new stream.
 *
 * Behaviour:
 *  - On overflow, [enqueue] throws [OutgoingMessageBufferFullException] — back-pressure to the caller.
 *  - Each message is timestamped on enqueue. Messages older than [maxAgeMs] are dropped on the next
 *    [enqueue]/[drainTo]. This implements the "drop the buffer if we did not reconnect within the
 *    configured timeout" requirement (see MLP_RECONNECT_BUFFER_TIMEOUT_MS).
 */
class OutgoingMessageBuffer(
    private val maxSize: Int = DEFAULT_MAX_SIZE,
    private val maxAgeMs: Long = DEFAULT_MAX_AGE_MS,
    private val clock: Clock = Clock.systemUTC(),
) : WithLogger {
    private val lock = ReentrantLock()
    private val queue = ArrayDeque<TimestampedMessage>()

    fun enqueue(message: ServiceToGateProto.Builder) {
        val (sizeAfter, evicted) = lock.withLock {
            val evicted = evictStaleLocked()
            if (queue.size >= maxSize) {
                logger.error(
                    "OutgoingMessageBuffer overflow: size=${queue.size}, maxSize=$maxSize, " +
                            "maxAgeMs=$maxAgeMs. Caller will get OutgoingMessageBufferFullException; " +
                            "tune MLP_RECONNECT_BUFFER_SIZE / MLP_RECONNECT_BUFFER_TIMEOUT_MS."
                )
                throw OutgoingMessageBufferFullException(
                    "Outgoing message buffer is full (size=$maxSize)"
                )
            }
            queue.addLast(TimestampedMessage(message, clock.instant()))
            queue.size to evicted
        }
        if (evicted > 0) {
            logger.warn(
                "OutgoingMessageBuffer dropped $evicted stale message(s) on enqueue " +
                        "(older than ${maxAgeMs}ms — reconnect did not complete in time). " +
                        "Buffer size after enqueue: $sizeAfter."
            )
        }
    }

    suspend fun drainTo(send: suspend (ServiceToGateProto.Builder) -> Unit) {
        while (true) {
            val (head, evicted) = lock.withLock {
                val evicted = evictStaleLocked()
                queue.firstOrNull() to evicted
            }
            if (evicted > 0) {
                logger.warn(
                    "OutgoingMessageBuffer dropped $evicted stale message(s) during drain " +
                            "(older than ${maxAgeMs}ms — reconnect window missed)."
                )
            }
            if (head == null) break

            send(head.message)

            lock.withLock {
                if (queue.firstOrNull() === head) {
                    queue.removeFirst()
                }
            }
        }
    }

    fun clear() = lock.withLock { queue.clear() }

    fun size(): Int = lock.withLock { queue.size }

    fun isEmpty(): Boolean = lock.withLock { queue.isEmpty() }

    private fun evictStaleLocked(): Int {
        if (maxAgeMs <= 0) return 0
        val cutoff = clock.instant().minusMillis(maxAgeMs)
        var evicted = 0
        while (queue.isNotEmpty() && queue.first().enqueuedAt.isBefore(cutoff)) {
            queue.removeFirst()
            evicted++
        }
        return evicted
    }

    private data class TimestampedMessage(
        val message: ServiceToGateProto.Builder,
        val enqueuedAt: Instant,
    )

    companion object {
        const val DEFAULT_MAX_SIZE = MlpServiceConfig.RECONNECT_BUFFER_SIZE
        const val DEFAULT_MAX_AGE_MS = MlpServiceConfig.RECONNECT_BUFFER_TIMEOUT_MS

        fun create(config: MlpServiceConfig): OutgoingMessageBuffer =
            OutgoingMessageBuffer(config.reconnectBufferSize, config.reconnectBufferTimeoutMs)
    }
}

class OutgoingMessageBufferFullException(message: String) : RuntimeException(message)
