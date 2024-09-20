package com.mlp.sdk.datatypes.asr.common

import com.google.protobuf.ByteString

data class AsrRequest(val audioContent: ByteString, val config: RecognitionConfig?) {
    constructor(audioContent: ByteArray, config: RecognitionConfig?) : this(ByteString.copyFrom(audioContent), config)

    companion object {
        const val ASR_DATATYPE = "ASR_REQUEST" // TODO
    }
}
