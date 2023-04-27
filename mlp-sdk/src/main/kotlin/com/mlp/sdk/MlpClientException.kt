package com.mlp.sdk

class MlpClientException(
        val errorCode: String,
        val errorMessage: String,
        val args: Map<String, String>,
        val requestId: String?
) : RuntimeException(errorMessage)
