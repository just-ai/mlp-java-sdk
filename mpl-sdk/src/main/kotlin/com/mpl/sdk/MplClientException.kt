package com.mpl.sdk

class MplClientException(
        val errorCode: String,
        val errorMessage: String,
        val args: Map<String, String>,
) : RuntimeException(errorMessage)
