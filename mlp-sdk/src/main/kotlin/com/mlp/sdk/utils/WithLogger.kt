package com.mlp.sdk.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface WithLogger {
    val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)
}