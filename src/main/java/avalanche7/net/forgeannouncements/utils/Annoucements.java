package avalanche7.net.forgeannouncements.utils;

import avalanche7.net.forgeannouncements.configs.AnnouncementsConfigHandler;
import avalanche7.net.forgeannouncements.configs.MainConfigHandler;
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

    public static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final Random random = new Random();
    private static MinecraftServer server;

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        if (!MainConfigHandler.CONFIG.announcementsEnable.get()) {
            DebugLogger.debugLog("Announcements feature is disabled.");
            return;
        }
        server = event.getServer();
        DebugLogger.debugLog("Server is starting, scheduling announcements.");
        scheduleAnnouncements();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!scheduler.isShutdown()) {
                DebugLogger.debugLog("Server is stopping, shutting down scheduler...");
                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException ex) {
                    scheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                DebugLogger.debugLog("Scheduler has been shut down.");
            }
        }));
    }

    public static void scheduleAnnouncements() {
        if (AnnouncementsConfigHandler.CONFIG.globalEnable.get()) {
            long globalInterval = AnnouncementsConfigHandler.CONFIG.globalInterval.get();
            DebugLogger.debugLog("Scheduling global messages with interval: {} seconds", globalInterval);
            scheduler.scheduleAtFixedRate(Annoucements::broadcastGlobalMessages, globalInterval, globalInterval, TimeUnit.SECONDS);
        } else {
            DebugLogger.debugLog("Global messages are disabled.");
        }

        if (AnnouncementsConfigHandler.CONFIG.actionbarEnable.get()) {
            long actionbarInterval = AnnouncementsConfigHandler.CONFIG.actionbarInterval.get();
            DebugLogger.debugLog("Scheduling actionbar messages with interval: {} seconds", actionbarInterval);
            scheduler.scheduleAtFixedRate(Annoucements::broadcastActionbarMessages, actionbarInterval, actionbarInterval, TimeUnit.SECONDS);
        } else {
            DebugLogger.debugLog("Actionbar messages are disabled.");
        }

        if (AnnouncementsConfigHandler.CONFIG.titleEnable.get()) {
            long titleInterval = AnnouncementsConfigHandler.CONFIG.titleInterval.get();
            DebugLogger.debugLog("Scheduling title messages with interval: {} seconds", titleInterval);
            scheduler.scheduleAtFixedRate(Annoucements::broadcastTitleMessages, titleInterval, titleInterval, TimeUnit.SECONDS);
        } else {
            DebugLogger.debugLog("Title messages are disabled.");
        }

        if (AnnouncementsConfigHandler.CONFIG.bossbarEnable.get()) {
            long bossbarInterval = AnnouncementsConfigHandler.CONFIG.bossbarInterval.get();
            DebugLogger.debugLog("Scheduling bossbar messages with interval: {} seconds", bossbarInterval);
            scheduler.scheduleAtFixedRate(Annoucements::broadcastBossbarMessages, bossbarInterval, bossbarInterval, TimeUnit.SECONDS);
        } else {
            DebugLogger.debugLog("Bossbar messages are disabled.");
        }
    }

    private static int globalMessageIndex = 0;
    private static int actionbarMessageIndex = 0;
    private static int titleMessageIndex = 0;
    private static int bossbarMessageIndex = 0;
    private static void broadcastGlobalMessages() {
        if (server != null) {
            List<? extends String> messages = AnnouncementsConfigHandler.CONFIG.globalMessages.get();
            String prefix = AnnouncementsConfigHandler.CONFIG.prefix.get() + "§r";
            String header = AnnouncementsConfigHandler.CONFIG.header.get();
            String footer = AnnouncementsConfigHandler.CONFIG.footer.get();

            String messageText;
            if (AnnouncementsConfigHandler.CONFIG.orderMode.get().equals("SEQUENTIAL")) {
                messageText = messages.get(globalMessageIndex).replace("{Prefix}", prefix);
                globalMessageIndex = (globalMessageIndex + 1) % messages.size();
            } else {
                messageText = messages.get(random.nextInt(messages.size())).replace("{Prefix}", prefix);
            }

            MutableComponent message = createClickableMessage(messageText);

            if (AnnouncementsConfigHandler.CONFIG.headerAndFooter.get()) {
                server.getPlayerList().getPlayers().forEach(player -> {
                    player.sendSystemMessage(parseMessageWithColor(header));
                    player.sendSystemMessage(message);
                    player.sendSystemMessage(parseMessageWithColor(footer));
                });
            } else {
                server.getPlayerList().getPlayers().forEach(player -> {
                    player.sendSystemMessage(message);
                });
            }
             DebugLogger.debugLog("Broadcasted global message: {}", message.getString());
        } else {
            DebugLogger.debugLog("Server instance is null.");
        }
    }

    private static void broadcastActionbarMessages() {
        if (server != null) {
            List<? extends String> messages = AnnouncementsConfigHandler.CONFIG.actionbarMessages.get();
            String prefix = AnnouncementsConfigHandler.CONFIG.prefix.get() + "§r";

            String messageText;
            if (AnnouncementsConfigHandler.CONFIG.orderMode.get().equals("SEQUENTIAL")) {
                messageText = messages.get(actionbarMessageIndex).replace("{Prefix}", prefix);
                actionbarMessageIndex = (actionbarMessageIndex + 1) % messages.size();
            } else {
                messageText = messages.get(random.nextInt(messages.size())).replace("{Prefix}", prefix);
            }
            MutableComponent message = parseMessageWithColor(messageText);

            server.getPlayerList().getPlayers().forEach(player -> {
                player.connection.send(new ClientboundSetActionBarTextPacket(message));
            });
            DebugLogger.debugLog("Broadcasted actionbar message: {}", message.getString());
        } else {
            DebugLogger.debugLog("Server instance is null.");
        }
    }

    private static void broadcastTitleMessages() {
        if (server != null) {
            List<? extends String> messages = AnnouncementsConfigHandler.CONFIG.titleMessages.get();
            String prefix = AnnouncementsConfigHandler.CONFIG.prefix.get() + "§r";

            String messageText;
            if (AnnouncementsConfigHandler.CONFIG.orderMode.get().equals("SEQUENTIAL")) {
                messageText = messages.get(titleMessageIndex).replace("{Prefix}", prefix);
                titleMessageIndex = (titleMessageIndex + 1) % messages.size();
            } else {
                messageText = messages.get(random.nextInt(messages.size())).replace("{Prefix}", prefix);
            }
            Component message = parseMessageWithColor(messageText);

            server.getPlayerList().getPlayers().forEach(player -> {
                player.connection.send(new ClientboundClearTitlesPacket(false));
                player.connection.send(new ClientboundSetTitleTextPacket(message));
            });
             DebugLogger.debugLog("Broadcasted title message: {}", message.getString());
        } else {
            DebugLogger.debugLog("Server instance is null.");
        }
    }

    private static void broadcastBossbarMessages() {
        if (server != null) {
            List<? extends String> messages = AnnouncementsConfigHandler.CONFIG.bossbarMessages.get();
            String prefix = AnnouncementsConfigHandler.CONFIG.prefix.get() + "§r";
            int bossbarTime = AnnouncementsConfigHandler.CONFIG.bossbarTime.get();
            String bossbarColor = AnnouncementsConfigHandler.CONFIG.bossbarColor.get();

            String messageText;
            if (AnnouncementsConfigHandler.CONFIG.orderMode.get().equals("SEQUENTIAL")) {
                messageText = messages.get(bossbarMessageIndex).replace("{Prefix}", prefix);
                bossbarMessageIndex = (bossbarMessageIndex + 1) % messages.size();
            } else {
                messageText = messages.get(random.nextInt(messages.size())).replace("{Prefix}", prefix);
            }
            MutableComponent message = parseMessageWithColor(messageText);

            ServerBossEvent bossEvent = new ServerBossEvent(message, BossEvent.BossBarColor.valueOf(bossbarColor.toUpperCase()), BossEvent.BossBarOverlay.PROGRESS);

            ClientboundBossEventPacket addPacket = ClientboundBossEventPacket.createAddPacket(bossEvent);
            server.getPlayerList().getPlayers().forEach(player -> {
                player.connection.send(addPacket);
            });
            DebugLogger.debugLog("Broadcasted bossbar message: {}", message.getString());

            scheduler.schedule(() -> {
                if (server != null) {
                    ClientboundBossEventPacket removePacket = ClientboundBossEventPacket.createRemovePacket(bossEvent.getId());
                    server.getPlayerList().getPlayers().forEach(player -> {
                        player.connection.send(removePacket);
                    });
                    DebugLogger.debugLog("Removed bossbar message after {} seconds", bossbarTime);
                } else {
                    DebugLogger.debugLog("Server instance is null.");
                }
            }, bossbarTime, TimeUnit.SECONDS);
        } else {
            DebugLogger.debugLog("Server instance is null.");
        }
    }

    public static MutableComponent parseMessageWithColor(String rawMessage) {
        rawMessage = rawMessage.replace("&", "§");

        MutableComponent message = Component.literal("");
        String[] parts = rawMessage.split("§");

        Style style = Style.EMPTY;
        if (!rawMessage.startsWith("§")) {
            message.append(Component.literal(parts[0]).setStyle(style));
        }

        for (int i = rawMessage.startsWith("§") ? 0 : 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }

            char colorCode = parts[i].charAt(0);
            String text = parts[i].substring(1);

            style = applyColorCode(style, colorCode);
            MutableComponent component = Component.literal(text).setStyle(style);
            message.append(component);
        }

        return message;
    }

    private static MutableComponent createClickableMessage(String rawMessage) {
        MutableComponent message = Component.literal("");
        String[] parts = rawMessage.split(" ");
        for (int i = 0; i < parts.length; i++) {
            MutableComponent part = Component.literal(parts[i]);
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
