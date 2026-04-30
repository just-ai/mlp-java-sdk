package com.mlp.sdk.datatypes.billing

data class DeferredBillingCharge(
    val idempotencyKey: String,
    val amount: Long,
    val currency: String?,
    val llmModelName: String?,
    val billingDetails: Map<String, Long>? = emptyMap(),
)

data class DeferredBillingParams(
    val callerAccountId: Long?,
    val apiKeyName: String?,
    val billingKeyName: String?,
    val billingAccountId: Long?,
    val billingUserId: String?,
)