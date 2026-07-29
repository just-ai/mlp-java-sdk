package com.mlp.sdk

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BillingUnitsThreadLocalTest {

    @AfterEach
    fun cleanup() {
        // Clear all ThreadLocal variables after each test
        BillingUnitsThreadLocal.clearUnits()
        BillingUnitsThreadLocal.clearDetails()
        BillingUnitsThreadLocal.clearSelfCostUnits()
        BillingUnitsThreadLocal.clearSelfCostCurrency()
        BillingUnitsThreadLocal.clearDeferredBillingRequestId()
        BillingUnitsThreadLocal.clearRecurringBillingRequestId()
        BillingUnitsThreadLocal.clearBillingCurrencyType()
    }

    @Test
    fun `should set and get self-cost currency`() {
        BillingUnitsThreadLocal.setSelfCostCurrency("USD")
        assertEquals("USD", BillingUnitsThreadLocal.getSelfCostCurrency())
    }

    @Test
    fun `should clear self-cost currency`() {
        BillingUnitsThreadLocal.setSelfCostCurrency("USD")
        BillingUnitsThreadLocal.clearSelfCostCurrency()
        assertNull(BillingUnitsThreadLocal.getSelfCostCurrency())
    }

    @Test
    fun `clearAll should also clear self-cost currency`() {
        BillingUnitsThreadLocal.setSelfCostUnits(70L)
        BillingUnitsThreadLocal.setSelfCostCurrency("USD")
        BillingUnitsThreadLocal.clearAll()
        assertNull(BillingUnitsThreadLocal.getSelfCostUnits())
        assertNull(BillingUnitsThreadLocal.getSelfCostCurrency())
    }

    @Test
    fun `clearAll should also clear the recurring billing request id`() {
        // clearAll — единственная точка сброса между запросами в переиспользуемом потоке; забытое
        // поле утекло бы в СЛЕДУЮЩИЙ запрос и привязало бы его к чужой подписке.
        BillingUnitsThreadLocal.setRecurringBillingRequestId("sub-1")
        BillingUnitsThreadLocal.clearAll()
        assertNull(BillingUnitsThreadLocal.getRecurringBillingRequestId())
    }

    @Test
    fun `self-cost currency is independent of the client billing currency type`() {
        // Клиентская стоимость и себестоимость живут в РАЗНЫХ валютах: клиенту считаем в рублях,
        // вендор тарифицирует нас в своей. Один канал не должен подменять другой.
        BillingUnitsThreadLocal.setBillingCurrencyType("RUB")
        BillingUnitsThreadLocal.setSelfCostCurrency("USD")

        assertEquals("RUB", BillingUnitsThreadLocal.getBillingCurrencyType())
        assertEquals("USD", BillingUnitsThreadLocal.getSelfCostCurrency())
    }

    @Test
    fun `should maintain thread isolation for self-cost currency`() {
        BillingUnitsThreadLocal.setSelfCostCurrency("USD")
        val threadResults = mutableListOf<String?>()
        val thread = Thread {
            threadResults.add(BillingUnitsThreadLocal.getSelfCostCurrency())
            BillingUnitsThreadLocal.setSelfCostCurrency("RUB")
            threadResults.add(BillingUnitsThreadLocal.getSelfCostCurrency())
        }
        thread.start()
        thread.join()

        assertEquals("USD", BillingUnitsThreadLocal.getSelfCostCurrency())
        assertEquals(2, threadResults.size)
        assertNull(threadResults[0])
        assertEquals("RUB", threadResults[1])
    }

    @Test
    fun `should set and get self-cost units`() {
        BillingUnitsThreadLocal.setSelfCostUnits(42L)
        assertEquals(42L, BillingUnitsThreadLocal.getSelfCostUnits())
    }

    @Test
    fun `should clear self-cost units`() {
        BillingUnitsThreadLocal.setSelfCostUnits(42L)
        BillingUnitsThreadLocal.clearSelfCostUnits()
        assertNull(BillingUnitsThreadLocal.getSelfCostUnits())
    }

    @Test
    fun `clearAll should also clear self-cost units`() {
        BillingUnitsThreadLocal.setUnits(100L)
        BillingUnitsThreadLocal.setSelfCostUnits(70L)
        BillingUnitsThreadLocal.clearAll()
        assertNull(BillingUnitsThreadLocal.getUnits())
        assertNull(BillingUnitsThreadLocal.getSelfCostUnits())
    }

    @Test
    fun `self-cost units should not affect client units and vice versa`() {
        BillingUnitsThreadLocal.setUnits(100L)
        BillingUnitsThreadLocal.setSelfCostUnits(70L)
        assertEquals(100L, BillingUnitsThreadLocal.getUnits())
        assertEquals(70L, BillingUnitsThreadLocal.getSelfCostUnits())
    }

    @Test
    fun `should maintain thread isolation for self-cost units`() {
        BillingUnitsThreadLocal.setSelfCostUnits(11L)
        val threadResults = mutableListOf<Long?>()
        val thread = Thread {
            threadResults.add(BillingUnitsThreadLocal.getSelfCostUnits())
            BillingUnitsThreadLocal.setSelfCostUnits(22L)
            threadResults.add(BillingUnitsThreadLocal.getSelfCostUnits())
        }
        thread.start()
        thread.join()
        assertEquals(11L, BillingUnitsThreadLocal.getSelfCostUnits())
        assertEquals(2, threadResults.size)
        assertNull(threadResults[0])
        assertEquals(22L, threadResults[1])
    }

    @Test
    fun `should set and get deferred billing request id`() {
        val requestId = "test-request-id-123"
        BillingUnitsThreadLocal.setDeferredBillingRequestId(requestId)

        assertEquals(requestId, BillingUnitsThreadLocal.getDeferredBillingRequestId())
    }

    @Test
    fun `should clear deferred billing request id`() {
        BillingUnitsThreadLocal.setDeferredBillingRequestId("test-id")
        BillingUnitsThreadLocal.clearDeferredBillingRequestId()

        assertNull(BillingUnitsThreadLocal.getDeferredBillingRequestId())
    }

    @Test
    fun `should maintain thread isolation for deferred billing id`() {
        BillingUnitsThreadLocal.setDeferredBillingRequestId("main-thread-id")

        val threadResults = mutableListOf<String?>()
        val thread = Thread {
            // Should be null in another thread
            threadResults.add(BillingUnitsThreadLocal.getDeferredBillingRequestId())

            // Set value in another thread
            BillingUnitsThreadLocal.setDeferredBillingRequestId("other-thread-id")
            threadResults.add(BillingUnitsThreadLocal.getDeferredBillingRequestId())
        }

        thread.start()
        thread.join()

        // Verify that the value in main thread hasn't changed
        assertEquals("main-thread-id", BillingUnitsThreadLocal.getDeferredBillingRequestId())

        // Verify that in another thread it was null, then its own value was set
        assertEquals(2, threadResults.size)
        assertNull(threadResults[0])
        assertEquals("other-thread-id", threadResults[1])
    }

    @Test
    fun `should maintain backward compatibility with existing billing methods`() {
        // Verify that old methods still work
        BillingUnitsThreadLocal.setUnits(100L)
        assertEquals(100L, BillingUnitsThreadLocal.getUnits())

        val detailedUnits = mapOf("tokens" to 50L, "requests" to 1L)
        BillingUnitsThreadLocal.setDetailedUnits(detailedUnits)
        assertEquals(detailedUnits, BillingUnitsThreadLocal.getDetailedUnits())

        // Verify that new methods don't affect old ones
        BillingUnitsThreadLocal.setDeferredBillingRequestId("test-id")
        assertEquals(100L, BillingUnitsThreadLocal.getUnits())
        assertEquals(detailedUnits, BillingUnitsThreadLocal.getDetailedUnits())
    }

    @Test
    fun `should allow multiple deferred billing fields to coexist`() {
        val requestId = "multi-test-id"
        val polling = true

        BillingUnitsThreadLocal.setDeferredBillingRequestId(requestId)

        // Verify that all values were saved independently
        assertEquals(requestId, BillingUnitsThreadLocal.getDeferredBillingRequestId())
    }

    @Test
    fun `should set and get billing currency type`() {
        val currencyType = "TOKENS"
        BillingUnitsThreadLocal.setBillingCurrencyType(currencyType)

        assertEquals(currencyType, BillingUnitsThreadLocal.getBillingCurrencyType())
    }

    @Test
    fun `should clear billing currency type`() {
        BillingUnitsThreadLocal.setBillingCurrencyType("TOKENS")
        BillingUnitsThreadLocal.clearBillingCurrencyType()

        assertNull(BillingUnitsThreadLocal.getBillingCurrencyType())
    }

    @Test
    fun `should maintain thread isolation for billing currency type`() {
        BillingUnitsThreadLocal.setBillingCurrencyType("MAIN_TOKENS")

        val threadResults = mutableListOf<String?>()
        val thread = Thread {
            // Should be null in another thread
            threadResults.add(BillingUnitsThreadLocal.getBillingCurrencyType())

            // Set value in another thread
            BillingUnitsThreadLocal.setBillingCurrencyType("OTHER_TOKENS")
            threadResults.add(BillingUnitsThreadLocal.getBillingCurrencyType())
        }

        thread.start()
        thread.join()

        // Verify that the value in main thread hasn't changed
        assertEquals("MAIN_TOKENS", BillingUnitsThreadLocal.getBillingCurrencyType())

        // Verify that in another thread it was null, then its own value was set
        assertEquals(2, threadResults.size)
        assertNull(threadResults[0])
        assertEquals("OTHER_TOKENS", threadResults[1])
    }

    @Test
    fun `should clear all billing fields including currency type`() {
        BillingUnitsThreadLocal.setUnits(100L)
        BillingUnitsThreadLocal.setDetailedUnits(mapOf("tokens" to 50L))
        BillingUnitsThreadLocal.setDeferredBillingRequestId("test-id")
        BillingUnitsThreadLocal.setBillingCurrencyType("TOKENS")

        BillingUnitsThreadLocal.clearAll()

        assertNull(BillingUnitsThreadLocal.getUnits())
        assertNull(BillingUnitsThreadLocal.getDetailedUnits())
        assertNull(BillingUnitsThreadLocal.getDeferredBillingRequestId())
        assertNull(BillingUnitsThreadLocal.getBillingCurrencyType())
    }

    @Test
    fun `billing currency type should not affect other billing fields`() {
        BillingUnitsThreadLocal.setUnits(100L)
        BillingUnitsThreadLocal.setDetailedUnits(mapOf("tokens" to 50L, "requests" to 1L))
        BillingUnitsThreadLocal.setDeferredBillingRequestId("test-id")
        BillingUnitsThreadLocal.setBillingCurrencyType("CUSTOM_TOKENS")

        assertEquals(100L, BillingUnitsThreadLocal.getUnits())
        assertEquals(mapOf("tokens" to 50L, "requests" to 1L), BillingUnitsThreadLocal.getDetailedUnits())
        assertEquals("test-id", BillingUnitsThreadLocal.getDeferredBillingRequestId())
        assertEquals("CUSTOM_TOKENS", BillingUnitsThreadLocal.getBillingCurrencyType())
    }
}
