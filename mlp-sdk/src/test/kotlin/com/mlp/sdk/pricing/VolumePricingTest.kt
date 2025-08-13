package com.mlp.sdk.pricing

import com.mlp.sdk.pricing.ModelPricing.Pricing.UnitType.INPUT_TEXT_TOKENS
import java.math.BigDecimal
import java.util.Currency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VolumePricingTest {

    private val usd = Currency.getInstance("USD")

    @Test
    fun `cost function - applies volume pricing tier 1`() {
        val config = createVolumeTestConfiguration()
        val calculator = CostCalculator(config)
        
        val result = calculator.computeUnitCostOrThrow("gpt-4", 200000L, INPUT_TEXT_TOKENS)
        
        val expected = BigDecimal("5")
        assertEquals(expected.toDouble(), result.amount.toDouble())
        assertEquals(usd, result.currency)
    }

    @Test
    fun `cost function - applies volume pricing tier 2`() {
        val config = createVolumeTestConfiguration()
        val calculator = CostCalculator(config)
        
        val result = calculator.computeUnitCostOrThrow("gpt-4", 600000L, INPUT_TEXT_TOKENS)
        
        val expected = BigDecimal("12")
        assertEquals(expected.toDouble(), result.amount.toDouble())
        assertEquals(usd, result.currency)
    }

    @Test
    fun `getPrice - returns base price when no volume pricing applies`() {
        val config = PricingConfiguration(
            currency = usd,
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
        
        val calculator = CostCalculator(config)
        
        val result = calculator.computeUnitCostOrThrow("test-model", 5000L, INPUT_TEXT_TOKENS)
        val expectedAmount = BigDecimal("0.03") * BigDecimal("5000") / BigDecimal("1000")
        
        assertEquals(expectedAmount, result.amount)
    }

    @Test
    fun `getPrice - applies volume pricing when in range`() {
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "test-model",
                    snapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.03"),
                            overrides = ModelPricing.Overrides(
                                volumePricing = listOf(
                                    VolumePricing(
                                        price = BigDecimal("0.025"),
                                        unitsRange = VolumePricing.UnitsRange(from = 10000L, to = 50000L)
                                    )
                                )
                            )
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = emptyList()
        )
        
        val calculator = CostCalculator(config)
        
        val result = calculator.computeUnitCostOrThrow("test-model", 20000L, INPUT_TEXT_TOKENS)
        val expectedAmount = BigDecimal("0.025") * BigDecimal("20000") / BigDecimal("1000")
        
        assertEquals(expectedAmount.stripTrailingZeros(), result.amount)
    }

    @Test
    fun `getPrice - applies highest tier when multiple tiers match`() {
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "test-model",
                    snapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.03"),
                            overrides = ModelPricing.Overrides(
                                volumePricing = listOf(
                                    VolumePricing(
                                        price = BigDecimal("0.025"),
                                        unitsRange = VolumePricing.UnitsRange(from = 10000L, to = 50000L)
                                    ),
                                    VolumePricing(
                                        price = BigDecimal("0.02"),
                                        unitsRange = VolumePricing.UnitsRange(from = 30000L, to = null)
                                    )
                                )
                            )
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = emptyList()
        )
        
        val calculator = CostCalculator(config)
        
        val result = calculator.computeUnitCostOrThrow("test-model", 40000L, INPUT_TEXT_TOKENS)
        val expectedAmount = BigDecimal("0.02") * BigDecimal("40000") / BigDecimal("1000")
        
        assertEquals(expectedAmount.stripTrailingZeros(), result.amount)
    }

    @Test
    fun `getPrice handles null volume pricing overrides`() {
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "test-model",
                    snapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.03"),
                            overrides = null
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = emptyList()
        )
        
        val calculator = CostCalculator(config)
        
        val result = calculator.computeUnitCostOrThrow("test-model", 5000L, INPUT_TEXT_TOKENS)
        val expectedAmount = BigDecimal("0.03") * BigDecimal("5000") / BigDecimal("1000")
        
        assertEquals(expectedAmount, result.amount)
    }

    @Test
    fun `getPrice handles empty volume pricing list`() {
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "test-model",
                    snapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.03"),
                            overrides = ModelPricing.Overrides(volumePricing = emptyList())
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = emptyList()
        )
        
        val calculator = CostCalculator(config)
        
        val result = calculator.computeUnitCostOrThrow("test-model", 5000L, INPUT_TEXT_TOKENS)
        val expectedAmount = BigDecimal("0.03") * BigDecimal("5000") / BigDecimal("1000")
        
        assertEquals(expectedAmount, result.amount)
    }

    @Test
    fun `getPrice handles units below volume pricing threshold`() {
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "test-model",
                    snapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.03"),
                            overrides = ModelPricing.Overrides(
                                volumePricing = listOf(
                                    VolumePricing(
                                        price = BigDecimal("0.025"),
                                        unitsRange = VolumePricing.UnitsRange(from = 100000L, to = null)
                                    )
                                )
                            )
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = emptyList()
        )
        
        val calculator = CostCalculator(config)
        
        val result = calculator.computeUnitCostOrThrow("test-model", 5000L, INPUT_TEXT_TOKENS)
        val expectedAmount = BigDecimal("0.03") * BigDecimal("5000") / BigDecimal("1000")
        
        assertEquals(expectedAmount, result.amount)
    }

    @Test
    fun `volume pricing with null upper bound works correctly`() {
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "test-model",
                    snapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.03"),
                            overrides = ModelPricing.Overrides(
                                volumePricing = listOf(
                                    VolumePricing(
                                        price = BigDecimal("0.02"),
                                        unitsRange = VolumePricing.UnitsRange(from = 50000L, to = null)
                                    )
                                )
                            )
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = emptyList()
        )
        
        val calculator = CostCalculator(config)
        
        val result = calculator.computeUnitCostOrThrow("test-model", 100000L, INPUT_TEXT_TOKENS)
        val expectedAmount = BigDecimal("0.02") * BigDecimal("100000") / BigDecimal("1000")
        
        assertEquals(expectedAmount.stripTrailingZeros(), result.amount)
    }

    @Test
    fun `volume pricing exact boundary conditions`() {
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "test-model",
                    snapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.03"),
                            overrides = ModelPricing.Overrides(
                                volumePricing = listOf(
                                    VolumePricing(
                                        price = BigDecimal("0.025"),
                                        unitsRange = VolumePricing.UnitsRange(from = 10000L, to = 50000L)
                                    ),
                                    VolumePricing(
                                        price = BigDecimal("0.02"),
                                        unitsRange = VolumePricing.UnitsRange(from = 50000L, to = null)
                                    )
                                )
                            )
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = emptyList()
        )
        
        val calculator = CostCalculator(config)
        
        val result10000 = calculator.computeUnitCostOrThrow("test-model", 10000L, INPUT_TEXT_TOKENS)
        assertEquals(BigDecimal("0.25"), result10000.amount)
        
        val result9999 = calculator.computeUnitCostOrThrow("test-model", 9999L, INPUT_TEXT_TOKENS)
        assertEquals(BigDecimal("0.29997"), result9999.amount)
        
        val result50000 = calculator.computeUnitCostOrThrow("test-model", 50000L, INPUT_TEXT_TOKENS)
        assertEquals(BigDecimal("1"), result50000.amount)
        
        val result49999 = calculator.computeUnitCostOrThrow("test-model", 49999L, INPUT_TEXT_TOKENS)
        assertEquals(BigDecimal("1.249975"), result49999.amount)
    }

    @Test
    fun `volume pricing with overlapping ranges uses last matching tier`() {
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "test-model",
                    snapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.03"),
                            overrides = ModelPricing.Overrides(
                                volumePricing = listOf(
                                    VolumePricing(
                                        price = BigDecimal("0.025"),
                                        unitsRange = VolumePricing.UnitsRange(from = 10000L, to = 100000L)
                                    ),
                                    VolumePricing(
                                        price = BigDecimal("0.02"),
                                        unitsRange = VolumePricing.UnitsRange(from = 20000L, to = 80000L)
                                    )
                                )
                            )
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = emptyList()
        )
        
        val calculator = CostCalculator(config)
        
        val result30000 = calculator.computeUnitCostOrThrow("test-model", 30000L, INPUT_TEXT_TOKENS)
        assertEquals(BigDecimal("0.6"), result30000.amount)
    }

    @Test
    fun `volume pricing with zero from range`() {
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "test-model",
                    snapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.03"),
                            overrides = ModelPricing.Overrides(
                                volumePricing = listOf(
                                    VolumePricing(
                                        price = BigDecimal("0.01"),
                                        unitsRange = VolumePricing.UnitsRange(from = 0L, to = 10000L)
                                    )
                                )
                            )
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = emptyList()
        )
        
        val calculator = CostCalculator(config)
        
        val result1000 = calculator.computeUnitCostOrThrow("test-model", 1000L, INPUT_TEXT_TOKENS)
        assertEquals(BigDecimal("0.01"), result1000.amount)
        
        val result0 = calculator.computeUnitCostOrThrow("test-model", 0L, INPUT_TEXT_TOKENS)
        assertEquals(BigDecimal.ZERO, result0.amount)
    }

    @Test
    fun `overrides can be null without causing errors`() {
        val config = PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "test-model",
                    snapshot = null,
                    modelVendor = null,
                    pricing = listOf(
                        ModelPricing.Pricing(
                            unit = INPUT_TEXT_TOKENS,
                            perUnit = 1000L,
                            basePrice = BigDecimal("0.03"),
                            overrides = null
                        )
                    ),
                    currency = usd,
                    isDefaultPricing = false
                )
            ),
            exchangeRates = emptyList()
        )
        
        val calculator = CostCalculator(config)
        
        val result = calculator.computeUnitCostOrThrow("test-model", 1000L, INPUT_TEXT_TOKENS)
        
        assertEquals(BigDecimal("0.03"), result.amount)
    }

    private fun createVolumeTestConfiguration(): PricingConfiguration {
        return PricingConfiguration(
            currency = usd,
            modelsPricing = listOf(
                ModelPricing(
                    model = "gpt-4",
                    snapshot = null,
                    modelVendor = null,
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
