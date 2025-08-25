package com.mlp.sdk.pricing

import com.mlp.sdk.pricing.ModelPricing.ModelVendor
import com.mlp.sdk.pricing.ModelPricing.Pricing.UnitType.CACHED_INPUT_TEXT_TOKENS
import com.mlp.sdk.pricing.ModelPricing.Pricing.UnitType.INPUT_TEXT_TOKENS
import com.mlp.sdk.pricing.ModelPricing.Pricing.UnitType.OUTPUT_TEXT_TOKENS
import java.math.BigDecimal
import java.util.Currency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Main integration tests for CostCalculator functionality.
 * 
 * Specific feature tests are now organized in separate test files:
 * - CostDataClassTest: Tests for Cost data class operations
 * - PricingSearchTest: Tests for model/pricing search functionality
 * - CostCalculationTest: Tests for cost calculation logic
 * - VolumePricingTest: Tests for volume pricing features
 * - FallbackModelTest: Tests for fallback model selection
 * - CurrencyConversionTest: Tests for currency conversion features
 * - ConfigurationRequirementsTest: Tests for TDD requirements from PricingConfiguration
 */
class CostCalculatorTest {

    private val usd = Currency.getInstance("USD")
    private val eur = Currency.getInstance("EUR")

    @Test
    fun `end-to-end chat completion cost calculation with all features`() {
        val config = createCompleteTestConfiguration()
        val calculator = CostCalculator(config)
        
        
        val regularResult = calculator.computeCompletionRequestCost("gpt-4", 150000 as Int, 5000, 2000)
        
        
        val expectedInputCost = BigDecimal("3.75") 
        val expectedOutputCost = BigDecimal("0.30") 
        val expectedCachedCost = BigDecimal("0.03") 
        val expectedTotal = expectedInputCost + expectedOutputCost + expectedCachedCost
        
        assertEquals(expectedTotal, regularResult.amount)
        assertEquals(usd, regularResult.currency)
    }

    @Test
    fun `end-to-end fallback model usage with currency conversion`() {
        val config = createCompleteTestConfiguration()
        val calculator = CostCalculator(config)
        
        
        val fallbackResult = calculator.computeCompletionRequestCost("unknown-model", 5000 as Int, 2000, null)
        
        val expectedInputCost = BigDecimal("0.025") 
        val expectedOutputCost = BigDecimal("0.02") 
        val expectedTotal = expectedInputCost + expectedOutputCost
        
        assertEquals(expectedTotal, fallbackResult.amount)
        assertEquals(usd, fallbackResult.currency)
    }

    @Test
    fun `end-to-end model filtering by vendor`() {
        val config = createCompleteTestConfiguration()
        val modelsProvider = ModelsProvider(config)
        
        val anthropicModels = modelsProvider.modelsList(ModelVendor.ANTHROPIC)
        val metaModels = modelsProvider.modelsList(ModelVendor.META)
        val unknownVendorModels = modelsProvider.modelsList(ModelVendor.YANDEX)
        
        assertEquals(listOf("gpt-4", "fallback-model"), anthropicModels)
        assertEquals(listOf("llama-2", "fallback-model"), metaModels)
        assertEquals(listOf("fallback-model"), unknownVendorModels)
    }

    @Test
    fun `end-to-end mixed currency scenario`() {
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "eur-model",
                    defaultSnapshot = null,
                    modelVendor = ModelVendor.OPENAI,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.05")
                        )
                    ),
                    currency = eur,
                    isDefaultPricing = false
                ),
                ModelPricing(
                    model = "usd-fallback",
                    defaultSnapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.02")
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = true
                )
            ),
            exchangeRates = listOf(
                ExchangeRate(from = eur, to = usd, rate = BigDecimal("1.1"))
            )
        )
        
        val calculator = CostCalculator(config)
        val eurResult = calculator.computeUnitCostOrThrow("eur-model", 1000L, INPUT_TEXT_TOKENS)
        val unknownResult = calculator.computeUnitCostOrThrow("unknown-model", 1000L, INPUT_TEXT_TOKENS)
        
        
        assertEquals(eur, eurResult.currency)
        assertEquals(BigDecimal("0.05"), eurResult.amount)
        
        
        assertEquals(usd, unknownResult.currency)
        assertEquals(BigDecimal("0.02"), unknownResult.amount)
    }

    @Test
    fun `integration test with complex volume pricing and multiple models`() {
        val config = createCompleteTestConfiguration()
        val calculator = CostCalculator(config)
        
        
        val smallVolume = calculator.computeUnitCostOrThrow("gpt-4", 50000L, INPUT_TEXT_TOKENS) 
        val mediumVolume = calculator.computeUnitCostOrThrow("gpt-4", 200000L, INPUT_TEXT_TOKENS) 
        val largeVolume = calculator.computeUnitCostOrThrow("gpt-4", 600000L, INPUT_TEXT_TOKENS) 
        
        assertEquals(BigDecimal("1.5"), smallVolume.amount) 
        assertEquals(BigDecimal("5"), mediumVolume.amount) 
        assertEquals(BigDecimal("12"), largeVolume.amount) 
    }

    @Test
    fun `integration test with all unit types`() {
        val config = createCompleteTestConfiguration()
        val calculator = CostCalculator(config)
        
        val inputCost = calculator.computeUnitCostOrThrow("gpt-4", 1000L, INPUT_TEXT_TOKENS)
        val outputCost = calculator.computeUnitCostOrThrow("gpt-4", 1000L, OUTPUT_TEXT_TOKENS)
        val cachedCost = calculator.computeUnitCostOrThrow("gpt-4", 1000L, CACHED_INPUT_TEXT_TOKENS)
        
        assertEquals(BigDecimal("0.03"), inputCost.amount)
        assertEquals(BigDecimal("0.06"), outputCost.amount)
        assertEquals(BigDecimal("0.015"), cachedCost.amount)
        
        
        assertEquals(usd, inputCost.currency)
        assertEquals(usd, outputCost.currency)
        assertEquals(usd, cachedCost.currency)
    }

    private fun createCompleteTestConfiguration(): PricingConfiguration {
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
                            basePrice = BigDecimal("0.03"),
                            overrides = ModelPricing.Overrides(
                                volumePricing = listOf(
                                    VolumePricing(
                                        price = BigDecimal("0.025"),
                                        unitsRange = VolumePricing.UnitsRange(from = 100000L, to = 500000L)
                                    ),
                                    VolumePricing(
                                        price = BigDecimal("0.02"),
                                        unitsRange = VolumePricing.UnitsRange(from = 500000L, to = null)
                                    )
                                )
                            )
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
                ),
                ModelPricing(
                    model = "fallback-model",
                    defaultSnapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.005")
                        ),
                        ModelPricing.Pricing(
                            unit = OUTPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.01")
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = true
                )
            ),
            exchangeRates = listOf(
                ExchangeRate(from = eur, to = usd, rate = BigDecimal("1.1"))
            )
        )
    }
}
