package avalanche7.net.forgeannouncements;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketTitle;
import net.minecraft.network.play.server.SPacketUpdateBossInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mod.EventBusSubscriber(modid = "forgeannouncements")
public class Announcements {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final Random random = new Random();
    private static final Logger LOGGER = LogManager.getLogger();
    private static MinecraftServer server;

    @Mod.EventHandler
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
        if (ModConfigHandler.globalEnable) {
            long globalInterval = ModConfigHandler.globalInterval;
            if (ModConfigHandler.debugEnable) {
                LOGGER.info("Scheduling global messages with interval: {} seconds", globalInterval);
            }
            scheduler.scheduleAtFixedRate(Announcements::broadcastGlobalMessages, globalInterval, globalInterval, TimeUnit.SECONDS);
        } else {
            if (ModConfigHandler.debugEnable) {
                LOGGER.info("Global messages are disabled.");
            }
        }

        if (ModConfigHandler.actionbarEnable) {
            long actionbarInterval = ModConfigHandler.actionbarInterval;
            if (ModConfigHandler.debugEnable) {
                LOGGER.info("Scheduling actionbar messages with interval: {} seconds", actionbarInterval);
            }
            scheduler.scheduleAtFixedRate(Announcements::broadcastActionbarMessages, actionbarInterval, actionbarInterval, TimeUnit.SECONDS);
        } else {
            if (ModConfigHandler.debugEnable) {
                LOGGER.info("Actionbar messages are disabled.");
            }
        }

        if (ModConfigHandler.titleEnable) {
            long titleInterval = ModConfigHandler.titleInterval;
            if (ModConfigHandler.debugEnable) {
                LOGGER.info("Scheduling title messages with interval: {} seconds", titleInterval);
            }
            scheduler.scheduleAtFixedRate(Announcements::broadcastTitleMessages, titleInterval, titleInterval, TimeUnit.SECONDS);
        } else {
            if (ModConfigHandler.debugEnable) {
                LOGGER.info("Title messages are disabled.");
            }
        }

