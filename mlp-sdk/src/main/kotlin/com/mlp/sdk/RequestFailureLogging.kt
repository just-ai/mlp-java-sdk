package com.mlp.sdk

import org.slf4j.Logger

private val CLIENT_ERROR_STATUS_CODES = 400..499

internal fun logRequestFailure(logger: Logger, requestType: String, error: Throwable, requestId: Long? = null) {
    val request = if (requestId == null) "$requestType request" else "$requestType request $requestId"
    val errorCode = (error as? MlpException)?.error?.errorCode
    if (errorCode != null && errorCode.statusCode in CLIENT_ERROR_STATUS_CODES) {
        logger.warn(
            "Client error while processing $request: status ${errorCode.statusCode}, code ${errorCode.code}"
        )
    } else {
        logger.error("Error while processing $request", error)
    }
}
