package avalanche7.net.forgeannouncements;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.*;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mod.EventBusSubscriber(modid = "forgeannouncements", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Annoucements {

    static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final Random random = new Random();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static MinecraftServer server;

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        server = event.getServer();
        LOGGER.info("Server is starting, scheduling announcements.");
        scheduleAnnouncements();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!scheduler.isShutdown()) {
                LOGGER.info("Server is stopping, shutting down scheduler...");
                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException ex) {
                    scheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                LOGGER.info("Scheduler has been shut down.");
            }
        }));
    }

    public static void scheduleAnnouncements() {
        if (ModConfigHandler.CONFIG.globalEnable.get()) {
            long globalInterval = ModConfigHandler.CONFIG.globalInterval.get();
            if (ModConfigHandler.CONFIG.debugEnable.get()) {
                LOGGER.info("Scheduling global messages with interval: {} seconds", globalInterval);
            }
            scheduler.scheduleAtFixedRate(Annoucements::broadcastGlobalMessages, globalInterval, globalInterval, TimeUnit.SECONDS);
        } else {
            if (ModConfigHandler.CONFIG.debugEnable.get()) {
                LOGGER.info("Global messages are disabled.");
            }
        }

        if (ModConfigHandler.CONFIG.actionbarEnable.get()) {
            long actionbarInterval = ModConfigHandler.CONFIG.actionbarInterval.get();
            if (ModConfigHandler.CONFIG.debugEnable.get()) {
                LOGGER.info("Scheduling actionbar messages with interval: {} seconds", actionbarInterval);
            }
            scheduler.scheduleAtFixedRate(Annoucements::broadcastActionbarMessages, actionbarInterval, actionbarInterval, TimeUnit.SECONDS);
        } else {
            if (ModConfigHandler.CONFIG.debugEnable.get()) {
                LOGGER.info("Actionbar messages are disabled.");
            }
        }

        if (ModConfigHandler.CONFIG.titleEnable.get()) {
            long titleInterval = ModConfigHandler.CONFIG.titleInterval.get();
            if (ModConfigHandler.CONFIG.debugEnable.get()) {
                LOGGER.info("Scheduling title messages with interval: {} seconds", titleInterval);
            }
            scheduler.scheduleAtFixedRate(Annoucements::broadcastTitleMessages, titleInterval, titleInterval, TimeUnit.SECONDS);
        } else {
            if (ModConfigHandler.CONFIG.debugEnable.get()) {
                LOGGER.info("Title messages are disabled.");
            }
        }

        if (ModConfigHandler.CONFIG.bossbarEnable.get()) {
            long bossbarInterval = ModConfigHandler.CONFIG.bossbarInterval.get();
            if (ModConfigHandler.CONFIG.debugEnable.get()) {
                LOGGER.info("Scheduling bossbar messages with interval: {} seconds", bossbarInterval);
            }
            scheduler.scheduleAtFixedRate(Annoucements::broadcastBossbarMessages, bossbarInterval, bossbarInterval, TimeUnit.SECONDS);
        } else {
            if (ModConfigHandler.CONFIG.debugEnable.get()) {
                LOGGER.info("Bossbar messages are disabled.");
            }
        }
    }

    private static void broadcastGlobalMessages() {
        if (server != null) {
            List<? extends String> messages = ModConfigHandler.CONFIG.globalMessages.get();
            String prefix = ModConfigHandler.CONFIG.prefix.get() + "§r";
            String header = ModConfigHandler.CONFIG.header.get();
            String footer = ModConfigHandler.CONFIG.footer.get();

            String messageText = messages.get(random.nextInt(messages.size())).replace("{Prefix}", prefix);
            MutableComponent message = createClickableMessage(messageText);

            if (ModConfigHandler.CONFIG.headerAndFooter.get()) {
                server.getPlayerList().getPlayers().forEach(player -> {
                    player.sendMessage(parseMessageWithColor(header), player.getUUID());
                    player.sendMessage(message, player.getUUID());
                    player.sendMessage(parseMessageWithColor(footer), player.getUUID());
                });
            } else {
                server.getPlayerList().getPlayers().forEach(player -> {
                    player.sendMessage(message, player.getUUID());
                });
            }
            if (ModConfigHandler.CONFIG.debugEnable.get()) {
                LOGGER.info("Broadcasted global message: {}", message.getString());
            }
        } else {
            LOGGER.warn("Server instance is null.");
        }
    }

    private static void broadcastActionbarMessages() {
        if (server != null) {
            List<? extends String> messages = ModConfigHandler.CONFIG.actionbarMessages.get();
            String prefix = ModConfigHandler.CONFIG.prefix.get() + "§r";

            String messageText = messages.get(random.nextInt(messages.size())).replace("{Prefix}", prefix);
            MutableComponent message = parseMessageWithColor(messageText);

            server.getPlayerList().getPlayers().forEach(player -> {
                player.connection.send(new ClientboundSetActionBarTextPacket(message));
            });
            if (ModConfigHandler.CONFIG.debugEnable.get()) {
                LOGGER.info("Broadcasted actionbar message: {}", message.getString());
            }
        } else {
            LOGGER.warn("Server instance is null.");
        }
    }

    private static void broadcastTitleMessages() {
        if (server != null) {
            List<? extends String> messages = ModConfigHandler.CONFIG.titleMessages.get();
            String prefix = ModConfigHandler.CONFIG.prefix.get() + "§r";

            String messageText = messages.get(random.nextInt(messages.size())).replace("{Prefix}", prefix);
            Component message = parseMessageWithColor(messageText);

            server.getPlayerList().getPlayers().forEach(player -> {
                player.connection.send(new ClientboundClearTitlesPacket(false));
                player.connection.send(new ClientboundSetTitleTextPacket(message));
            });
            if (ModConfigHandler.CONFIG.debugEnable.get()) {
                LOGGER.info("Broadcasted title message: {}", message.getString());
            }
        } else {
            LOGGER.warn("Server instance is null.");
        }
    }

    private static void broadcastBossbarMessages() {
        if (server != null) {
            List<? extends String> messages = ModConfigHandler.CONFIG.bossbarMessages.get();
            String prefix = ModConfigHandler.CONFIG.prefix.get() + "§r";
            int bossbarTime = ModConfigHandler.CONFIG.bossbarTime.get();
            String bossbarColor = ModConfigHandler.CONFIG.bossbarColor.get();

            String messageText = messages.get(random.nextInt(messages.size())).replace("{Prefix}", prefix);
            MutableComponent message = parseMessageWithColor(messageText);

            ServerBossEvent bossEvent = new ServerBossEvent(message, BossEvent.BossBarColor.valueOf(bossbarColor.toUpperCase()), BossEvent.BossBarOverlay.PROGRESS);

            ClientboundBossEventPacket addPacket = ClientboundBossEventPacket.createAddPacket(bossEvent);
            server.getPlayerList().getPlayers().forEach(player -> {
                player.connection.send(addPacket);
            });
            if (ModConfigHandler.CONFIG.debugEnable.get()) {
                LOGGER.info("Broadcasted bossbar message: {}", message.getString());
            }

            scheduler.schedule(() -> {
                if (server != null) {
                    ClientboundBossEventPacket removePacket = ClientboundBossEventPacket.createRemovePacket(bossEvent.getId());
                    server.getPlayerList().getPlayers().forEach(player -> {
                        player.connection.send(removePacket);
                    });
                    if (ModConfigHandler.CONFIG.debugEnable.get()) {
                        LOGGER.info("Removed bossbar message after {} seconds", bossbarTime);
                    }
                } else {
                    LOGGER.warn("Server instance is null.");
                }
            }, bossbarTime, TimeUnit.SECONDS);
        } else {
            LOGGER.warn("Server instance is null.");
        }
    }

    static MutableComponent parseMessageWithColor(String rawMessage) {
        rawMessage = rawMessage.replace("&", "§");

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

    private static MutableComponent createClickableMessage(String rawMessage) {
        MutableComponent message = new TextComponent("");
        String[] parts = rawMessage.split(" ");
        for (int i = 0; i < parts.length; i++) {
            MutableComponent part = new TextComponent(parts[i]);
            if (parts[i].startsWith("http://") || parts[i].startsWith("https://")) {
                part.setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, parts[i])));
            }
            message.append(part);
            if (i < parts.length - 1) {
                message.append(" ");
            }
        }
        return message;
    }
    private static Style applyColorCode(Style style, char colorCode) {
        switch (colorCode) {
            case '0':
                style = style.withColor(TextColor.fromRgb(0x000000)); // Black
                break;
            case '1':
                style = style.withColor(TextColor.fromRgb(0x0000AA)); // Dark Blue
                break;
            case '2':
                style = style.withColor(TextColor.fromRgb(0x00AA00)); // Dark Green
                break;
            case '3':
                style = style.withColor(TextColor.fromRgb(0x00AAAA)); // Dark Aqua
                break;
            case '4':
                style = style.withColor(TextColor.fromRgb(0xAA0000)); // Dark Red
                break;
            case '5':
                style = style.withColor(TextColor.fromRgb(0xAA00AA)); // Dark Purple
                break;
            case '6':
                style = style.withColor(TextColor.fromRgb(0xFFAA00)); // Gold
                break;
            case '7':
                style = style.withColor(TextColor.fromRgb(0xAAAAAA)); // Gray
                break;
            case '8':
                style = style.withColor(TextColor.fromRgb(0x555555)); // Dark Gray
                break;
            case '9':
                style = style.withColor(TextColor.fromRgb(0x5555FF)); // Blue
                break;
            case 'a':
                style = style.withColor(TextColor.fromRgb(0x55FF55)); // Green
                break;
            case 'b':
                style = style.withColor(TextColor.fromRgb(0x55FFFF)); // Aqua
                break;
            case 'c':
                style = style.withColor(TextColor.fromRgb(0xFF5555)); // Red
                break;
            case 'd':
                style = style.withColor(TextColor.fromRgb(0xFF55FF)); // Light Purple
                break;
            case 'e':
                style = style.withColor(TextColor.fromRgb(0xFFFF55)); // Yellow
                break;
            case 'f':
                style = style.withColor(TextColor.fromRgb(0xFFFFFF)); // White
                break;
            case 'k':
                style = style.withObfuscated(true);
                break;
            case 'l':
                style = style.withBold(true);
                break;
            case 'm':
                style = style.withStrikethrough(true);
                break;
            case 'n':
                style = style.withUnderlined(true);
                break;
            case 'o':
                style = style.withItalic(true);
                break;
            case 'r':
                style = Style.EMPTY; // Reset
                break;
            default:
                break;
        }
        return style;
    }
}
