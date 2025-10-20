package com.mlp.sdk

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

data class PredictRequestMetadata(
    val requestId: String,
    val originalRequestId: String?,

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type",
    )
    @JsonSubTypes(
        JsonSubTypes.Type(value = NoBillingAccount::class, name = "NONE"),
        JsonSubTypes.Type(value = ApiTokenBillingAccount::class, name = "API_TOKEN"),
        JsonSubTypes.Type(value = BillingTokenBillingAccount::class, name = "BILLING_TOKEN"),
    )
    val billingAccount: BillingAccount,

    val isPaymentRequired: Boolean,

    val maxPricePerCallCurrency: Double,
    val tokenToCurrencyRate: Long,

    val model: PredictRequestModelMetadata,
)

enum class BillingAccountType {
    NONE,
    API_TOKEN,
    BILLING_TOKEN,
}

sealed interface BillingAccount {
    val type: BillingAccountType
}

data class NoBillingAccount(
    override val type: BillingAccountType = BillingAccountType.NONE,
) : BillingAccount

sealed interface ActualBillingAccount : BillingAccount {
    val callerAccountId: Long
    val apiTokenName: String
}

data class ApiTokenBillingAccount(
    override val type: BillingAccountType = BillingAccountType.API_TOKEN,
    override val callerAccountId: Long,
    override val apiTokenName: String,
) : ActualBillingAccount

data class BillingTokenBillingAccount(
    override val type: BillingAccountType = BillingAccountType.BILLING_TOKEN,
    override val callerAccountId: Long,
    override val apiTokenName: String,
    val billingAccountId: Long,
    val billingTokenName: String,
) : ActualBillingAccount

data class PredictRequestModelMetadata(
    val id: Long,
    val accountId: Long,
    val name: String,
    val accountShortName: String,
    val resourceGroup: String?,
    val billingUnitPriceInNanoToken: Long,
)
