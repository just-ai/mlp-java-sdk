package com.justai.caila.sdk

class CailaClientException(
        val errorCode: String,
        val errorMessage: String,
        val args: Map<String, String>,
) : RuntimeException(errorMessage)
