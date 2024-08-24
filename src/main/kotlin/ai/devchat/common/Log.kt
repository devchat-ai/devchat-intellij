package ai.devchat.common

import ai.devchat.common.Constants.ASSISTANT_NAME_ZH
import com.intellij.openapi.diagnostic.LogLevel
import com.intellij.openapi.diagnostic.Logger

object Log {
    private val LOG = Logger.getInstance(ASSISTANT_NAME_ZH)
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
        LOG.info(message)
    }

    @JvmStatic
    fun error(message: String) {
        LOG.error(message)
    }

    @JvmStatic
    fun warn(message: String) {
        LOG.warn(message)
    }

    fun debug(message: String) {
        LOG.debug(message)
    }
}
