package com.mpl.sdk

import com.mpl.sdk.State.Condition
import com.mpl.sdk.State.Condition.ACTIVE
import com.mpl.sdk.State.Condition.NOT_STARTED
import com.mpl.sdk.State.Condition.SHUTTING_DOWN
import com.mpl.sdk.State.Condition.SHUT_DOWN
import com.mpl.sdk.State.Condition.STARTING
import com.mpl.sdk.utils.WithLogger
import java.util.concurrent.CountDownLatch

abstract class WithState(condition: Condition = NOT_STARTED) {
    internal val state = State("$this", condition)
}

class State(val componentName: String, startCondition: Condition = NOT_STARTED) : WithLogger {

    private val shutdownLatch = CountDownLatch(1)

    @Volatile
    var condition = startCondition
        private set

    val notStarted get() = condition == NOT_STARTED
    val starting get() = condition == STARTING
    val active get() = condition == ACTIVE
    val shuttingDown get() = condition == SHUTTING_DOWN
    val shutdown get() = condition == SHUT_DOWN

    fun starting() {
        check(condition == NOT_STARTED) {
            "Can't starting $componentName because it's in illegal state $condition (expected: $NOT_STARTED)"
        }
        logger.info("$componentName is starting")
        condition = STARTING
    }

    fun active() {
        val expectedStates = listOf(NOT_STARTED, STARTING)
        check(condition in expectedStates) {
            "Can't activate $componentName because it's in illegal state $condition (expected: $expectedStates)"
        }
        logger.info("$componentName is active")
        condition = ACTIVE
    }

    fun shuttingDown() {
        val expectedStates = listOf(NOT_STARTED, STARTING, ACTIVE)
        check(condition in expectedStates) {
            "Can't start shutting down $componentName because it's in illegal state $condition (expected: $expectedStates)"
        }
        logger.info("$componentName is shutting down")
        condition = SHUTTING_DOWN
    }

    fun shutdown() {
        logger.info("$componentName is shut down")
        condition = SHUT_DOWN
        shutdownLatch.countDown()
    }

    fun awaitShutdown() {
        val expectedStates = listOf(STARTING, ACTIVE)
        check(condition in expectedStates) {
            "Can't await shutdown $componentName because it's in illegal state $condition (expected: $expectedStates)"
        }
        shutdownLatch.await()
    }

    enum class Condition {
        NOT_STARTED, STARTING, ACTIVE, SHUTTING_DOWN, SHUT_DOWN;
    }

    override fun toString() = condition.toString()
}

fun State.isShutdownTypeState() = shuttingDown || shutdown
