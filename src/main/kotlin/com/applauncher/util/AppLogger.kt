package com.applauncher.util

import java.util.logging.Level
import java.util.logging.Logger

object AppLogger {
    private val logger: Logger = Logger.getLogger("AppLauncher")

    fun error(message: String, throwable: Throwable? = null) {
        logger.log(Level.SEVERE, message, throwable)
    }

    fun warn(message: String, throwable: Throwable? = null) {
        logger.log(Level.WARNING, message, throwable)
    }

    fun info(message: String) {
        logger.log(Level.INFO, message)
    }
}
