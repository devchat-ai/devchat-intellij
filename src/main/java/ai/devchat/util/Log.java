package ai.devchat.util;

import ai.devchat.cli.DevChat;
import com.intellij.openapi.diagnostic.LogLevel;
import com.intellij.openapi.diagnostic.Logger;

public class Log {
    private static final Logger LOG = Logger.getInstance(DevChat.class);
    private static final String PREFIX = "[DevChat] ";

    private static void setLevel(LogLevel level) {
        LOG.setLevel(level);
    }

    public static void setLevelInfo() {
        LOG.setLevel(LogLevel.INFO);
    }

    public static void setLevelDebug() {
        LOG.setLevel(LogLevel.DEBUG);
    }

    public static void info(String message) {
        LOG.info(PREFIX + message);
    }

    public static void error(String message) {
        LOG.error(PREFIX + message);
    }

    public static void debug(String message) {
        LOG.debug(PREFIX + message);
    }
}
