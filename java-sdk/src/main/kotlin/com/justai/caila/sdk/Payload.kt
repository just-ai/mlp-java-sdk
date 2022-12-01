package com.justai.caila.sdk

sealed interface CailaResponse

data class Payload(val dataType: String?, val data: String): CailaResponse {
    constructor(data: String) : this(null, data)

    companion object {
        val emptyPayload = Payload("{}")
    }
}

data class CailaResponseException(val exception: Throwable): CailaResponse
