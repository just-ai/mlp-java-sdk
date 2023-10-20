package com.mlp.sdk

import com.mlp.gate.SimpleStatusProto

class MlpException(
    val error: MlpError
): RuntimeException(error.errorCode.message) {

    constructor(error: MlpErrorCode, args: Map<String, String> = emptyMap()) : this(MlpError(error, args))

    constructor(message: String) : this(MlpError(CommonErrorCode.INTERNAL_ERROR, mapOf("message" to message)))
}

class MlpError(
    val errorCode: MlpErrorCode,
    val args: Map<String, String> = emptyMap()
) {
    constructor(error: MlpErrorCode, ex: Exception) :
            this(error, mapOf("message" to (ex.message ?: ""), "class" to ex.javaClass.name))

    constructor(error: MlpErrorCode, vararg pairs: Pair<String, String>) :
            this(error, pairs.toMap())
}

interface MlpErrorCode {
    val code: String
    val message: String
    val status: SimpleStatusProto
}

enum class CommonErrorCode(
        override val code: String,
        override val message: String,
        override val status: SimpleStatusProto): MlpErrorCode {
    INTERNAL_ERROR("mlp-action.common.internal-error", "Internal error. Message: \${message}", SimpleStatusProto.INTERNAL_SERVER_ERROR),

    BAD_REQUEST("mlp-action.common.bad-request", "Bad request", SimpleStatusProto.BAD_REQUEST),

    PROCESSING_EXCEPTION("mlp-action.common.processing-exception", "Something went wrong during processing the request", SimpleStatusProto.INTERNAL_SERVER_ERROR),

    REQUEST_TYPE_NOT_SUPPORTED("mlp-action.common.method-not-supported", "\${type} requests are not supported by this action", SimpleStatusProto.BAD_REQUEST),
}
