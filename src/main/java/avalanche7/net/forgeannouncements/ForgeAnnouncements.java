package avalanche7.net.forgeannouncements;

import avalanche7.net.forgeannouncements.configs.MOTDConfigHandler;
import avalanche7.net.forgeannouncements.configs.AnnouncementsConfigHandler;
import avalanche7.net.forgeannouncements.configs.MentionConfigHandler;
import avalanche7.net.forgeannouncements.utils.Mentions;
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod("forgeannouncements")
public class ForgeAnnouncements {

    private static final Logger LOGGER = LogUtils.getLogger();

    public ForgeAnnouncements() {
        LOGGER.info("Initializing Forge Announcement mod...");
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(Mentions.class);

        try {
            createDefaultConfigs();

            ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, AnnouncementsConfigHandler.SERVER_CONFIG, "forgeannouncements/announcements.toml");
            ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, MOTDConfigHandler.SERVER_CONFIG, "forgeannouncements/motd.toml");
            ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, MentionConfigHandler.SERVER_CONFIG, "forgeannouncements/mentions.toml");

            AnnouncementsConfigHandler.loadConfig(AnnouncementsConfigHandler.SERVER_CONFIG, FMLPaths.CONFIGDIR.get().resolve("forgeannouncements/announcements.toml").toString());
            MOTDConfigHandler.loadConfig(MOTDConfigHandler.SERVER_CONFIG, FMLPaths.CONFIGDIR.get().resolve("forgeannouncements/motd.toml").toString());
            MentionConfigHandler.loadConfig(MentionConfigHandler.SERVER_CONFIG, FMLPaths.CONFIGDIR.get().resolve("forgeannouncements/mentions.toml").toString());
        } catch (Exception e) {
            LOGGER.error("Failed to register or load configuration", e);
            throw new RuntimeException("Configuration loading failed", e);
        }

        logInitialConfigs();
    }

    private void createDefaultConfigs() throws IOException {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve("forgeannouncements");
        if (!Files.exists(configDir)) {
            Files.createDirectories(configDir);
        }

        Path announcementsConfig = configDir.resolve("announcements.toml");
        if (!Files.exists(announcementsConfig)) {
            Files.createFile(announcementsConfig);
            AnnouncementsConfigHandler.loadConfig(AnnouncementsConfigHandler.SERVER_CONFIG, announcementsConfig.toString());
            AnnouncementsConfigHandler.SERVER_CONFIG.save();
        }

        Path motdConfig = configDir.resolve("motd.toml");
        if (!Files.exists(motdConfig)) {
            Files.createFile(motdConfig);
            MOTDConfigHandler.loadConfig(MOTDConfigHandler.SERVER_CONFIG, motdConfig.toString());
            MOTDConfigHandler.SERVER_CONFIG.save();
        }

        Path mentionsConfig = configDir.resolve("mentions.toml");
        if (!Files.exists(mentionsConfig)) {
            Files.createFile(mentionsConfig);
            MentionConfigHandler.loadConfig(MentionConfigHandler.SERVER_CONFIG, mentionsConfig.toString());
            MentionConfigHandler.SERVER_CONFIG.save();
        }
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

    private void logInitialConfigs() {
        long globalInterval = AnnouncementsConfigHandler.CONFIG.globalInterval.get();
        boolean globalEnable = AnnouncementsConfigHandler.CONFIG.globalEnable.get();
        LOGGER.info("Initial Global Config: Interval Value: {}, Enabled: {}", globalInterval, globalEnable);

        long actionbarInterval = AnnouncementsConfigHandler.CONFIG.actionbarInterval.get();
        boolean actionbarEnable = AnnouncementsConfigHandler.CONFIG.actionbarEnable.get();
        LOGGER.info("Initial Actionbar Config: Interval Value: {}, Enabled: {}", actionbarInterval, actionbarEnable);

        long titleInterval = AnnouncementsConfigHandler.CONFIG.titleInterval.get();
        boolean titleEnable = AnnouncementsConfigHandler.CONFIG.titleEnable.get();
        LOGGER.info("Initial Title Config: Interval Value: {}, Enabled: {}", titleInterval, titleEnable);

        long bossbarInterval = AnnouncementsConfigHandler.CONFIG.bossbarInterval.get();
        boolean bossbarEnable = AnnouncementsConfigHandler.CONFIG.bossbarEnable.get();
        LOGGER.info("Initial Bossbar Config: Interval Value: {}, Enabled: {}", bossbarInterval, bossbarEnable);
    }

    public static class UpdateChecker {

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
