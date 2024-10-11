package com.mlp.sdk.datatypes.asr.common

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.protobuf.ByteString
import com.mlp.sdk.CommonErrorCode
import com.mlp.sdk.MlpException
import java.util.Base64

data class AsrRequest(val audioContent: ByteString, val config: RecognitionConfig?) {
    constructor(audioContent: ByteArray, config: RecognitionConfig?) : this(ByteString.copyFrom(audioContent), config)

    @JsonCreator
    constructor(@JsonProperty("audio_base64") audioBase64: String) : this(decodeBase64(audioBase64), null)

    companion object {
        @JvmStatic
        private val base64Decoder = Base64.getDecoder()
        @JvmStatic
        private fun decodeBase64(audioBase64: String) = kotlin.runCatching {
            base64Decoder.decode(audioBase64)
        }.getOrElse { throw MlpException(CommonErrorCode.BAD_REQUEST, mapOf("message" to it.toString())) }

        const val DATATYPE = "https://caila.io/specs/mlp-data-asr.proto#AsrRequestProto"
    }
}
