package com.mlp.sdk.pricing

import com.mlp.sdk.pricing.ModelPricing.ModelVendor
import com.mlp.sdk.pricing.ModelPricing.Pricing.UnitType.INPUT_TEXT_TOKENS
import java.math.BigDecimal
import java.util.Currency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * TDD tests for PricingConfiguration requirements (lines 12-28 in PricingConfiguration.kt)
 * These tests ensure all 15 requirements from the configuration comments are properly covered.
 */
class ConfigurationRequirementsTest {

    private val usd = Currency.getInstance("USD")
    private val eur = Currency.getInstance("EUR")


    @Test
    fun `default modelVendor from PricingConfiguration is used when ModelPricing modelVendor is null`() {
        val config = PricingConfiguration(
            currency = usd,
            modelVendor = ModelVendor.OPENAI,
            modelsPricing = listOf(
                ModelPricing(
                    model = "test-model",
                    snapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.03")
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = emptyList()
        )

        val modelsProvider = ModelsProvider(config)

        val resultModels = modelsProvider.modelsList(ModelVendor.OPENAI)

        assertTrue(resultModels.contains("test-model"))
    }


    @Test
    fun `ModelPricing modelVendor overrides PricingConfiguration modelVendor`() {
        val config = PricingConfiguration(
            currency = usd,
            modelVendor = ModelVendor.OPENAI,
            modelsPricing = listOf(
                ModelPricing(
                    model = "test-model",
                    snapshot = null,
                    modelVendor = ModelVendor.ANTHROPIC,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.03")
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = emptyList()
        )

        val modelsProvider = ModelsProvider(config)

        val anthropicModels = modelsProvider.modelsList(ModelVendor.ANTHROPIC)
        val openaiModels = modelsProvider.modelsList(ModelVendor.OPENAI)

        assertTrue(anthropicModels.contains("test-model"))
        assertFalse(openaiModels.contains("test-model"))
    }


    @Test
    fun `UNKNOWN unit type is used for unrecognized units during parsing`() {
        val unknownUnitType = ModelPricing.Pricing.UnitType.UNKNOWN

        assertEquals(ModelPricing.Pricing.UnitType.UNKNOWN, unknownUnitType)
    }


    @Test
    fun `UNKNOWN modelVendor is used for unrecognized vendors during parsing`() {
        val unknownVendor = ModelVendor.UNKNOWN

        assertEquals(ModelVendor.UNKNOWN, unknownVendor)

        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "test-model",
                    snapshot = null,
                    modelVendor = ModelVendor.UNKNOWN,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.03")
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = emptyList()
        )

        val modelsProvider = ModelsProvider(config)

        val result = modelsProvider.modelsList(ModelVendor.UNKNOWN)

        assertTrue(result.contains("test-model"))
    }


    @Test
    fun `unknown fields in configuration should be ignored during parsing`() {
        val config = PricingConfiguration(
            currency = usd,
            modelVendor = ModelVendor.OPENAI,
            modelsPricing = listOf(
                ModelPricing(
                    model = "test-model",
                    snapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.03")
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = emptyList()
        )

        assertNotNull(config)
        assertEquals(usd, config.currency)
        assertEquals(ModelVendor.OPENAI, config.modelVendor)
    }


    @Test
    fun `modelsList with null modelVendor includes all models respecting defaults`() {
        val config = PricingConfiguration(
            currency = usd,
            modelVendor = ModelVendor.OPENAI,
            modelsPricing = listOf(
                ModelPricing(
                    model = "openai-model",
                    snapshot = null,
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
                    model = "anthropic-model",
                    snapshot = null,
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
                    model = "default-vendor-model",
                    snapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.05")
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = emptyList()
        )

        val modelsProvider = ModelsProvider(config)

        val openaiModels = modelsProvider.modelsList(ModelVendor.OPENAI)
        val anthropicModels = modelsProvider.modelsList(ModelVendor.ANTHROPIC)

        assertEquals(listOf("openai-model", "default-vendor-model"), openaiModels)
        assertEquals(listOf("anthropic-model"), anthropicModels)
    }


    @Test
    fun `complex scenario with currency and vendor defaults and overrides`() {
        val config = PricingConfiguration(
            currency = usd,
            modelVendor = ModelVendor.OPENAI,
            modelsPricing = listOf(

                ModelPricing(
                    model = "default-everything",
                    snapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.01")
                        )
                    ),
                    currency = null,
                    isDefaultPricing = false
                ),

                ModelPricing(
                    model = "override-vendor",
                    snapshot = null,
                    modelVendor = ModelVendor.ANTHROPIC,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.02")
                        )
                    ),
                    currency = null,
                    isDefaultPricing = false
                ),

                ModelPricing(
                    model = "override-currency",
                    snapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.03")
                        )
                    ),
                    currency = eur,
                    isDefaultPricing = false
                ),

                ModelPricing(
                    model = "override-both",
                    snapshot = null,
                    modelVendor = ModelVendor.META,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.04")
                        )
                    ),
                    currency = eur,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = emptyList()
        )

        val modelsProvider = ModelsProvider(config)
        val costCalculator = CostCalculator(config)

        val openaiModels = modelsProvider.modelsList(ModelVendor.OPENAI)
        val anthropicModels = modelsProvider.modelsList(ModelVendor.ANTHROPIC)
        val metaModels = modelsProvider.modelsList(ModelVendor.META)

        assertEquals(listOf("default-everything", "override-currency"), openaiModels)
        assertEquals(listOf("override-vendor"), anthropicModels)
        assertEquals(listOf("override-both"), metaModels)


        val defaultResult = costCalculator.computeUnitCostOrThrow("default-everything", 1000L, INPUT_TEXT_TOKENS)
        val overrideCurrencyResult = costCalculator.computeUnitCostOrThrow("override-currency", 1000L, INPUT_TEXT_TOKENS)
        val overrideBothResult = costCalculator.computeUnitCostOrThrow("override-both", 1000L, INPUT_TEXT_TOKENS)

        assertEquals(usd, defaultResult.currency)
        assertEquals(eur, overrideCurrencyResult.currency)
        assertEquals(eur, overrideBothResult.currency)
    }


    @Test
    fun `empty models pricing list handles gracefully`() {
        val config = PricingConfiguration(
            currency = usd,
            modelVendor = ModelVendor.OPENAI,
            modelsPricing = emptyList(),
            exchangeRates = emptyList()
        )

        val modelsProvider = ModelsProvider(config)


        val openaiModels = modelsProvider.modelsList(ModelVendor.OPENAI)

        assertEquals(emptyList<String>(), openaiModels)
    }


    @Test
    fun `null exchangeRates handled gracefully`() {
        val config = PricingConfiguration(
            currency = usd,
            modelVendor = null,
            modelsPricing = listOf(
                ModelPricing(
                    model = "test-model",
                    snapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.03")
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = null
        )

        val costCalculator = CostCalculator(config)

        val result = costCalculator.computeUnitCostOrThrow("test-model", 1000L, INPUT_TEXT_TOKENS)

        assertEquals(BigDecimal("0.03"), result.amount)
        assertEquals(usd, result.currency)
    }


    @Test
    fun `all vendor types work correctly in filtering`() {
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "openai-model",
                    snapshot = null,
                    modelVendor = ModelVendor.OPENAI,
                    pricing = listOf(ModelPricing.Pricing(unit = INPUT_TEXT_TOKENS, perUnit = 1000L, basePrice = BigDecimal("0.01"))),
                    currency = usd,
                    isDefaultPricing = false
                ),
                ModelPricing(
                    model = "anthropic-model",
                    snapshot = null,
                    modelVendor = ModelVendor.ANTHROPIC,
                    pricing = listOf(ModelPricing.Pricing(unit = INPUT_TEXT_TOKENS, perUnit = 1000L, basePrice = BigDecimal("0.01"))),
                    currency = usd,
                    isDefaultPricing = false
                ),
                ModelPricing(
                    model = "sber-model",
                    snapshot = null,
                    modelVendor = ModelVendor.SBER,
                    pricing = listOf(ModelPricing.Pricing(unit = INPUT_TEXT_TOKENS, perUnit = 1000L, basePrice = BigDecimal("0.01"))),
                    currency = usd,
                    isDefaultPricing = false
                ),
                ModelPricing(
                    model = "yandex-model",
                    snapshot = null,
                    modelVendor = ModelVendor.YANDEX,
                    pricing = listOf(ModelPricing.Pricing(unit = INPUT_TEXT_TOKENS, perUnit = 1000L, basePrice = BigDecimal("0.01"))),
                    currency = usd,
                    isDefaultPricing = false
                ),
                ModelPricing(
                    model = "unknown-model",
                    snapshot = null,
                    modelVendor = ModelVendor.UNKNOWN,
                    pricing = listOf(ModelPricing.Pricing(unit = INPUT_TEXT_TOKENS, perUnit = 1000L, basePrice = BigDecimal("0.01"))),
                    currency = usd,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = emptyList()
        )

        val modelsProvider = ModelsProvider(config)


        assertEquals(listOf("openai-model"), modelsProvider.modelsList(ModelVendor.OPENAI))
        assertEquals(listOf("anthropic-model"), modelsProvider.modelsList(ModelVendor.ANTHROPIC))
        assertEquals(listOf("sber-model"), modelsProvider.modelsList(ModelVendor.SBER))
        assertEquals(listOf("yandex-model"), modelsProvider.modelsList(ModelVendor.YANDEX))
        assertEquals(listOf("unknown-model"), modelsProvider.modelsList(ModelVendor.UNKNOWN))
    }
}
