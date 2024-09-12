package com.mlp.sdk

sealed interface MlpResponse

object BillingUnitsThreadLocal {
    private val tl = ThreadLocal<Long>()
    fun clear() {
        tl.set(null)
    }

    fun setUnits(units: Long) {
        tl.set(units)
    }

    fun getUnits(): Long? {
        return tl.get()
    }
}

sealed interface PayloadInterface {
    val dataType: String?
    fun stringData(): String
}

data class StreamPayloadInterface(val payload: PayloadInterface, val last: Boolean)

data class PayloadWithConfig(val payload: PayloadInterface, val config: Payload?)

data class Payload(
    override val dataType: String?,
    val data: String,
): MlpResponse, PayloadInterface {
    constructor(data: String) : this(null, data)

    override fun stringData(): String = data

    companion object {
        val emptyPayload = Payload("{}")
    }
}

data class ProtobufPayload(
    override val dataType: String?,
    val data: com.google.protobuf.ByteString,
): MlpResponse, PayloadInterface {
    override fun stringData(): String = data.toStringUtf8()
}


data class RawPayload(
    val dataType: String?,
    val data: String,
    val headers: Map<String, String> = emptyMap()
): MlpResponse {
    val asPayload
        get() = Payload(dataType, data)
}

data class MlpResponseException(val exception: Throwable) : MlpResponse

class MlpPartialBinaryResponse(): MlpResponse
