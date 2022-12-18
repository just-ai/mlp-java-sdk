package com.mpl.sdk

import com.mpl.gate.SimpleStatusProto

class MplException(
        val error: MplError
): RuntimeException(error.errorCode.message) {

    constructor(error: MplErrorCode, args: Map<String, String> = emptyMap()) : this(MplError(error, args))

    constructor(message: String) : this(MplError(CommonErrorCode.INTERNAL_ERROR, mapOf("message" to message)))
}

class MplError(
    val errorCode: MplErrorCode,
    val args: Map<String, String> = emptyMap()
) {
    constructor(error: MplErrorCode, ex: Exception) :
            this(error, mapOf("message" to (ex.message ?: ""), "class" to ex.javaClass.name))

    constructor(error: MplErrorCode, vararg pairs: Pair<String, String>) :
            this(error, pairs.toMap())
}

interface MplErrorCode {
    val code: String
    val message: String
    val status: SimpleStatusProto
}

enum class CommonErrorCode(
        override val code: String,
        override val message: String,
        override val status: SimpleStatusProto): MplErrorCode {
    INTERNAL_ERROR("mpl-action.common.internal-error", "Internal error. Message: \${message}", SimpleStatusProto.INTERNAL_SERVER_ERROR),

    BAD_REQUEST("mpl-action.common.bad-request", "Bad request", SimpleStatusProto.BAD_REQUEST),

    PROCESSING_EXCEPTION("mpl-action.common.processing-exception", "Something went wrong during processing the request", SimpleStatusProto.INTERNAL_SERVER_ERROR),

    REQUEST_TYPE_NOT_SUPPORTED("mpl-action.common.method-not-supported", "\${type} requests are not supported by this action", SimpleStatusProto.BAD_REQUEST),
}
