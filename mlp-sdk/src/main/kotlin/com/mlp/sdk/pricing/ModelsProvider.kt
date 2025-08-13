package com.mlp.sdk.pricing

class ModelsProvider(private val configuration: PricingConfiguration) {

    fun modelsList(modelVendor: ModelPricing.ModelVendor) =
        configuration.modelsPricing
            .filter {
                val pricingVendor = it.modelVendor ?: configuration.modelVendor
                modelVendor == pricingVendor || pricingVendor == null
            }
            .map { it.model }
}
