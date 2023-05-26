package com.mlp.sdk

sealed interface MlpResponse

data class Payload(val dataType: String?, val data: String,
                   val billingUnits: Long? = null): MlpResponse {
    constructor(data: String) : this(null, data)

    companion object {
        val emptyPayload = Payload("{}")
    }
}

data class MlpResponseException(val exception: Throwable): MlpResponse
