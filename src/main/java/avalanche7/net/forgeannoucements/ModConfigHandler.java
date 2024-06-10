package avalanche7.net.forgeannoucements;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.List;

@Mod.EventBusSubscriber(modid = "forgeannoucements", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModConfigHandler {

    public static final ForgeConfigSpec SERVER_CONFIG;
    public static final Config CONFIG;
    private static final Logger LOGGER = LogUtils.getLogger();

    static {
        final Pair<Config, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Config::new);
        SERVER_CONFIG = specPair.getRight();
        CONFIG = specPair.getLeft();
    }

    public static class Config {
        public final ForgeConfigSpec.BooleanValue enable;
        public final ForgeConfigSpec.BooleanValue headerAndFooter;
        public final ForgeConfigSpec.IntValue interval;
        public final ForgeConfigSpec.ConfigValue<String> prefix;
        public final ForgeConfigSpec.ConfigValue<String> header;
        public final ForgeConfigSpec.ConfigValue<String> footer;
        public final ForgeConfigSpec.ConfigValue<String> sound;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> messages;

        public Config(ForgeConfigSpec.Builder builder) {
            builder.comment("Auto Broadcast Settings")
                    .push("Auto_Broadcast");

            enable = builder.comment("Enable auto broadcast")
                    .define("Global_Messages.Enable", true);

            headerAndFooter = builder.comment("Enable header and footer")
                    .define("Global_Messages.Header_And_Footer", true);

            interval = builder.comment("Interval in seconds")
                    .defineInRange("Global_Messages.Interval", 1800, 1, Integer.MAX_VALUE);

            prefix = builder.comment("Prefix for messages")
                    .define("Global_Messages.Prefix", "§9§l[§b§lPREFIX§9§l]");

            header = builder.comment("Header for messages")
                    .define("Global_Messages.Header", "§7*§7§m---------------------------------------------------§7*");

            footer = builder.comment("Footer for messages")
                    .define("Global_Messages.Footer", "§7*§7§m---------------------------------------------------§7*");

            sound = builder.comment("Sound to play")
                    .define("Global_Messages.Sound", "");

            messages = builder.comment("Messages to broadcast")
                    .defineList("Global_Messages.Messages",
                            List.of(
                                    "{Prefix} §7Website: https://link/."
                            ),
                            obj -> obj instanceof String);

            builder.pop();
        }
    }

    public static void loadConfig(ForgeConfigSpec config, String path) {
        LOGGER.info("Loading configuration from file: {}", path);
        try {
            final CommentedFileConfig file = CommentedFileConfig.builder(path)
                    .sync()
                    .autosave()
                    .writingMode(com.electronwill.nightconfig.core.io.WritingMode.REPLACE)
                    .build();
            file.load();
            config.setConfig(file);
            LOGGER.info("Configuration loaded successfully from file: {}", path);
            LOGGER.info("Enable: {}", CONFIG.enable.get());
            LOGGER.info("Interval: {}", CONFIG.interval.get());
            LOGGER.info("Messages: {}", CONFIG.messages.get());
        } catch (Exception e) {
            LOGGER.error("Failed to load configuration from file: {}", path, e);
            throw new RuntimeException("Configuration loading failed", e);
        }
    }
}
