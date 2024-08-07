package avalanche7.net.forgeannouncements.utils;

import avalanche7.net.forgeannouncements.configs.MainConfigHandler;
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