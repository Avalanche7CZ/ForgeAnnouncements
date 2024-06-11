package avalanche7.net.forgeannouncements;

import net.minecraft.network.play.server.SUpdateBossInfoPacket;
import net.minecraft.network.play.server.STitlePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.world.BossInfo;
import net.minecraft.world.server.ServerBossInfo;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mod.EventBusSubscriber(modid = "forgeannouncements", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Annoucements {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final Random random = new Random();
    private static final Logger LOGGER = LogManager.getLogger();
    private static MinecraftServer server;

    @SubscribeEvent
    public static void onServerStarting(FMLServerStartingEvent event) {
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
            ITextComponent message = createClickableMessage(messageText);

            PlayerList playerList = server.getPlayerList();
            if (ModConfigHandler.CONFIG.headerAndFooter.get()) {
                playerList.getPlayers().forEach(player -> {
                    player.sendMessage(parseMessageWithColor(header), player.getUUID());
                    player.sendMessage(message, player.getUUID());
                    player.sendMessage(parseMessageWithColor(footer), player.getUUID());
                });
            } else {
                playerList.getPlayers().forEach(player -> {
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
            ITextComponent message = parseMessageWithColor(messageText);

            PlayerList playerList = server.getPlayerList();
            playerList.getPlayers().forEach(player -> {
                player.connection.send(new STitlePacket(STitlePacket.Type.ACTIONBAR, message));
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
            ITextComponent message = parseMessageWithColor(messageText);

            PlayerList playerList = server.getPlayerList();
            playerList.getPlayers().forEach(player -> {
                player.connection.send(new STitlePacket(STitlePacket.Type.RESET, StringTextComponent.EMPTY));
                player.connection.send(new STitlePacket(STitlePacket.Type.TITLE, message));
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
            ITextComponent message = parseMessageWithColor(messageText);

            ServerBossInfo bossEvent = new ServerBossInfo(message, BossInfo.Color.valueOf(bossbarColor.toUpperCase()), BossInfo.Overlay.PROGRESS);

            SUpdateBossInfoPacket addPacket = new SUpdateBossInfoPacket(SUpdateBossInfoPacket.Operation.ADD, bossEvent);
            PlayerList playerList = server.getPlayerList();
            playerList.getPlayers().forEach(player -> {
                player.connection.send(addPacket);
            });
            if (ModConfigHandler.CONFIG.debugEnable.get()) {
                LOGGER.info("Broadcasted bossbar message: {}", message.getString());
            }

            scheduler.schedule(() -> {
                if (server != null) {
                    SUpdateBossInfoPacket removePacket = new SUpdateBossInfoPacket(SUpdateBossInfoPacket.Operation.REMOVE, bossEvent);
                    playerList.getPlayers().forEach(player -> {
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

    private static ITextComponent parseMessageWithColor(String rawMessage) {
        ITextComponent message = new StringTextComponent("");
        String[] parts = rawMessage.split("§");

        Style style = Style.EMPTY;
        if (!rawMessage.startsWith("§")) {
            message.getSiblings().add(new StringTextComponent(parts[0]).setStyle(style));
        }

        for (int i = rawMessage.startsWith("§") ? 0 : 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }

            char colorCode = parts[i].charAt(0);
            String text = parts[i].substring(1);

            style = applyColorCode(style, colorCode);
            StringTextComponent component = (StringTextComponent) new StringTextComponent(text).setStyle(style);
            message.getSiblings().add(component);
        }

        return message;
    }

    private static ITextComponent createClickableMessage(String rawMessage) {
        ITextComponent message = new StringTextComponent("");
        String[] parts = rawMessage.split(" ");
        for (int i = 0; i < parts.length; i++) {
            StringTextComponent part = new StringTextComponent(parts[i]);
            if (parts[i].startsWith("http://") || parts[i].startsWith("https://")) {
                part.setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, parts[i])));
            }
            message.getSiblings().add(part);
            if (i < parts.length - 1) {
                message.getSiblings().add(new StringTextComponent(" "));
            }
        }
        return message;
    }

    private static Style applyColorCode(Style style, char colorCode) {
        TextFormatting[] values = TextFormatting.values();
        if (Character.isDigit(colorCode)) {
            int index = Character.getNumericValue(colorCode);
            if (index >= 0 && index < values.length) {
                style = style.withColor(values[index]);
            }
        } else {
            switch (colorCode) {
                case 'k':
                    style = style.setObfuscated(true);
                    break;
                case 'l':
                    style = style.withBold(true);
                    break;
                case 'm':
                    style = style.setStrikethrough(true);
                    break;
                case 'n':
                    style = style.setUnderlined(true);
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
        }
        return style;
    }
}



