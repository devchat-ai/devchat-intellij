package ai.devchat.common

import com.intellij.openapi.diagnostic.LogLevel
import com.intellij.openapi.diagnostic.Logger

object Log {
    private val LOG = Logger.getInstance("DevChat")
    private const val PREFIX = "[DevChat] "
    private fun setLevel(level: LogLevel) {
        LOG.setLevel(level)
    }

    @JvmStatic
    fun setLevelInfo() {
        LOG.setLevel(LogLevel.INFO)
    }

    fun setLevelDebug() {
        LOG.setLevel(LogLevel.DEBUG)
    }

    @JvmStatic
    fun info(message: String) {
        LOG.info(PREFIX + message)
    }

    @JvmStatic
    fun error(message: String) {
        LOG.error(PREFIX + message)
    }

    @JvmStatic
    fun warn(message: String) {
        LOG.warn(PREFIX + message)
    }

    fun debug(message: String) {
        LOG.debug(PREFIX + message)
    }
}
