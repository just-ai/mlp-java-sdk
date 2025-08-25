package com.mlp.sdk.pricing

import com.mlp.sdk.pricing.ModelPricing.ModelVendor
import com.mlp.sdk.pricing.ModelPricing.Pricing.UnitType.CACHED_INPUT_TEXT_TOKENS
import com.mlp.sdk.pricing.ModelPricing.Pricing.UnitType.EMBEDDING_TOKENS
import com.mlp.sdk.pricing.ModelPricing.Pricing.UnitType.INPUT_CONTEXT_TOKENS
import com.mlp.sdk.pricing.ModelPricing.Pricing.UnitType.INPUT_TEXT_TOKENS
import com.mlp.sdk.pricing.ModelPricing.Pricing.UnitType.OUTPUT_TEXT_TOKENS
import com.mlp.sdk.pricing.ModelPricing.Pricing.UnitType.UNKNOWN
import java.math.BigDecimal
import java.util.Currency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class CostCalculationTest {

    private val usd = Currency.getInstance("USD")

    @Test
    fun `getChatCompletionTotalCost with Int parameters`() {
        val config = createTestPricingConfiguration()
        val calculator = CostCalculator(config)
        
        val result = calculator.computeCompletionRequestCost("gpt-4", 5000 as Int, 2000, 1000)
        
        val expectedInputCost = BigDecimal("0.15")
        val expectedOutputCost = BigDecimal("0.12")
        val expectedCachedCost = BigDecimal("0.015")
        val expectedTotal = expectedInputCost + expectedOutputCost + expectedCachedCost
        
        assertEquals(expectedTotal, result.amount)
        assertEquals(usd, result.currency)
    }

    @Test
    fun `getChatCompletionTotalCost with Long parameters and no cached tokens`() {
        val config = createTestPricingConfiguration()
        val calculator = CostCalculator(config)
        
        val result = calculator.computeCompletionRequestCost("gpt-4", 5000L, 2000L, null)
        
        val expectedInputCost = BigDecimal("0.15")
        val expectedOutputCost = BigDecimal("0.12")
        val expectedTotal = expectedInputCost + expectedOutputCost
        
        assertEquals(expectedTotal, result.amount)
        assertEquals(usd, result.currency)
    }

    @Test
    fun `cost function - calculates basic cost correctly`() {
        val config = createTestPricingConfiguration()
        val calculator = CostCalculator(config)
        
        val result = calculator.computeUnitCostOrThrow("gpt-4", 5000L, INPUT_TEXT_TOKENS)
        
        val expected = BigDecimal("0.15")
        assertEquals(expected, result.amount)
        assertEquals(usd, result.currency)
    }

    @Test
    fun `cost function - throws exception for unknown unit type`() {
        val config = createTestPricingConfiguration()
        val calculator = CostCalculator(config)
        
        assertThrows(NoSuchElementException::class.java) {
            calculator.computeUnitCostOrThrow("gpt-4", 5000L, UNKNOWN)
        }
    }

    @Test
    fun `costOrZero - returns cost when unit type exists`() {
        val config = createTestPricingConfiguration()
        val calculator = CostCalculator(config)
        
        val result = calculator.computeUnitCostOrZero("gpt-4", 5000L, INPUT_TEXT_TOKENS)
        
        val expected = BigDecimal("0.15")
        assertEquals(expected, result.amount)
        assertEquals(usd, result.currency)
    }

    @Test
    fun `costOrZero - returns zero when unit type does not exist`() {
        val config = createTestPricingConfiguration()
        val calculator = CostCalculator(config)
        
        val result = calculator.computeUnitCostOrZero("llama-2", 5000L, CACHED_INPUT_TEXT_TOKENS)
        
        assertEquals(BigDecimal.ZERO, result.amount)
        assertEquals(usd, result.currency)
    }

    @Test
    fun `costOrZero - returns zero when units is null`() {
        val config = createTestPricingConfiguration()
        val calculator = CostCalculator(config)
        
        val result = calculator.computeUnitCostOrZero("gpt-4", null, INPUT_TEXT_TOKENS)
        
        assertEquals(BigDecimal.ZERO, result.amount)
        assertEquals(usd, result.currency)
    }

    @Test
    fun `cost calculation works correctly with different perUnit values`() {
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "test-model-1k",
                    defaultSnapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("1.0")
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = false
                ),
                ModelPricing(
                    model = "test-model-100k",
                    defaultSnapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 100000L,
                            basePrice = BigDecimal("50.0")
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = emptyList()
        )
        
        val calculator = CostCalculator(config)
        
        val result1k = calculator.computeUnitCostOrThrow("test-model-1k", 5000L, INPUT_TEXT_TOKENS)
        val result100k = calculator.computeUnitCostOrThrow("test-model-100k", 5000L, INPUT_TEXT_TOKENS)
        
        assertEquals(BigDecimal("5"), result1k.amount)
        assertEquals(BigDecimal("2.5"), result100k.amount)
    }

    @Test
    fun `compatibleUnits are used when requested unit not found`() {
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "test-model",
                    defaultSnapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.03"),
                            compatibleUnits = listOf(INPUT_CONTEXT_TOKENS, EMBEDDING_TOKENS)
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = emptyList()
        )
        
        val calculator = CostCalculator(config)
        
        assertThrows(NoSuchElementException::class.java) {
            calculator.computeUnitCostOrThrow("test-model", 1000L, INPUT_CONTEXT_TOKENS)
        }
    }

    @Test
    fun `compatibleUnits multiple levels of compatibility`() {
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "test-model",
                    defaultSnapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.03"),
                            compatibleUnits = listOf(INPUT_CONTEXT_TOKENS)
                        ),
                        ModelPricing.Pricing(
                            unit = INPUT_CONTEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.025"),
                            compatibleUnits = listOf(EMBEDDING_TOKENS)
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = emptyList()
        )
        
        val calculator = CostCalculator(config)
        
        val result1 = calculator.computeUnitCostOrThrow("test-model", 1000L, INPUT_CONTEXT_TOKENS)
        assertEquals(BigDecimal("0.025"), result1.amount)
        
        assertThrows(NoSuchElementException::class.java) {
            calculator.computeUnitCostOrThrow("test-model", 1000L, EMBEDDING_TOKENS)
        }
    }

    @Test
    fun `cost calculation with zero units`() {
        val config = createTestPricingConfiguration()
        val calculator = CostCalculator(config)
        
        val result = calculator.computeUnitCostOrThrow("gpt-4", 0L, INPUT_TEXT_TOKENS)
        
        assertEquals(BigDecimal.ZERO.toDouble(), result.amount.toDouble())
        assertEquals(usd, result.currency)
    }

    @Test
    fun `cost calculation with very large units`() {
        val config = createTestPricingConfiguration()
        val calculator = CostCalculator(config)
        
        val result = calculator.computeUnitCostOrThrow("gpt-4", 1_000_000L, INPUT_TEXT_TOKENS)
        
        val expected = BigDecimal("30.0")
        assertEquals(expected.toDouble(), result.amount.toDouble())
        assertEquals(usd, result.currency)
    }

    @Test
    fun `getChatCompletionTotalCost with all zero tokens`() {
        val config = createTestPricingConfiguration()
        val calculator = CostCalculator(config)
        
        val result = calculator.computeCompletionRequestCost("gpt-4", 0L, 0L, 0L)
        
        assertEquals(BigDecimal.ZERO, result.amount)
        assertEquals(usd, result.currency)
    }

    @Test
    fun `getChatCompletionTotalCost with only cached tokens`() {
        val config = createTestPricingConfiguration()
        val calculator = CostCalculator(config)
        
        val result = calculator.computeCompletionRequestCost("gpt-4", 0L, 0L, 1000L)
        
        assertEquals(BigDecimal("0.015"), result.amount)
        assertEquals(usd, result.currency)
    }

    private fun createTestPricingConfiguration(): PricingConfiguration {
        return PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "gpt-4",
                    defaultSnapshot = null,
                    modelVendor = ModelVendor.ANTHROPIC,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.03")
                        ),
                        ModelPricing.Pricing(
                            unit = OUTPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.06")
                        ),
                        ModelPricing.Pricing(
                            unit = CACHED_INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.015")
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = false
                ),
                ModelPricing(
                    model = "llama-2",
                    defaultSnapshot = null,
                    modelVendor = ModelVendor.META,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.01")
                        ),
                        ModelPricing.Pricing(
                            unit = OUTPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.02")
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = emptyList()
        )
    }
}
