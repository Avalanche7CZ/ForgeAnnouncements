package avalanche7.net.forgeannoucements;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

@Mod("forgeannoucements")
public class ForgeAnnoucements {

    private static final Logger LOGGER = LogUtils.getLogger();

    public ForgeAnnoucements() {

        LOGGER.info("Initializing Forge Annoucements mod...");
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);

        try {
            ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ModConfigHandler.SERVER_CONFIG);
            ModConfigHandler.loadConfig(ModConfigHandler.SERVER_CONFIG, FMLPaths.CONFIGDIR.get().resolve("forgeannoucements-common.toml").toString());
        } catch (Exception e) {
            LOGGER.error("Failed to register or load configuration", e);
            throw new RuntimeException("Configuration loading failed", e);
        }

        long configInterval = ModConfigHandler.CONFIG.interval.get();
        LOGGER.info("Initial Config Interval Value: {}", configInterval);
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("Forge Annoucements mod has been enabled.");
        LOGGER.info("=========================");
        LOGGER.info("Forge Annoucements");
        LOGGER.info("Version 1.0.0");
        LOGGER.info("Author: Avalanche7CZ");
        LOGGER.info("=========================");
    }
}
