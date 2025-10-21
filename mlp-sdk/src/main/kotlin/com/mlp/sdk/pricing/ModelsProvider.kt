package com.mlp.sdk.pricing

import com.mlp.api.datatypes.pricing.ModelPricing
import com.mlp.api.datatypes.pricing.PricingConfiguration

class ModelsProvider(private val configuration: PricingConfiguration) {

    fun modelsList(modelVendor: ModelPricing.ModelVendor) =
        configuration.modelsPricing
            .filter {
                val pricingVendor = it.modelVendor ?: configuration.modelVendor
                modelVendor == pricingVendor || pricingVendor == null
            }
            .map { it.model }
}
