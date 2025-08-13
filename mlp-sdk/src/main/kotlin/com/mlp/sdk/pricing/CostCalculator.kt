package com.mlp.sdk.pricing

import com.mlp.sdk.CommonErrorCode
import com.mlp.sdk.MlpError
import com.mlp.sdk.MlpException
import com.mlp.sdk.pricing.ModelPricing.Pricing.UnitType
import com.mlp.sdk.pricing.ModelPricing.Pricing.UnitType.CACHED_INPUT_TEXT_TOKENS
import com.mlp.sdk.pricing.ModelPricing.Pricing.UnitType.INPUT_TEXT_TOKENS
import com.mlp.sdk.pricing.ModelPricing.Pricing.UnitType.OUTPUT_TEXT_TOKENS
import com.mlp.sdk.utils.WithLogger
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP
import java.util.Currency

/**
 * Multiplier constant for converting cost amounts to micro format (1,000,000)
 */
val MICRO_MULTIPLIER = 1_000_000L.toBigDecimal()

/**
 * Computes the total cost for a completion request based on input, output, and cached input tokens.
 *
 * @param model The model name to calculate costs for
 * @param inputTokens Number of input tokens
 * @param outputTokens Number of output tokens
 * @param cachedInputTokens Number of cached input tokens (optional)
 * @return The total cost for the completion request
 */
fun CostCalculator.computeCompletionRequestCost(
    model: String,
    inputTokens: Long,
    outputTokens: Long,
    cachedInputTokens: Long? = null,
): Cost {
    val inputCost = computeUnitCostOrThrow(model, inputTokens, INPUT_TEXT_TOKENS)
    val outputCost = computeUnitCostOrThrow(model, outputTokens, OUTPUT_TEXT_TOKENS)
    val cachedInputCost = computeUnitCostOrZero(model, cachedInputTokens, CACHED_INPUT_TEXT_TOKENS)
    val cost = inputCost + outputCost + cachedInputCost

    logger.info("Chat completion cost for model $model: input: $inputCost (tokens $inputTokens), output: $outputCost (tokens $outputTokens), cached input: $cachedInputCost (tokens $cachedInputTokens), total: $cost")
    return cost
}

/**
 * Calculator for computing costs based on model pricing configuration.
 *
 * @param configuration The pricing configuration containing model pricing information
 * @param exactlyMatch If true, model names must match exactly; if false, uses prefix matching
 */
class CostCalculator(private val configuration: PricingConfiguration, private val modelPrefixMatching: Boolean = true) : WithLogger {

    fun computeUnitCostOrThrow(
        model: String,
        units: Long,
        unitType: UnitType,
    ): Cost {
        val modelPricing = retrievePricingOrThrow(model)
        val unitPricing = modelPricing.pricing.first { it.unit == unitType }
        val price = getPrice(units, unitPricing)
        val costAmount = (price * BigDecimal(units)).divide(BigDecimal(unitPricing.perUnit), 6, HALF_UP)
            .stripTrailingZeros()
            .withoutScientificAnnotation()

        return Cost(costAmount, modelPricing.currency ?: configuration.currency)
    }

    fun computeUnitCostOrZero(
        model: String,
        units: Long?,
        unitType: UnitType,
    ): Cost {
        val modelPricing = retrievePricingOrNull(model) ?: return Cost.zero(configuration.currency)
        if (units == null) return Cost.zero(modelPricing.currency ?: configuration.currency)

        return runCatching { computeUnitCostOrThrow(model, units, unitType) }
            .getOrDefault(Cost.zero(modelPricing.currency ?: configuration.currency))
    }

    fun retrievePricingOrThrow(model: String) =
        retrievePricingOrNull(model)
            ?: throw MlpException(
                MlpError(
                    CommonErrorCode.BAD_REQUEST,
                    mapOf("message" to "Unknown model: $model.")
                )
            )

    fun retrievePricingOrNull(model: String, useDefaultPricing: Boolean = true) =
        configuration.modelsPricing.filter { isModelMatch(it, model) }.maxByOrNull { it.model.length }
            ?: configuration.modelsPricing.firstOrNull { it.isDefaultPricing }?.takeIf { useDefaultPricing }

    private fun isModelMatch(modelPricing: ModelPricing, model: String): Boolean {
        return if (modelPrefixMatching) model.startsWith(modelPricing.model) else modelPricing.model == model
    }

    // TODO improve
    private fun getPrice(unitsCount: Long, unitPricing: ModelPricing.Pricing): BigDecimal {
        var price = unitPricing.basePrice

        unitPricing.overrides?.volumePricing?.sortedBy { it.unitsRange.from }
            ?.lastOrNull { unitsCount >= it.unitsRange.from && (it.unitsRange.to == null || unitsCount < it.unitsRange.to) }
            ?.let { price = it.price }

        return price
    }
}

/**
 * Represents a cost with an amount and currency.
 *
 * @param amount The cost amount
 * @param currency The currency for the cost (nullable)
 */
data class Cost(val amount: BigDecimal, val currency: Currency?) {

    fun convertToMicroFormat(): Long = (amount * MICRO_MULTIPLIER).toLong()

    operator fun plus(other: Cost): Cost {
        require(this.currency == other.currency) { "Cannot add costs with different currencies: ${this.currency} and ${other.currency}" }
        return Cost(this.amount + other.amount, this.currency)
    }

    operator fun minus(other: Cost): Cost {
        require(this.currency == other.currency) { "Cannot subtract costs with different currencies: ${this.currency} and ${other.currency}" }
        return Cost(this.amount - other.amount, this.currency)
    }

    override fun toString(): String {
        val symbol = currency?.symbol ?: "?"
        return "${amount.stripTrailingZeros().toPlainString()}$symbol"
    }

    companion object {
        fun zero(currency: Currency?): Cost {
            return Cost(BigDecimal.ZERO, currency)
        }
    }
}


fun CostCalculator.computeCompletionRequestCost(
    model: String,
    inputTokens: Int,
    outputTokens: Int,
    cachedInputTokens: Int? = null,
) = computeCompletionRequestCost(model, inputTokens.toLong(), outputTokens.toLong(), cachedInputTokens?.toLong())

private fun BigDecimal.withoutScientificAnnotation(): BigDecimal = if (scale() < 0) setScale(0) else this

