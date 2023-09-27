package com.mlp.sdk.utils

import org.slf4j.ILoggerFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Deprecated("Use WithSdkContext") // TODO проверить что имя не изменилось
interface WithLogger {

    /**
     * You can specify your factory implementation to define your loggers
     */
    val loggerFactory: ILoggerFactory?
        get() = null

    val logger: Logger
        get() = loggerFactory?.getLogger(javaClass.name)
            ?: LoggerFactory.getLogger(javaClass)
}
