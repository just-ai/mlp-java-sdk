package com.justai.caila.sdk

import com.justai.caila.gate.SimpleStatusProto

class CailaException(
        val error: CailaError
): RuntimeException(error.errorCode.message) {

    constructor(error: CailaErrorCode, args: Map<String, String> = emptyMap()) : this(CailaError(error, args))

    constructor(message: String) : this(CailaError(CommonErrorCode.INTERNAL_ERROR, mapOf("message" to message)))
}

class CailaError(
        val errorCode: CailaErrorCode,
        val args: Map<String, String> = emptyMap()
) {
    constructor(error: CailaErrorCode, ex: Exception) :
            this(error, mapOf("message" to (ex.message ?: ""), "class" to ex.javaClass.name))

    constructor(error: CailaErrorCode, vararg pairs: Pair<String, String>) :
            this(error, pairs.toMap())
}

interface CailaErrorCode {
    val code: String
    val message: String
    val status: SimpleStatusProto
}

enum class CommonErrorCode(
        override val code: String,
        override val message: String,
        override val status: SimpleStatusProto): CailaErrorCode {
    INTERNAL_ERROR("caila-action.common.internal-error", "Internal error. Message: \${message}", SimpleStatusProto.INTERNAL_SERVER_ERROR),

    BAD_REQUEST("caila-action.common.bad-request", "Bad request", SimpleStatusProto.BAD_REQUEST),

    PROCESSING_EXCEPTION("caila-action.common.processing-exception", "Something went wrong during processing the request", SimpleStatusProto.INTERNAL_SERVER_ERROR),

    REQUEST_TYPE_NOT_SUPPORTED("caila-action.common.method-not-supported", "\${type} requests are not supported by this action", SimpleStatusProto.BAD_REQUEST),
}
