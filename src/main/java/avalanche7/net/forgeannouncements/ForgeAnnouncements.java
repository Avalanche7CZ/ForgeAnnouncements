package avalanche7.net.forgeannouncements;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

@Mod("forgeannouncements")
public class ForgeAnnouncements {

    private static final Logger LOGGER = LogUtils.getLogger();

    public ForgeAnnouncements() {

        LOGGER.info("Initializing Forge Announcement mod...");
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);

        try {
            ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ModConfigHandler.SERVER_CONFIG);
            ModConfigHandler.loadConfig(ModConfigHandler.SERVER_CONFIG, FMLPaths.CONFIGDIR.get().resolve("forgeannouncements-common.toml").toString());
        } catch (Exception e) {
            LOGGER.error("Failed to register or load configuration", e);
            throw new RuntimeException("Configuration loading failed", e);
        }

        long globalInterval = ModConfigHandler.CONFIG.globalInterval.get();
        boolean globalEnable = ModConfigHandler.CONFIG.globalEnable.get();
        LOGGER.info("Initial Global Config: Interval Value: {}, Enabled: {}", globalInterval, globalEnable);

        long actionbarInterval = ModConfigHandler.CONFIG.actionbarInterval.get();
        boolean actionbarEnable = ModConfigHandler.CONFIG.actionbarEnable.get();
        LOGGER.info("Initial Actionbar Config: Interval Value: {}, Enabled: {}", actionbarInterval, actionbarEnable);

        long titleInterval = ModConfigHandler.CONFIG.titleInterval.get();
        boolean titleEnable = ModConfigHandler.CONFIG.titleEnable.get();
        LOGGER.info("Initial Title Config: Interval Value: {}, Enabled: {}", titleInterval, titleEnable);

        long bossbarInterval = ModConfigHandler.CONFIG.bossbarInterval.get();
        boolean bossbarEnable = ModConfigHandler.CONFIG.bossbarEnable.get();
        LOGGER.info("Initial Bossbar Config: Interval Value: {}, Enabled: {}", bossbarInterval, bossbarEnable);
    }

    private void setup(final FMLCommonSetupEvent event) {

        String version = ModLoadingContext.get().getActiveContainer().getModInfo().getVersion().toString();
        String displayName = ModLoadingContext.get().getActiveContainer().getModInfo().getDisplayName();

        LOGGER.info("Forge Announcements mod has been enabled.");
        LOGGER.info("=========================");
        LOGGER.info(displayName);
        LOGGER.info("Version " + version);
        LOGGER.info("Author: Avalanche7CZ");
        LOGGER.info("=========================");

        UpdateChecker.checkForUpdates();
    }

    public class UpdateChecker {

        private static final String LATEST_VERSION_URL = "https://raw.githubusercontent.com/Avalanche7CZ/ForgeAnnouncements/main/version.txt";
        private static String CURRENT_VERSION;

        public static void checkForUpdates() {
            try {
                CURRENT_VERSION = ModLoadingContext.get().getActiveContainer().getModInfo().getVersion().toString();

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
