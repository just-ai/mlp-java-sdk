package com.mpl.sdk

sealed interface MplResponse

data class Payload(val dataType: String?, val data: String): MplResponse {
    constructor(data: String) : this(null, data)

    companion object {
        val emptyPayload = Payload("{}")
    }
}

data class MplResponseException(val exception: Throwable): MplResponse
