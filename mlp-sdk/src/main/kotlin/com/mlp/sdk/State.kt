package com.mlp.sdk

import com.mlp.sdk.State.Condition
import com.mlp.sdk.State.Condition.ACTIVE
import com.mlp.sdk.State.Condition.NOT_STARTED
import com.mlp.sdk.State.Condition.SHUTTING_DOWN
import com.mlp.sdk.State.Condition.SHUT_DOWN
import com.mlp.sdk.State.Condition.STARTING
import java.util.concurrent.CountDownLatch

abstract class WithState(condition: Condition = NOT_STARTED): WithExecutionContext {
    internal val state by lazy { State(this, condition, context) }
}

class State(
    private val component: Any,
    startCondition: Condition = NOT_STARTED,
    override val context: MlpExecutionContext
) : WithExecutionContext {

    private val shutdownLatch = CountDownLatch(1)

    @Volatile
    var condition = startCondition
        private set

    @Volatile
    var shutdownReason: String? = null

    val notStarted get() = condition == NOT_STARTED
    val starting get() = condition == STARTING
    val active get() = condition == ACTIVE
    val shuttingDown get() = condition == SHUTTING_DOWN
    val shutdown get() = condition == SHUT_DOWN

    fun starting() {
        check(condition == NOT_STARTED) {
            "Can't starting $component because it's in illegal state $condition (expected: $NOT_STARTED)"
        }
        logger.info("$component is starting")
        condition = STARTING
    }

    fun active() {
        val expectedStates = listOf(NOT_STARTED, STARTING)
        check(condition in expectedStates) {
            "Can't activate $component because it's in illegal state $condition (expected: $expectedStates)"
        }
        logger.info("$component is active")
        condition = ACTIVE
    }

    fun shuttingDown() {
        if (condition == SHUT_DOWN || condition == SHUTTING_DOWN) {
            return
        }
        val expectedStates = listOf(NOT_STARTED, STARTING, ACTIVE)
        check(condition in expectedStates) {
            "Can't start shutting down $component because it's in illegal state $condition (expected: $expectedStates)"
        }
        logger.info("$component is shutting down")
        condition = SHUTTING_DOWN
    }

    fun shutdown() {
        logger.info("$component is shut down")
        condition = SHUT_DOWN
        shutdownLatch.countDown()
    }

    fun awaitShutdown() {
        val expectedStates = listOf(STARTING, ACTIVE)
        check(condition in expectedStates) {
            "Can't await shutdown $component because it's in illegal state $condition (expected: $expectedStates)"
        }
        shutdownLatch.await()
    }

    enum class Condition {
        NOT_STARTED, STARTING, ACTIVE, SHUTTING_DOWN, SHUT_DOWN;
    }

    override fun toString() = condition.toString()
}

fun State.isShutdownTypeState() = shuttingDown || shutdown
