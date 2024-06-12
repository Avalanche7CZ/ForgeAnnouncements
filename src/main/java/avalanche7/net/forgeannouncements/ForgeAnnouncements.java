package avalanche7.net.forgeannouncements;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.common.config.Configuration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;


@Mod(modid = ForgeAnnouncements.MODID, name = ForgeAnnouncements.NAME, version = ForgeAnnouncements.VERSION, acceptableRemoteVersions = "*")
public class ForgeAnnouncements {

    public static final String MODID = "forgeannouncements";
    public static final String NAME = "Forge Announcements";
    public static final String VERSION = "12.0.0";

    private static final Logger LOGGER = LogManager.getLogger(MODID);

    private Configuration config;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("Initializing Forge Announcements mod...");
        config = new Configuration(event.getSuggestedConfigurationFile());
        ModConfigHandler.init(config);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        LOGGER.info("Forge Announcements mod has been enabled.");
        LOGGER.info("=========================");
        LOGGER.info("ForgeAnnouncements");
        LOGGER.info("Version: " + VERSION);
        LOGGER.info("Author: Avalanche7CZ");
        LOGGER.info("=========================");

        UpdateChecker.checkForUpdates();
        Announcements.onServerStarting(event);
    }

    public static class UpdateChecker {

        private static final Logger LOGGER = LogManager.getLogger(MODID);
        private static final String LATEST_VERSION_URL = "https://raw.githubusercontent.com/Avalanche7CZ/ForgeAnnouncements/1.12.2/version.txt";
        private static String CURRENT_VERSION = VERSION;

        public static void checkForUpdates() {
            try {
                URL url = new URL(LATEST_VERSION_URL);
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                String latestVersion = reader.readLine();
                reader.close();

                if (!CURRENT_VERSION.equals(latestVersion)) {
                    LOGGER.info("A new version of the mod is available: " + latestVersion);
                } else {
                    LOGGER.info("You are running the latest version of the mod: " + CURRENT_VERSION);
                }
            } catch (Exception e) {
                LOGGER.info("Failed to check for updates.");
            }
        }
    }
}
