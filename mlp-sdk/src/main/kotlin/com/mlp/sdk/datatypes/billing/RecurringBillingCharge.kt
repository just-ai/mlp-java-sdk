package com.mlp.sdk.datatypes.billing

data class RecurringBillingCharge(
    val chargeId: String,
    val amountInUnits: Long,
    val billingDetails: Map<String, Long>? = emptyMap(),
)