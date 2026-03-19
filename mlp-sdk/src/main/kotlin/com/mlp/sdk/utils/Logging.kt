package com.mlp.sdk.utils

import com.google.protobuf.MessageLite

internal fun WithLogger.logProto(
    body: MessageLite,
    prompt: String,
    noContentLogging: Boolean = false
) {
    if (noContentLogging) {
        logger.debug("$prompt: <content-hidden>")
        return
    }

    // This size is always smaller than string version
    val approximateSize = body.serializedSize
    if (approximateSize > 1000) {
        logger.debug("$prompt: data length at least $approximateSize")
        return
    }

    // Stringify can produce OOM for large bodies
    val minimizedRequest = body.toString()
        .replace("\n", " ")
        .replace("  ", " ")
    val messageFitted = minimizedRequest.length <= 1000

    if (messageFitted)
        logger.debug("$prompt: \t$minimizedRequest")
    else
        logger.debug("$prompt: data length ${minimizedRequest.length}")
}
