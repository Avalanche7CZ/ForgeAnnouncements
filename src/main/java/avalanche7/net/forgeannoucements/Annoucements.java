package avalanche7.net.forgeannoucements;

import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mod.EventBusSubscriber(modid = "forgeannoucements", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Annoucements {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final Random random = new Random();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static MinecraftServer server;

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        server = event.getServer();
        LOGGER.info("Server is starting, scheduling announcements.");
        scheduleAnnouncements();
    }

    public static void scheduleAnnouncements() {
        long interval = ModConfigHandler.CONFIG.interval.get();
        LOGGER.info("Read interval from config: {}", interval);
        if (ModConfigHandler.CONFIG.enable.get()) {
            LOGGER.info("Scheduling announcements with interval: {} seconds", interval);
            scheduler.scheduleAtFixedRate(Annoucements::broadcastMessage, interval, interval, TimeUnit.SECONDS);
        } else {
            LOGGER.info("Auto broadcast is disabled.");
        }
    }

    private static MutableComponent parseMessageWithColor(String rawMessage) {
        MutableComponent message = new TextComponent("");
        String[] parts = rawMessage.split("§");

        Style style = Style.EMPTY;
        if (!rawMessage.startsWith("§")) {
            message.append(new TextComponent(parts[0]).setStyle(style));
        }

        for (int i = rawMessage.startsWith("§") ? 0 : 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }

            char colorCode = parts[i].charAt(0);
            String text = parts[i].substring(1);

            style = applyColorCode(style, colorCode);
            MutableComponent component = new TextComponent(text).setStyle(style);
            message.append(component);
        }

        return message;
    }

    private static Style applyColorCode(Style style, char colorCode) {
        switch (colorCode) {
            case '0': return style.withColor(TextColor.fromRgb(0x000000)); // Black
            case '1': return style.withColor(TextColor.fromRgb(0x0000AA)); // Dark Blue
            case '2': return style.withColor(TextColor.fromRgb(0x00AA00)); // Dark Green
            case '3': return style.withColor(TextColor.fromRgb(0x00AAAA)); // Dark Aqua
            case '4': return style.withColor(TextColor.fromRgb(0xAA0000)); // Dark Red
            case '5': return style.withColor(TextColor.fromRgb(0xAA00AA)); // Dark Purple
            case '6': return style.withColor(TextColor.fromRgb(0xFFAA00)); // Gold
            case '7': return style.withColor(TextColor.fromRgb(0xAAAAAA)); // Gray
            case '8': return style.withColor(TextColor.fromRgb(0x555555)); // Dark Gray
            case '9': return style.withColor(TextColor.fromRgb(0x5555FF)); // Blue
            case 'a': return style.withColor(TextColor.fromRgb(0x55FF55)); // Green
            case 'b': return style.withColor(TextColor.fromRgb(0x55FFFF)); // Aqua
            case 'c': return style.withColor(TextColor.fromRgb(0xFF5555)); // Red
            case 'd': return style.withColor(TextColor.fromRgb(0xFF55FF)); // Light Purple
            case 'e': return style.withColor(TextColor.fromRgb(0xFFFF55)); // Yellow
            case 'f': return style.withColor(TextColor.fromRgb(0xFFFFFF)); // White
            case 'k': return style.withObfuscated(true);
            case 'l': return style.withBold(true);
            case 'm': return style.withStrikethrough(true);
            case 'n': return style.withUnderlined(true);
            case 'o': return style.withItalic(true);
            case 'r': return Style.EMPTY; // Reset
            default: return style;
        }
    }

    private static void broadcastMessage() {
        if (server != null) {
            List<? extends String> messages = ModConfigHandler.CONFIG.messages.get();
            String prefix = ModConfigHandler.CONFIG.prefix.get();
            String header = ModConfigHandler.CONFIG.header.get();
            String footer = ModConfigHandler.CONFIG.footer.get();

            String messageText = messages.get(random.nextInt(messages.size())).replace("{Prefix}", prefix);
            MutableComponent message = parseMessageWithColor(messageText);

            if (ModConfigHandler.CONFIG.headerAndFooter.get()) {
                MutableComponent headerComponent = parseMessageWithColor(header);
                MutableComponent footerComponent = parseMessageWithColor(footer);

                server.getPlayerList().getPlayers().forEach(player -> {
                    player.sendMessage(headerComponent, player.getUUID());
                    player.sendMessage(message, player.getUUID());
                    player.sendMessage(footerComponent, player.getUUID());
                });
            } else {
                server.getPlayerList().getPlayers().forEach(player -> {
                    player.sendMessage(message, player.getUUID());
                });
            }
            LOGGER.info("Broadcasted message: {}", message.getString());
        } else {
            LOGGER.warn("Server instance is null.");
        }
    }
}
