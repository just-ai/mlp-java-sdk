package com.mlp.sdk.utils

const val CONTENT_HIDDEN_HEADER = "Content-Hidden"
const val REQUEST_ID_HEADER = "Z-requestId"
const val CALLER_ACCOUNT_ID_HEADER = "Z-callerAccountId"
const val SERVER_TIME_HEADER = "Z-Server-Time"
const val MLP_BILLING_KEY_HEADER = "MLP-BILLING-KEY"
const val CUSTOM_BILLING_HEADER = "Z-custom-billing"
const val CUSTOM_BILLING_DETAILS_HEADER = "Z-custom-billing-details"

/**
 * Себестоимость запроса (provider self-cost) в micro НАТИВНОЙ валюты вендора — той, в которой он
 * тарифицирует нас, — а валюта едет в [CUSTOM_SELF_COST_CURRENCY_HEADER].
 *
 * ВНИМАНИЕ, единица изменилась (CAILA-6308): раньше здесь было заявлено «micro-currency того же
 * масштаба, что [CUSTOM_BILLING_HEADER]», то есть валюта платформы. На практике адаптеры отдавали
 * валюту вендора (veai — USD), потому что вендорский инвойс сверяют в ней, а не в рублях; при этом
 * потребитель считал значение рублёвым и занижал себестоимость в курс раз. Теперь валюта передаётся
 * явно, а сумма без валюты потребителем игнорируется.
 *
 * Отдельный от клиентской стоимости канал: клиенту НЕ передаётся, только админ-агрегация в биллинге
 * и метрики. Необязательный: нет header'ов — нет данных о себестоимости (не ноль и не клиентская
 * цена).
 */
const val CUSTOM_SELF_COST_HEADER = "Z-custom-self-cost"

/**
 * ISO 4217 валюты [CUSTOM_SELF_COST_HEADER] (например `USD`, `RUB`). Без неё сумма себестоимости не
 * интерпретируема — micro-USD не отличить от micro-RUB, — поэтому потребитель такую пару отбрасывает.
 */
const val CUSTOM_SELF_COST_CURRENCY_HEADER = "Z-custom-self-cost-currency"
const val DEFERRED_BILLING_ID_HEADER = "Z-deferred-billing-id"
const val RECURRING_BILLING_ID_HEADER = "Z-recurring-billing-id"
const val BILLING_CURRENCY_TYPE_HEADER = "Z-billing-currency-type"

const val MLP_API_KEY_NAME_HEADER = "MLP-API-KEY-NAME"
const val MLP_BILLING_KEY_NAME_HEADER = "MLP-BILLING-KEY-NAME"
const val MLP_BILLING_ACCOUNT_ID_HEADER = "MLP-BILLING-ACCOUNT-ID"
const val MLP_BILLING_USER_ID_HEADER = "MLP-BILLING-USER-ID"
