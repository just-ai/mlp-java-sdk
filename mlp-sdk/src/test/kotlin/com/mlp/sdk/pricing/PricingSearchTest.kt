package com.mlp.sdk.pricing

import com.mlp.sdk.pricing.ModelPricing.ModelVendor
import com.mlp.sdk.pricing.ModelPricing.Pricing.UnitType.INPUT_TEXT_TOKENS
import com.mlp.sdk.pricing.ModelPricing.Pricing.UnitType.OUTPUT_TEXT_TOKENS
import com.mlp.sdk.CommonErrorCode
import com.mlp.sdk.MlpException
import java.math.BigDecimal
import java.util.Currency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PricingSearchTest {

    private val usd = Currency.getInstance("USD")

    @Test
    fun `findPricingOrNull - returns exact model match`() {
        val config = createTestPricingConfiguration()
        val calculator = CostCalculator(config)
        
        val result = calculator.retrievePricingOrNull("gpt-4")
        
        assertNotNull(result)
        assertEquals("gpt-4", result!!.model)
    }

    @Test
    fun `findPricingOrNull - returns fallback when model not found`() {
        val config = createTestPricingConfiguration()
        val calculator = CostCalculator(config)
        
        val result = calculator.retrievePricingOrNull("unknown-model")
        
        assertNotNull(result)
        assertEquals("fallback-model", result!!.model)
        assertTrue(result.isDefaultPricing)
    }

    @Test
    fun `findPricingOrNull - returns null when no model and no fallback`() {
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "gpt-4",
                    defaultSnapshot = null,
                    modelVendor = ModelVendor.ANTHROPIC,
                    pricing = emptyList(),
                    currency = usd,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = emptyList()
        )
        
        val calculator = CostCalculator(config)
        
        val result = calculator.retrievePricingOrNull("unknown-model")
        
        assertNull(result)
    }

    @Test
    fun `getPricingOrThrow - returns exact model match`() {
        val config = createTestPricingConfiguration()
        val calculator = CostCalculator(config)
        
        val result = calculator.retrievePricingOrThrow("gpt-4")
        
        assertEquals("gpt-4", result.model)
    }

    @Test
    fun `getPricingOrThrow - returns fallback when model not found`() {
        val config = createTestPricingConfiguration()
        val calculator = CostCalculator(config)
        
        val result = calculator.retrievePricingOrThrow("unknown-model")
        
        assertEquals("fallback-model", result.model)
        assertTrue(result.isDefaultPricing)
    }

    @Test
    fun `getPricingOrThrow - throws MlpException when no model and no fallback`() {
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "gpt-4",
                    defaultSnapshot = null,
                    modelVendor = ModelVendor.ANTHROPIC,
                    pricing = emptyList(),
                    currency = usd,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = emptyList()
        )
        
        val calculator = CostCalculator(config)
        
        val exception = assertThrows(MlpException::class.java) {
            calculator.retrievePricingOrThrow("unknown-model")
        }
        
        assertEquals(CommonErrorCode.BAD_REQUEST, exception.error.errorCode)
        assertTrue(exception.error.args["message"].toString().contains("Unknown model: unknown-model"))
    }

    @Test
    fun `exact model match takes precedence over fallback even with snapshot mismatch`() {
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "gpt-4",
                    defaultSnapshot = "v1.0",
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
        
        val result = calculator.retrievePricingOrThrow("gpt-4")
        
        assertEquals("gpt-4", result.model)
        assertFalse(result.isDefaultPricing)
    }

    @Test
    fun `model search with empty model name uses fallback`() {
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "",
                    defaultSnapshot = null,
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
        
        val result = calculator.retrievePricingOrThrow("test-model")
        
        assertEquals("fallback-model", result.model)
        assertTrue(result.isDefaultPricing)
    }

    @Test
    fun `model search with null snapshot matches any snapshot`() {
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
                    model = "gpt-4",
                    defaultSnapshot = "v1.0",
                    modelVendor = ModelVendor.OPENAI,
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
        
        val calculator = CostCalculator(config)
        
        val result = calculator.retrievePricingOrThrow("gpt-4")
        
        assertEquals("gpt-4", result.model)
        assertNull(result.defaultSnapshot)
    }

    @Test
    fun `pricing search considers both model and snapshot`() {
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "gpt-4",
                    defaultSnapshot = "v1.0",
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
                    model = "gpt-4",
                    defaultSnapshot = "v2.0",
                    modelVendor = ModelVendor.OPENAI,
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
        
        val calculator = CostCalculator(config)
        
        val resultV1 = calculator.retrievePricingOrThrow("gpt-4")
        
        assertEquals("gpt-4", resultV1.model)
        assertEquals("v1.0", resultV1.defaultSnapshot)
    }

    @Test
    fun `modelsList - returns all models when vendor is null`() {
        val config = createTestPricingConfiguration()
        val modelsProvider = ModelsProvider(config)
        
        val result = modelsProvider.modelsList(ModelVendor.ANTHROPIC)
        
        assertEquals(listOf("gpt-4", "fallback-model"), result)
    }

    @Test
    fun `modelsList - returns only matching vendor models`() {
        val config = createTestPricingConfiguration()
        val modelsProvider = ModelsProvider(config)
        
        val result = modelsProvider.modelsList(ModelVendor.META)
        
        assertEquals(listOf("llama-2", "fallback-model"), result)
    }

    @Test
    fun `modelsList - returns fallback when no matching models`() {
        val config = createTestPricingConfiguration()
        val modelsProvider = ModelsProvider(config)
        
        val result = modelsProvider.modelsList(ModelVendor.YANDEX)
        
        assertEquals(listOf("fallback-model"), result)
    }

    @Test
    fun `exactlyMatch true - requires exact model name match`() {
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
        
        val exactCalculator = CostCalculator(config, modelPrefixMatching = false)
        
        // Exact match should work
        val exactResult = exactCalculator.retrievePricingOrNull("gpt-4")
        assertNotNull(exactResult)
        assertEquals("gpt-4", exactResult!!.model)
        
        // Prefix match should not work with exactlyMatch=true, should use fallback
        val prefixResult = exactCalculator.retrievePricingOrNull("gpt-4-turbo")
        assertNotNull(prefixResult)
        assertEquals("fallback-model", prefixResult!!.model)
        assertTrue(prefixResult.isDefaultPricing)
    }

    @Test
    fun `exactlyMatch false - allows prefix matching (default behavior)`() {
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
        
        val prefixCalculator = CostCalculator(config, modelPrefixMatching = true)
        
        // Exact match should work
        val exactResult = prefixCalculator.retrievePricingOrNull("gpt-4")
        assertNotNull(exactResult)
        assertEquals("gpt-4", exactResult!!.model)
        
        // Prefix match should work with exactlyMatch=false
        val prefixResult = prefixCalculator.retrievePricingOrNull("gpt-4-turbo")
        assertNotNull(prefixResult)
        assertEquals("gpt-4", prefixResult!!.model)
        assertFalse(prefixResult.isDefaultPricing)
    }

    @Test
    fun `default exactlyMatch behavior is false`() {
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
                )
            ),
            exchangeRates = emptyList()
        )
        
        val defaultCalculator = CostCalculator(config)
        
        // Prefix match should work by default (exactlyMatch defaults to false)
        val prefixResult = defaultCalculator.retrievePricingOrNull("gpt-4-turbo")
        assertNotNull(prefixResult)
        assertEquals("gpt-4", prefixResult!!.model)
    }

    @Test
    fun `modelsList with UNKNOWN vendor includes only unknown vendor models`() {
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "unknown-vendor-model",
                    defaultSnapshot = null,
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
                ),
                ModelPricing(
                    model = "openai-model",
                    defaultSnapshot = null,
                    modelVendor = ModelVendor.OPENAI,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.04")
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = emptyList()
        )
        
        val modelsProvider = ModelsProvider(config)
        
        val unknownModels = modelsProvider.modelsList(ModelVendor.UNKNOWN)
        val openaiModels = modelsProvider.modelsList(ModelVendor.OPENAI)
        
        assertEquals(listOf("unknown-vendor-model"), unknownModels)
        assertEquals(listOf("openai-model"), openaiModels)
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
            exchangeRates = emptyList()
        )
    }
}
