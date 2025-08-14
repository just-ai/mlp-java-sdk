package com.mlp.sdk.pricing

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.mlp.sdk.pricing.ModelPricing.ModelVendor
import java.math.BigDecimal
import java.util.Currency


/**
 * Complete pricing configuration for the system including models, exchange rates, and currency settings.
 * 
 * @property currency The default currency used if model pricing currency is null (optional)
 * @property modelVendor The default model vendor used if model pricing model vendor is null (optional)
 * @property modelsPricing List of pricing configurations for different models
 * @property exchangeRates List of exchange rates for currency conversion (optional)
 */
data class PricingConfiguration(
    val currency: Currency? = null,
    val modelVendor: ModelVendor? = null,
    val modelsPricing: List<ModelPricing>,

    val exchangeRates: List<ExchangeRate>? = null
)

/**
 * Pricing configuration for a specific model.
 * 
 * @property model Model identifier
 * @property snapshot Model snapshot version (optional)
 * @property modelVendor Model vendor/creator (optional)
 * @property pricing List of pricing rules for different unit types
 * @property currency Currency used for this model's pricing
 * @property isDefaultPricing Will be used if there is no model with the specified name
 */
data class ModelPricing(
    val model: String,
    val snapshot: String?,
    val pricing: List<Pricing>,
    val currency: Currency? = null,
    val modelVendor: ModelVendor? = null,
    val isDefaultPricing: Boolean = false,
) {

    /**
     * Pricing rule for a specific unit type.
     * 
     * @property unit Type of billing unit (tokens, requests, etc.)
     * @property perUnit Number of units for the base price
     * @property basePrice Base price for the specified number of units
     * @property overrides Optional pricing overrides (e.g., volume pricing)
     * @property compatibleUnits List of compatible unit types for backward compatibility
     */
    data class Pricing(
        val unit: UnitType, // enum здесь для того, чтобы обновлялись методы для агрегированного подсчета стоимости
        val perUnit: Long,
        val basePrice: BigDecimal,
        val overrides: Overrides? = null,
        val compatibleUnits: List<UnitType>? = null, // Для обратной совместимости при добавлении нового юнита
    ) {
        /**
         * Type of billing unit for model usage.
         * Uses custom Jackson deserializer to handle unknown values gracefully.
         */
        @JsonDeserialize(using = UnitTypeDeserializer::class)
        enum class UnitType {
            /** Input text tokens */
            INPUT_TEXT_TOKENS,
            /** Output text tokens */
            OUTPUT_TEXT_TOKENS,
            /** Cached input text tokens */
            CACHED_INPUT_TEXT_TOKENS,
            
            /** Input images for vision models */
            INPUT_IMAGES,
            /** Output images for image generation */
            OUTPUT_IMAGES,
            /** Input images counted as tokens */
            INPUT_IMAGE_TOKENS,
            /** Output images counted as tokens */
            OUTPUT_IMAGE_TOKENS,
            
            /** Input audio in seconds */
            INPUT_AUDIO_SECONDS,
            /** Output audio in seconds */
            OUTPUT_AUDIO_SECONDS,
            /** Audio pricing by character count for TTS */
            AUDIO_CHARACTERS,
            
            /** Embedding model tokens */
            EMBEDDING_TOKENS,
            /** Embedding requests */
            EMBEDDING_REQUESTS,
            
            /** Training tokens for fine-tuning */
            TRAINING_TOKENS,
            /** Training epochs */
            TRAINING_EPOCHS,
            /** Hosted model hours */
            HOSTED_MODEL_HOURS,
            
            /** API requests */
            REQUESTS,
            /** Character count */
            CHARACTERS,
            /** Long context input tokens */
            INPUT_CONTEXT_TOKENS,
            /** Batch API requests */
            BATCH_REQUESTS,
            
            /** Content moderation requests */
            MODERATION_REQUESTS,
            
            /** Unknown or unrecognized unit type */
            UNKNOWN
        }
    }

    /**
     * Pricing overrides that can modify the base pricing rules.
     * 
     * @property volumePricing Optional volume-based pricing tiers
     * @property imageTierPricing Optional image-based pricing tiers for different quality/resolution combinations
     */
    data class Overrides(
        val volumePricing: List<VolumePricing>? = null,
        val imageTierPricing: List<ImageTierPricing>? = null,
    )

    /**
     * Model vendor/creator enumeration.
     * Uses custom Jackson deserializer to handle unknown values gracefully.
     */
    @JsonDeserialize(using = ModelVendorDeserializer::class)
    enum class ModelVendor {
        /** OpenAI */
        OPENAI,
        /** Anthropic */
        ANTHROPIC,
        /** Sberbank */
        SBER,
        /** Alibaba */
        ALIBABA,
        /** Meta (Facebook) */
        META,
        /** Google */
        GOOGLE,
        /** Mistral AI */
        MISTRAL,
        /** xAI (Elon Musk's AI company) */
        XAI,
        /** NVIDIA */
        NVIDIA,
        /** Cohere */
        COHERE,
        /** Hugging Face */
        HUGGINGFACE,
        /** Yandex */
        YANDEX,
        /** Text Generation Inference */
        TGI,
        /** Text Generation Inference */
        DEEPSEEK,
        /** Unknown or unrecognized vendor */
        UNKNOWN
    }
}

