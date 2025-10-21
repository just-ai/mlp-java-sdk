package com.mlp.sdk.pricing

import com.mlp.api.datatypes.pricing.ExchangeRate
import com.mlp.api.datatypes.pricing.ModelPricing
import com.mlp.api.datatypes.pricing.ModelPricing.ModelVendor
import com.mlp.api.datatypes.pricing.ModelPricing.Pricing.UnitType.INPUT_TEXT_TOKENS
import com.mlp.api.datatypes.pricing.PricingConfiguration
import java.math.BigDecimal
import java.util.Currency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CurrencyConversionTest {

    private val usd = Currency.getInstance("USD")
    private val eur = Currency.getInstance("EUR")

    @Test
    fun `currency conversion is applied when exchangeRates are available`() {
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
                            basePrice = BigDecimal("10")
                        )
                    ),
                    currency = eur,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = listOf(
                ExchangeRate(from = eur, to = usd, rate = BigDecimal("1.2"))
            )
        )
        
        val calculator = CostCalculator(config)
        
        val result = calculator.computeUnitCostOrThrow("test-model", 1000L, INPUT_TEXT_TOKENS)
        
        assertEquals(eur, result.currency)
        assertEquals(BigDecimal("10"), result.amount)
    }

    @Test
    fun `currency conversion missing exchange rate returns original currency`() {
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
                            basePrice = BigDecimal("10")
                        )
                    ),
                    currency = eur,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = listOf(
                ExchangeRate(from = usd, to = eur, rate = BigDecimal("0.9"))
            )
        )
        
        val calculator = CostCalculator(config)
        
        val result = calculator.computeUnitCostOrThrow("test-model", 1000L, INPUT_TEXT_TOKENS)
        
        assertEquals(eur, result.currency)
        assertEquals(BigDecimal("10"), result.amount)
    }

    @Test
    fun `currency conversion with zero exchange rate`() {
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
                            basePrice = BigDecimal("10")
                        )
                    ),
                    currency = eur,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = listOf(
                ExchangeRate(from = eur, to = usd, rate = BigDecimal.ZERO)
            )
        )
        
        val calculator = CostCalculator(config)
        
        val result = calculator.computeUnitCostOrThrow("test-model", 1000L, INPUT_TEXT_TOKENS)
        
        assertEquals(eur, result.currency)
        assertEquals(BigDecimal("10"), result.amount)
    }

    @Test
    fun `currency conversion with negative exchange rate`() {
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
                            basePrice = BigDecimal("10")
                        )
                    ),
                    currency = eur,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = listOf(
                ExchangeRate(from = eur, to = usd, rate = BigDecimal("-1.2"))
            )
        )
        
        val calculator = CostCalculator(config)
        
        val result = calculator.computeUnitCostOrThrow("test-model", 1000L, INPUT_TEXT_TOKENS)
        
        assertEquals(eur, result.currency)
    }

    @Test
    fun `currency conversion with same from and to currency`() {
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
                            basePrice = BigDecimal("10")
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = listOf(
                ExchangeRate(from = usd, to = usd, rate = BigDecimal("1"))
            )
        )
        
        val calculator = CostCalculator(config)
        
        val result = calculator.computeUnitCostOrThrow("test-model", 1000L, INPUT_TEXT_TOKENS)
        
        assertEquals(usd, result.currency)
        assertEquals(BigDecimal("10"), result.amount)
    }

    @Test
    fun `default currency from PricingConfiguration is used when ModelPricing currency is null`() {
        val config = PricingConfiguration(
            currency = eur,
            modelsPricing = listOf(
                ModelPricing(
                    model = "test-model",
                    defaultSnapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.05")
                        )
                    ),
                    currency = null,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = emptyList()
        )
        
        val calculator = CostCalculator(config)
        
        val result = calculator.computeUnitCostOrThrow("test-model", 1000L, INPUT_TEXT_TOKENS)
        
        assertEquals(eur, result.currency)
    }

    @Test
    fun `ModelPricing currency overrides PricingConfiguration currency`() {
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
                            basePrice = BigDecimal("0.05")
                        )
                    ),
                    currency = eur,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = emptyList()
        )
        
        val calculator = CostCalculator(config)
        
        val result = calculator.computeUnitCostOrThrow("test-model", 1000L, INPUT_TEXT_TOKENS)
        
        assertEquals(eur, result.currency)
    }

    @Test
    fun `no error when currency and modelVendor are null everywhere`() {
        val config = PricingConfiguration(
            currency = null,
            modelVendor = null,
            modelsPricing = listOf(
                ModelPricing(
                    model = "test-model",
                    defaultSnapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.03")
                        )
                    ),
                    currency = null,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = emptyList()
        )
        
        val calculator = CostCalculator(config)
        
        val result = calculator.computeUnitCostOrThrow("test-model", 1000L, INPUT_TEXT_TOKENS)
        
        assertEquals(BigDecimal("0.03"), result.amount)
        assertNull(result.currency)
    }

    @Test
    fun `currency precedence - ModelPricing overrides config even when both are null`() {
        val config1 = PricingConfiguration(
            currency = null,
            modelsPricing = listOf(
                ModelPricing(
                    model = "test-model",
                    defaultSnapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.03")
                        )
                    ),
                    currency = null,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = emptyList()
        )

        var calculator = CostCalculator(config1)

        val result = calculator.computeUnitCostOrThrow("test-model", 1000L, INPUT_TEXT_TOKENS)
        assertNull(result.currency)
        
        val config2 = PricingConfiguration(
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
                            basePrice = BigDecimal("0.03")
                        )
                    ),
                    currency = null,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = emptyList()
        )


        calculator = CostCalculator(config2)

        val result2 = calculator.computeUnitCostOrThrow("test-model", 1000L, INPUT_TEXT_TOKENS)
        assertEquals(usd, result2.currency)
    }

    @Test
    fun `complex currency conversion scenario with fallback model`() {
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
                    model = "fallback-usd",
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
        
        val eurModelResult = calculator.computeUnitCostOrThrow("eur-model", 1000L, INPUT_TEXT_TOKENS)
        val unknownModelResult = calculator.computeUnitCostOrThrow("unknown-model", 1000L, INPUT_TEXT_TOKENS)
        
        assertEquals(eur, eurModelResult.currency)
        assertEquals(BigDecimal("0.05"), eurModelResult.amount)
        
        assertEquals(usd, unknownModelResult.currency)
        assertEquals(BigDecimal("0.02"), unknownModelResult.amount)
    }

    @Test
    fun `multiple exchange rates with different currencies`() {
        val gbp = Currency.getInstance("GBP")
        val jpy = Currency.getInstance("JPY")
        
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "eur-model",
                    defaultSnapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("10")
                        )
                    ),
                    currency = eur,
                    isDefaultPricing = false
                ),
                ModelPricing(
                    model = "gbp-model",
                    defaultSnapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("8")
                        )
                    ),
                    currency = gbp,
                    isDefaultPricing = false
                ),
                ModelPricing(
                    model = "jpy-model",
                    defaultSnapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("1000")
                        )
                    ),
                    currency = jpy,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = listOf(
                ExchangeRate(from = eur, to = usd, rate = BigDecimal("1.2")),
                ExchangeRate(from = gbp, to = usd, rate = BigDecimal("1.3")),
                ExchangeRate(from = jpy, to = usd, rate = BigDecimal("0.007"))
            )
        )
        
        val calculator = CostCalculator(config)
        
        val eurResult = calculator.computeUnitCostOrThrow("eur-model", 1000L, INPUT_TEXT_TOKENS)
        val gbpResult = calculator.computeUnitCostOrThrow("gbp-model", 1000L, INPUT_TEXT_TOKENS)
        val jpyResult = calculator.computeUnitCostOrThrow("jpy-model", 1000L, INPUT_TEXT_TOKENS)
        
        assertEquals(eur, eurResult.currency)
        assertEquals(BigDecimal("10"), eurResult.amount)
        
        assertEquals(gbp, gbpResult.currency)
        assertEquals(BigDecimal("8"), gbpResult.amount)
        
        assertEquals(jpy, jpyResult.currency)
        assertEquals(BigDecimal("1000"), jpyResult.amount)
    }

    @Test
    fun `currency null handling in edge cases`() {
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "null-currency-model",
                    defaultSnapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.05")
                        )
                    ),
                    currency = null,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = null
        )

        val calculator = CostCalculator(config)

        val result = calculator.computeUnitCostOrThrow("null-currency-model", 1000L, INPUT_TEXT_TOKENS)
        
        assertEquals(usd, result.currency)
        assertEquals(BigDecimal("0.05"), result.amount)
    }
}
