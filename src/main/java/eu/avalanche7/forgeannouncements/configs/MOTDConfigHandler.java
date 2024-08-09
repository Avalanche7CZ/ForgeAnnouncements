package eu.avalanche7.forgeannouncements.configs;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.tuple.Pair;

@Mod.EventBusSubscriber(modid = "forgeannouncements", bus = Mod.EventBusSubscriber.Bus.MOD)
public class MOTDConfigHandler {

    public static final ForgeConfigSpec SERVER_CONFIG;
    public static final Config CONFIG;

    static {
        final Pair<Config, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Config::new);
        SERVER_CONFIG = specPair.getRight();
        CONFIG = specPair.getLeft();
    }

    public static class Config {
        public final ForgeConfigSpec.ConfigValue<String> motdMessage;

        public Config(ForgeConfigSpec.Builder builder) {
            motdMessage = builder.comment("Message of the Day")
                    .define("MOTD.Message", "§a[title=Welcome Message]\n§7[subtitle=Server Information]\n§aWelcome to the server!\n§7Visit our website: §c[link=http://example.com] \n§bType [command=/help] for commands\n§eHover over this message [hover=This is a hover text!] to see more info.\n§7[divider]");
        }
    }

    public static void loadConfig(ForgeConfigSpec config, String path) {
        final CommentedFileConfig file = CommentedFileConfig.builder(path)
                .sync()
                .autosave()
                .writingMode(com.electronwill.nightconfig.core.io.WritingMode.REPLACE)
                .build();
        file.load();
        config.setConfig(file);
    }
}
