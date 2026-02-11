package com.mlp.sdk

sealed interface MlpResponse {
    val headers: Map<String, String>?
        get() = emptyMap()
    val statusCode: Int?
        get() = 200
}

object BillingUnitsThreadLocal {

    private val units = ThreadLocal<Long>()

    private val detailedUnits = ThreadLocal<Map<String, Long>>()

    private val deferredBillingRequestId = ThreadLocal<String>()

    private val billingCurrencyType = ThreadLocal<String>()

    fun clearAll() {
        clearUnits()
        clearDetails()
        clearDeferredBillingRequestId()
        clearBillingCurrencyType()
    }

    fun clearUnits() {
        units.set(null)
    }

    fun clearDetails() {
        detailedUnits.set(null)
    }

    fun clearDeferredBillingRequestId() {
        deferredBillingRequestId.set(null)
    }

    fun clearBillingCurrencyType() {
        billingCurrencyType.set(null)
    }

    fun setUnits(units: Long) {
        this.units.set(units)
    }

    fun setDetailedUnits(map: Map<String, Long>) {
        this.detailedUnits.set(map)
    }

    fun setDeferredBillingRequestId(id: String) {
        this.deferredBillingRequestId.set(id)
    }

    fun setBillingCurrencyType(currencyType: String) {
        this.billingCurrencyType.set(currencyType)
    }

    fun getUnits(): Long? {
        return units.get()
    }

    fun getDetailedUnits(): Map<String, Long>? {
        return detailedUnits.get()
    }

    fun getDeferredBillingRequestId(): String? {
        return deferredBillingRequestId.get()
    }

    fun getBillingCurrencyType(): String? {
        return billingCurrencyType.get()
    }
}

sealed interface PayloadInterface {
    val dataType: String?
    fun stringData(): String
}

data class StreamPayloadInterface(val payload: PayloadInterface, val last: Boolean)

data class PayloadWithConfig(val payload: PayloadInterface, val config: PayloadInterface?)

data class Payload(
    override val dataType: String?,
    val data: String,
    val contentHidden: Boolean = false
): MlpResponse, PayloadInterface {
    constructor(data: String) : this(null, data)

    override fun stringData(): String = data

    override fun toString(): String {
        if (contentHidden) return "Payload(dataType=$dataType, data=\"content-hidden\")"

        return "Payload(dataType=$dataType, data=$data)"
    }

    companion object {
        const val EMPTY_DATA_TYPE = "application/x-empty"

        val emptyPayload = Payload(EMPTY_DATA_TYPE, "{}")
    }
}

data class ProtobufPayload(
    override val dataType: String?,
    val data: com.google.protobuf.ByteString,
    val contentHidden: Boolean = false
): MlpResponse, PayloadInterface {
    override fun stringData(): String = data.toStringUtf8()

    override fun toString(): String {
        if (contentHidden) return "ProtobufPayload(dataType=$dataType, data=\"content-hidden\")"

        return "ProtobufPayload(dataType=$dataType, data=$data)"
    }
}


data class RawPayload(
    val data: String,
    override val dataType: String?,
    override val headers: Map<String, String> = emptyMap(),
    override val statusCode: Int = 200,
): MlpResponse, PayloadInterface {
    override fun stringData(): String = data

    val asPayload
        get() = Payload(dataType, data)
}

data class MlpResponseException(val exception: Throwable) : MlpResponse

class MlpPartialBinaryResponse(
    override val headers: Map<String, String>? = null,
    override val statusCode: Int? = null
) : MlpResponse

data class MlpHttpResponse(
    val statusCode: Int = 200,
    val body: Any? = null,
    val headers: Map<String, String> = emptyMap()
)