        if (ModConfigHandler.bossbarEnable) {
            long bossbarInterval = ModConfigHandler.bossbarInterval;
            if (ModConfigHandler.debugEnable) {
                LOGGER.info("Scheduling bossbar messages with interval: {} seconds", bossbarInterval);
            }
            scheduler.scheduleAtFixedRate(Announcements::broadcastBossbarMessages, bossbarInterval, bossbarInterval, TimeUnit.SECONDS);
        } else {
            if (ModConfigHandler.debugEnable) {
                LOGGER.info("Bossbar messages are disabled.");
            }
        }
    }

    private static void broadcastGlobalMessages() {
        if (server != null) {
            List<String> messages = ModConfigHandler.globalMessages;
            String prefix = ModConfigHandler.prefix + "§r";
            String header = ModConfigHandler.header;
            String footer = ModConfigHandler.footer;

            String messageText = messages.get(random.nextInt(messages.size())).replace("{Prefix}", prefix);
            ITextComponent message = createClickableMessage(messageText);

            PlayerList playerList = server.getPlayerList();
            if (ModConfigHandler.headerAndFooter) {
                for (EntityPlayerMP player : playerList.getPlayers()) {
                    player.sendMessage(parseMessageWithColor(header));
                    player.sendMessage(message);
                    player.sendMessage(parseMessageWithColor(footer));
                }
            } else {
                for (EntityPlayerMP player : playerList.getPlayers()) {
                    player.sendMessage(message);
                }
            }
            if (ModConfigHandler.debugEnable) {
                LOGGER.info("Broadcasted global message: {}", message.getUnformattedText());
            }
        } else {
            LOGGER.warn("Server instance is null.");
        }
    }

    private static void broadcastActionbarMessages() {
        if (server != null) {
            List<String> messages = ModConfigHandler.actionbarMessages;
            String prefix = ModConfigHandler.prefix + "§r";

            String messageText = messages.get(random.nextInt(messages.size())).replace("{Prefix}", prefix);
            ITextComponent message = parseMessageWithColor(messageText);

            PlayerList playerList = server.getPlayerList();
            for (EntityPlayerMP player : playerList.getPlayers()) {
                player.connection.sendPacket(new SPacketTitle(SPacketTitle.Type.ACTIONBAR, message));
            }
            if (ModConfigHandler.debugEnable) {
                LOGGER.info("Broadcasted actionbar message: {}", message.getUnformattedText());
            }
        } else {
            LOGGER.warn("Server instance is null.");
        }
    }

    private static void broadcastTitleMessages() {
        if (server != null) {
            List<String> messages = ModConfigHandler.titleMessages;
            String prefix = ModConfigHandler.prefix + "§r";

            String messageText = messages.get(random.nextInt(messages.size())).replace("{Prefix}", prefix);
            ITextComponent message = parseMessageWithColor(messageText);

            PlayerList playerList = server.getPlayerList();
            for (EntityPlayerMP player : playerList.getPlayers()) {
                player.connection.sendPacket(new SPacketTitle(SPacketTitle.Type.RESET, new TextComponentString("")));
                player.connection.sendPacket(new SPacketTitle(SPacketTitle.Type.TITLE, message));
            }
            if (ModConfigHandler.debugEnable) {
                LOGGER.info("Broadcasted title message: {}", message.getUnformattedText());
            }
        } else {
            LOGGER.warn("Server instance is null.");
        }
    }

    private static void broadcastBossbarMessages() {
        if (server != null) {
            List<String> messages = ModConfigHandler.bossbarMessages;
            String prefix = ModConfigHandler.prefix + "§r";
            int bossbarTime = ModConfigHandler.bossbarTime;
            String bossbarColor = ModConfigHandler.bossbarColor;

            String messageText = messages.get(random.nextInt(messages.size())).replace("{Prefix}", prefix);
            ITextComponent message = parseMessageWithColor(messageText);

            BossInfoServer bossEvent = new BossInfoServer(message, BossInfo.Color.valueOf(bossbarColor.toUpperCase()), BossInfo.Overlay.PROGRESS);

            SPacketUpdateBossInfo addPacket = new SPacketUpdateBossInfo(SPacketUpdateBossInfo.Operation.ADD, bossEvent);
            PlayerList playerList = server.getPlayerList();
            for (EntityPlayerMP player : playerList.getPlayers()) {
                player.connection.sendPacket(addPacket);
            }
            if (ModConfigHandler.debugEnable) {
                LOGGER.info("Broadcasted bossbar message: {}", message.getUnformattedText());
            }

            scheduler.schedule(() -> {
                if (server != null) {
                    SPacketUpdateBossInfo removePacket = new SPacketUpdateBossInfo(SPacketUpdateBossInfo.Operation.REMOVE, bossEvent);
                    for (EntityPlayerMP player : playerList.getPlayers()) {
                        player.connection.sendPacket(removePacket);
                    }
                    if (ModConfigHandler.debugEnable) {
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
        ITextComponent message = new TextComponentString("");
        String[] parts = rawMessage.split("§");

        Style style = new Style();
        if (!rawMessage.startsWith("§")) {
            message.appendSibling(new TextComponentString(parts[0]).setStyle(style));
        }

        for (int i = rawMessage.startsWith("§") ? 0 : 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }

            char colorCode = parts[i].charAt(0);
            String text = parts[i].substring(1);

            style = applyColorCode(style, colorCode);
            TextComponentString component = (TextComponentString) new TextComponentString(text).setStyle(style);
            message.appendSibling(component);
        }

        return message;
    }

    private static ITextComponent createClickableMessage(String rawMessage) {
        ITextComponent message = new TextComponentString("");
        String[] parts = rawMessage.split(" ");
        for (int i = 0; i < parts.length; i++) {
            TextComponentString part = new TextComponentString(parts[i]);
            if (parts[i].startsWith("http://") || parts[i].startsWith("https://")) {
                part.setStyle(new Style().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, parts[i])));
            }
            message.appendSibling(part);
            if (i < parts.length - 1) {
                message.appendSibling(new TextComponentString(" "));
            }
        }
        return message;
    }

    private static Style applyColorCode(Style style, char colorCode) {
        TextFormatting[] values = TextFormatting.values();
        if (Character.isDigit(colorCode)) {
            int index = Character.getNumericValue(colorCode);
            if (index >= 0 && index < values.length) {
                style.setColor(values[index]);
            }
        } else {
            switch (colorCode) {
                case 'k':
                    style.setObfuscated(true);
                    break;
                case 'l':
                    style.setBold(true);
                    break;
                case 'm':
                    style.setStrikethrough(true);
                    break;
                case 'n':
                    style.setUnderlined(true);
                    break;
                case 'o':
                    style.setItalic(true);
                    break;
                case 'r':
                    style = new Style(); // Reset
                    break;
                default:
                    break;
            }
        }
        return style;
    }
}
