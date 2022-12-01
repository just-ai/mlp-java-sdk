package com.platform.mpl.sdk

sealed interface PlatformResponse

data class Payload(val dataType: String?, val data: String): PlatformResponse {
    constructor(data: String) : this(null, data)

    companion object {
        val emptyPayload = Payload("{}")
    }
}

data class PlatformResponseException(val exception: Throwable): PlatformResponse
