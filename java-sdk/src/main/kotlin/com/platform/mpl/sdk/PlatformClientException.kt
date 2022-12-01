package com.platform.mpl.sdk

class PlatformClientException(
        val errorCode: String,
        val errorMessage: String,
        val args: Map<String, String>,
) : RuntimeException(errorMessage)
