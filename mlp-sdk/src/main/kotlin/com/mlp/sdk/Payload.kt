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

data class Payload(
    val dataType: String?,
    val data: String,
) {
    constructor(data: String) : this(null, data)

    companion object {
        val emptyPayload = Payload("{}")
    }
}

data class ResponsePayload(
    val dataType: String?,
    val data: String,
    val headers: Map<String, String> = emptyMap()
) : MlpResponse {
    constructor(data: String) : this(null, data)

    companion object {
        val emptyPayload = Payload("{}")
    }
}

data class MlpResponseException(val exception: Throwable) : MlpResponse

class MlpPartialBinaryResponse(): MlpResponse
