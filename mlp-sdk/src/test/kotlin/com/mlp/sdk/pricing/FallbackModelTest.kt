package com.mlp.sdk.pricing

import com.mlp.sdk.pricing.ModelPricing.ModelVendor
import com.mlp.sdk.pricing.ModelPricing.Pricing.UnitType.INPUT_TEXT_TOKENS
import com.mlp.sdk.pricing.ModelPricing.Pricing.UnitType.OUTPUT_TEXT_TOKENS
import java.math.BigDecimal
import java.util.Currency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FallbackModelTest {

    private val usd = Currency.getInstance("USD")
    private val eur = Currency.getInstance("EUR")

    @Test
    fun `fallback model is used when specific model not found`() {
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "gpt-4",
                    defaultSnapshot = null,
                    modelVendor = ModelVendor.OPENAI,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.03")
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
                            basePrice = BigDecimal("0.01")
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = true
                )
            ),
            exchangeRates = emptyList()
        )
        
        val calculator = CostCalculator(config)
        
        val result = calculator.computeUnitCostOrThrow("unknown-model", 1000L, INPUT_TEXT_TOKENS)
        
        assertEquals(BigDecimal("0.01"), result.amount)
        assertEquals(usd, result.currency)
    }

    @Test
    fun `highest price fallback model is selected when multiple exist`() {
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "fallback-cheap",
                    defaultSnapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.01")
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = true
                ),
                ModelPricing(
                    model = "fallback-expensive",
                    defaultSnapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.05")
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = true
                )
            ),
            exchangeRates = emptyList()
        )
        
        val calculator = CostCalculator(config)
        
        val result = calculator.retrievePricingOrThrow("unknown-model")
        
        assertEquals("fallback-cheap", result.model)
        assertTrue(result.isDefaultPricing)
    }

    @Test
    fun `fallback model selection when no regular models exist`() {
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "fallback-only",
                    defaultSnapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.01")
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = true
                )
            ),
            exchangeRates = emptyList()
        )
        
        val calculator = CostCalculator(config)
        
        val result = calculator.computeUnitCostOrThrow("any-model", 1000L, INPUT_TEXT_TOKENS)
        
        assertEquals(BigDecimal("0.01"), result.amount)
        assertEquals("fallback-only", calculator.retrievePricingOrThrow("any-model").model)
    }

    @Test
    fun `fallback model selection with multiple fallbacks different currencies`() {
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "fallback-usd",
                    defaultSnapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.01")
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = true
                ),
                ModelPricing(
                    model = "fallback-eur",
                    defaultSnapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.02")
                        )
                    ),
                    currency = eur,
                    isDefaultPricing = true
                )
            ),
            exchangeRates = emptyList()
        )
        
        val calculator = CostCalculator(config)
        
        val result = calculator.retrievePricingOrThrow("unknown-model")
        
        assertTrue(result.isDefaultPricing)
        assertEquals("fallback-usd", result.model)
    }

    @Test
    fun `fallback model with missing pricing unit throws exception`() {
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "fallback-model",
                    defaultSnapshot = null,
                    modelVendor = null,
                    pricing = listOf(
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
            exchangeRates = emptyList()
        )
        
        val calculator = CostCalculator(config)
        
        assertThrows(NoSuchElementException::class.java) {
            calculator.computeUnitCostOrThrow("unknown-model", 1000L, INPUT_TEXT_TOKENS)
        }
    }

    @Test
    fun `fallback model with different modelVendor inheritance`() {
        val config = PricingConfiguration(
            currency = usd,
            modelVendor = ModelVendor.OPENAI,
            modelsPricing = listOf(
                ModelPricing(
                    model = "fallback-model",
                    defaultSnapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.01")
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = true
                )
            ),
            exchangeRates = emptyList()
        )
        
        val modelsProvider = ModelsProvider(config)

        val openaiModels = modelsProvider.modelsList(ModelVendor.OPENAI)
        val anthropicModels = modelsProvider.modelsList(ModelVendor.ANTHROPIC)
        
        assertTrue(openaiModels.contains("fallback-model"))
        assertFalse(anthropicModels.contains("fallback-model"))
    }

    @Test
    fun `fallback model used when exact model exists but different vendor filter`() {
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "gpt-4",
                    defaultSnapshot = null,
                    modelVendor = ModelVendor.OPENAI,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.03")
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
                            basePrice = BigDecimal("0.01")
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = true
                )
            ),
            exchangeRates = emptyList()
        )
        
        val modelsProvider = ModelsProvider(config)
        
        val anthropicModels = modelsProvider.modelsList(ModelVendor.ANTHROPIC)
        
        assertEquals(listOf("fallback-model"), anthropicModels)
    }

    @Test
    fun `fallback model with complex pricing tiers`() {
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "fallback-complex",
                    defaultSnapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.01"),
                            overrides = ModelPricing.Overrides(
                                volumePricing = listOf(
                                    VolumePricing(
                                        price = BigDecimal("0.008"),
                                        unitsRange = VolumePricing.UnitsRange(from = 100000L, to = null)
                                    )
                                )
                            )
                        ),
                        ModelPricing.Pricing(
                            unit = OUTPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.02")
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = true
                )
            ),
            exchangeRates = emptyList()
        )
        
        val calculator = CostCalculator(config)
        
        val smallVolumeResult = calculator.computeUnitCostOrThrow("unknown-model", 10000L, INPUT_TEXT_TOKENS)
        val largeVolumeResult = calculator.computeUnitCostOrThrow("unknown-model", 200000L, INPUT_TEXT_TOKENS)
        val outputResult = calculator.computeUnitCostOrThrow("unknown-model", 5000L, OUTPUT_TEXT_TOKENS)
        
        assertEquals(BigDecimal("0.1"), smallVolumeResult.amount)
        assertEquals(BigDecimal("1.6"), largeVolumeResult.amount)
        assertEquals(BigDecimal("0.1"), outputResult.amount)
    }

    @Test
    fun `fallback model priority with mixed fallback flags`() {
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "normal-model",
                    defaultSnapshot = null,
                    modelVendor = ModelVendor.OPENAI,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.03")
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = false
                ),
                ModelPricing(
                    model = "fallback-first",
                    defaultSnapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.01")
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = true
                ),
                ModelPricing(
                    model = "normal-model-2",
                    defaultSnapshot = null,
                    modelVendor = ModelVendor.ANTHROPIC,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.04")
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = false
                ),
                ModelPricing(
                    model = "fallback-second",
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
            exchangeRates = emptyList()
        )
        
        val calculator = CostCalculator(config)
        
        val result = calculator.retrievePricingOrThrow("unknown-model")
        
        assertEquals("fallback-first", result.model)
        assertTrue(result.isDefaultPricing)
    }
}
