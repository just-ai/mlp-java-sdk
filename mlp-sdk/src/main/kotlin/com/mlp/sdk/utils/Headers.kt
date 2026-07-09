package com.mlp.sdk.utils

const val CONTENT_HIDDEN_HEADER = "Content-Hidden"
const val REQUEST_ID_HEADER = "Z-requestId"
const val CALLER_ACCOUNT_ID_HEADER = "Z-callerAccountId"
const val SERVER_TIME_HEADER = "Z-Server-Time"
const val MLP_BILLING_KEY_HEADER = "MLP-BILLING-KEY"
const val CUSTOM_BILLING_HEADER = "Z-custom-billing"
const val CUSTOM_BILLING_DETAILS_HEADER = "Z-custom-billing-details"

/**
 * Себестоимость запроса (provider self-cost), micro-currency того же масштаба, что [CUSTOM_BILLING_HEADER].
 * Отдельный от клиентской стоимости канал: gateway агрегирует его только для админ-observability
 * (метрики/лог), клиенту НЕ передаётся. Необязательный — при отсутствии gateway делает BC-fallback
 * (self-cost = client cost).
 */
const val CUSTOM_SELF_COST_HEADER = "Z-custom-self-cost"
const val DEFERRED_BILLING_ID_HEADER = "Z-deferred-billing-id"
const val RECURRING_BILLING_ID_HEADER = "Z-recurring-billing-id"
const val BILLING_CURRENCY_TYPE_HEADER = "Z-billing-currency-type"

const val MLP_API_KEY_NAME_HEADER = "MLP-API-KEY-NAME"
const val MLP_BILLING_KEY_NAME_HEADER = "MLP-BILLING-KEY-NAME"
const val MLP_BILLING_ACCOUNT_ID_HEADER = "MLP-BILLING-ACCOUNT-ID"
const val MLP_BILLING_USER_ID_HEADER = "MLP-BILLING-USER-ID"