/**
 * Exchange rate configuration between two currencies.
 *
 * @property from Source currency
 * @property to Target currency
 * @property rate Exchange rate from source to target currency
 */
data class ExchangeRate(
    val from: Currency,
    val to: Currency,
    val rate: BigDecimal,
)

/**
 * Volume-based pricing tier configuration.
 * 
 * @property price Price for this volume tier
 * @property unitsRange Range of units this pricing applies to
 */
data class VolumePricing(
    val price: BigDecimal,
    val unitsRange: UnitsRange,
) {
    /**
     * Range of units for volume pricing.
     * 
     * @property from Starting unit count for this range (inclusive)
     * @property to Ending unit count for this range (inclusive). Null means no upper limit
     */
    data class UnitsRange(
        val from: Long,
        val to: Long? = null,
    )
}

/**
 * Image-based pricing tier configuration for image generation with specific quality and resolution.
 * 
 * @property price Price for this image configuration
 * @property resolution Image resolution (e.g., "1024x1024", "1792x1024")
 * @property quality Optional image quality (e.g., "standard", "hd"). If null, applies to any quality
 */
data class ImageTierPricing(
    val price: BigDecimal,
    val resolution: String,
    val quality: String? = null,
)

/**
 * Custom Jackson deserializer for UnitType enum.
 * Returns UnitType.UNKNOWN for any unrecognized values.
 */
class UnitTypeDeserializer : JsonDeserializer<ModelPricing.Pricing.UnitType>() {
    /**
     * Deserializes JSON string to UnitType enum.
     * 
     * @param p JSON parser
     * @param ctxt Deserialization context
     * @return UnitType enum value, or UNKNOWN if the value cannot be parsed
     */
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): ModelPricing.Pricing.UnitType {
        val value = p.text?.uppercase()
        return try {
            ModelPricing.Pricing.UnitType.valueOf(value ?: "")
        } catch (e: IllegalArgumentException) {
            ModelPricing.Pricing.UnitType.UNKNOWN
        }
    }
}

/**
 * Custom Jackson deserializer for ModelVendor enum.
 * Returns ModelVendor.UNKNOWN for any unrecognized values.
 */
class ModelVendorDeserializer : JsonDeserializer<ModelVendor>() {
    /**
     * Deserializes JSON string to ModelVendor enum.
     * 
     * @param p JSON parser
     * @param ctxt Deserialization context
     * @return ModelVendor enum value, or UNKNOWN if the value cannot be parsed
     */
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): ModelVendor {
        val value = p.text?.uppercase()
        return try {
            ModelVendor.valueOf(value ?: "")
        } catch (e: IllegalArgumentException) {
            ModelVendor.UNKNOWN
        }
    }
}

