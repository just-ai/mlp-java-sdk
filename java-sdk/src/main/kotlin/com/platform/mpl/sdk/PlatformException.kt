package com.platform.mpl.sdk

import com.platform.mpl.gate.SimpleStatusProto

class PlatformException(
        val error: PlatformError
): RuntimeException(error.errorCode.message) {

    constructor(error: PlatformErrorCode, args: Map<String, String> = emptyMap()) : this(PlatformError(error, args))

    constructor(message: String) : this(PlatformError(CommonErrorCode.INTERNAL_ERROR, mapOf("message" to message)))
}

class PlatformError(
    val errorCode: PlatformErrorCode,
    val args: Map<String, String> = emptyMap()
) {
    constructor(error: PlatformErrorCode, ex: Exception) :
            this(error, mapOf("message" to (ex.message ?: ""), "class" to ex.javaClass.name))

    constructor(error: PlatformErrorCode, vararg pairs: Pair<String, String>) :
            this(error, pairs.toMap())
}

interface PlatformErrorCode {
    val code: String
    val message: String
    val status: SimpleStatusProto
}

enum class CommonErrorCode(
        override val code: String,
        override val message: String,
        override val status: SimpleStatusProto): PlatformErrorCode {
    INTERNAL_ERROR("mpl-action.common.internal-error", "Internal error. Message: \${message}", SimpleStatusProto.INTERNAL_SERVER_ERROR),

    BAD_REQUEST("mpl-action.common.bad-request", "Bad request", SimpleStatusProto.BAD_REQUEST),

    PROCESSING_EXCEPTION("mpl-action.common.processing-exception", "Something went wrong during processing the request", SimpleStatusProto.INTERNAL_SERVER_ERROR),

    REQUEST_TYPE_NOT_SUPPORTED("mpl-action.common.method-not-supported", "\${type} requests are not supported by this action", SimpleStatusProto.BAD_REQUEST),
}
