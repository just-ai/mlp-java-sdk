package com.mlp.sdk.pricing

import java.math.BigDecimal
import java.util.Currency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CostDataClassTest {

    private val usd = Currency.getInstance("USD")
    private val eur = Currency.getInstance("EUR")

    @Test
    fun `Cost data class - plus operation with same currency`() {
        val cost1 = Cost(BigDecimal("10.50"), usd)
        val cost2 = Cost(BigDecimal("5.25"), usd)
        
        val result = cost1 + cost2
        
        assertEquals(BigDecimal("15.75"), result.amount)
        assertEquals(usd, result.currency)
    }

    @Test
    fun `Cost data class - plus operation with different currencies throws exception`() {
        val cost1 = Cost(BigDecimal("10.50"), usd)
        val cost2 = Cost(BigDecimal("5.25"), eur)
        
        val exception = assertThrows(IllegalArgumentException::class.java) {
            cost1 + cost2
        }
        
        assertTrue(exception.message!!.contains("Cannot add costs with different currencies"))
    }

    @Test
    fun `Cost data class - minus operation with same currency`() {
        val cost1 = Cost(BigDecimal("10.50"), usd)
        val cost2 = Cost(BigDecimal("5.25"), usd)
        
        val result = cost1 - cost2
        
        assertEquals(BigDecimal("5.25"), result.amount)
        assertEquals(usd, result.currency)
    }

    @Test
    fun `Cost data class - minus operation with different currencies throws exception`() {
        val cost1 = Cost(BigDecimal("10.50"), usd)
        val cost2 = Cost(BigDecimal("5.25"), eur)
        
        val exception = assertThrows(IllegalArgumentException::class.java) {
            cost1 - cost2
        }
        
        assertTrue(exception.message!!.contains("Cannot subtract costs with different currencies"))
    }

    @Test
    fun `Cost data class - convertToMicroFormat`() {
        val cost = Cost(BigDecimal("10.50"), usd)
        
        val result = cost.convertToMicroFormat()
        
        assertEquals(10_500_000L, result)
    }

    @Test
    fun `Cost convertToMicroFormat handles zero amount`() {
        val cost = Cost(BigDecimal.ZERO, usd)
        
        val result = cost.convertToMicroFormat()
        
        assertEquals(0L, result)
    }

    @Test
    fun `Cost convertToMicroFormat handles fractional amounts`() {
        val cost = Cost(BigDecimal("0.000001"), usd)
        
        val result = cost.convertToMicroFormat()
        
        assertEquals(1L, result)
    }

    @Test
    fun `Cost companion object - zero creates zero cost with specified currency`() {
        val result = Cost.zero(eur)
        
        assertEquals(BigDecimal.ZERO, result.amount)
        assertEquals(eur, result.currency)
    }

    @Test
    fun `Cost companion object - zero creates zero cost with null currency`() {
        val result = Cost.zero(null)
        
        assertEquals(BigDecimal.ZERO, result.amount)
        assertNull(result.currency)
    }

    @Test
    fun `MICRO_MULTIPLIER constant has correct value`() {
        assertEquals(1_000_000L, MICRO_MULTIPLIER.toLong())
    }

    @Test
    fun `Cost operations with null currency`() {
        val cost1 = Cost(BigDecimal("10.0"), null)
        val cost2 = Cost(BigDecimal("5.0"), null)
        
        val resultPlus = cost1 + cost2
        val resultMinus = cost1 - cost2
        
        assertEquals(BigDecimal("15.0"), resultPlus.amount)
        assertNull(resultPlus.currency)
        
        assertEquals(BigDecimal("5.0"), resultMinus.amount)
        assertNull(resultMinus.currency)
    }

    @Test
    fun `Cost operations mixing null and non-null currencies throws exception`() {
        val cost1 = Cost(BigDecimal("10.0"), usd)
        val cost2 = Cost(BigDecimal("5.0"), null)
        
        assertThrows(IllegalArgumentException::class.java) {
            cost1 + cost2
        }
        
        assertThrows(IllegalArgumentException::class.java) {
            cost1 - cost2
        }
    }

    @Test
    fun `Cost with large amounts`() {
        val cost = Cost(BigDecimal("999999999.999999"), usd)
        
        val result = cost.convertToMicroFormat()
        
        assertEquals(999999999999999L, result)
    }

    @Test
    fun `Cost with negative amounts`() {
        val cost = Cost(BigDecimal("-10.50"), usd)
        
        val result = cost.convertToMicroFormat()
        
        assertEquals(-10_500_000L, result)
    }
}
