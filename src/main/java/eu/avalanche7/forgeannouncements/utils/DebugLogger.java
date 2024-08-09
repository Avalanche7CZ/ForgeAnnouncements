package eu.avalanche7.forgeannouncements.utils;

import eu.avalanche7.forgeannouncements.configs.MainConfigHandler;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class DebugLogger {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void debugLog(String message) {
        if (MainConfigHandler.CONFIG.debugEnable.get()) {
            LOGGER.info(message);
        }
    }

    public static void debugLog(String message, Exception e) {
        if (MainConfigHandler.CONFIG.debugEnable.get()) {
            LOGGER.error(message, e);
        }
    }

    public static void debugLog(String message, Object... args) {
        if (MainConfigHandler.CONFIG.debugEnable.get()) {
            LOGGER.info(message, args);
        }
    }
}
