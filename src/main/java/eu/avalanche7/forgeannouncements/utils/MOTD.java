package eu.avalanche7.forgeannouncements.utils;

import eu.avalanche7.forgeannouncements.configs.MOTDConfigHandler;
import eu.avalanche7.forgeannouncements.configs.MainConfigHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;



@Mod.EventBusSubscriber(modid = "forgeannouncements")
public class MOTD {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!MainConfigHandler.CONFIG.motdEnable.get()) {
                DebugLogger.debugLog("MOTD feature is disabled.");
            return;
        }

        ServerPlayer player = (ServerPlayer) event.getEntity();
        Component motdMessage = createMOTDMessage(player);
        player.sendSystemMessage(motdMessage);
    }

    private static Component createMOTDMessage(ServerPlayer player) {
        String[] lines = MOTDConfigHandler.CONFIG.motdMessage.get().split("\n");
        MutableComponent motdMessage = Component.literal("");
        for (String line : lines) {
            motdMessage.append(parseColoredText(line, player)).append("\n");
        }

        return motdMessage;
    }

    private static Component parseColoredText(String text, ServerPlayer player) {
        MutableComponent component = Component.literal("");
        String[] parts = text.split("§");

        if (parts.length > 0) {
            component.append(Component.literal(parts[0]));
        }

        for (int i = 1; i < parts.length; i++) {
            if (parts[i].length() > 0) {
                char colorCode = parts[i].charAt(0);
                String textPart = parts[i].substring(1);
                Style style = Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.getByCode(colorCode)));

                if (textPart.contains("[link=")) {
                    String[] linkParts = textPart.split("\\[link=", 2);
                    if (linkParts.length == 2) {
                        String remainingText = linkParts[1];
                        int endIndex = remainingText.indexOf("]");
                        if (endIndex != -1) {
                            String url = remainingText.substring(0, endIndex);
                            remainingText = remainingText.substring(endIndex + 1).trim();

                            component.append(Component.literal(url)
                                    .setStyle(style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, formatUrl(url)))));
                            textPart = remainingText;
                        }
                    }
                } else if (textPart.contains("[command=")) {
                    String[] commandParts = textPart.split("\\[command=", 2);
                    if (commandParts.length == 2) {
                        String remainingText = commandParts[1];
                        int endIndex = remainingText.indexOf("]");
                        if (endIndex != -1) {
                            String command = remainingText.substring(0, endIndex);
                            remainingText = remainingText.substring(endIndex + 1).trim();
                            if (command.startsWith("/")) {
                                command = command.substring(1);
                            }
                            String initialText = commandParts[0].isEmpty() ? "/" : commandParts[0];

                            component.append(Component.literal(initialText + command)
                                    .setStyle(style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + command))));
                            if (!remainingText.isEmpty()) {
                                component.append(" ");
                            }
                            textPart = remainingText;
                        }
                    }
                } else if (textPart.contains("[hover=")) {
                    String[] hoverParts = textPart.split("\\[hover=", 2);
                    if (hoverParts.length == 2) {
                        String hoverText = hoverParts[1];
                        int endIndex = hoverText.indexOf("]");
                        if (endIndex != -1) {
                            hoverText = hoverText.substring(0, endIndex);
                            String remainingText = hoverParts[1].substring(endIndex + 1);

                            component.append(Component.literal(hoverParts[0])
                                    .setStyle(style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText)))));
                            textPart = remainingText;
                        }
                    }
                } else if (textPart.contains("[divider]")) {
                    component.append(Component.literal("--------------------")
                            .setStyle(style.withColor(TextColor.fromLegacyFormat(ChatFormatting.GRAY))));
                    textPart = "";

                } else if (textPart.contains("[title=")) {
                    String[] titleParts = textPart.split("\\[title=", 2);
                    if (titleParts.length == 2) {
                        String titleText = titleParts[1];
                        int endIndex = titleText.indexOf("]");
                        if (endIndex != -1) {
                            titleText = titleText.substring(0, endIndex);
                            String remainingText = titleParts[1].substring(endIndex + 1);
                            Component titleComponent = Announcements.parseMessageWithColor("§" + colorCode + titleText);
                            ClientboundSetTitleTextPacket titlePacket = new ClientboundSetTitleTextPacket(titleComponent);
                            player.connection.send(titlePacket);
                            textPart = remainingText;
                        }
                    }
                } else if (textPart.contains("[subtitle=")) {
                    String[] subtitleParts = textPart.split("\\[subtitle=", 2);
                    if (subtitleParts.length == 2) {
                        String subtitleText = subtitleParts[1];
                        int endIndex = subtitleText.indexOf("]");
                        if (endIndex != -1) {
                            subtitleText = subtitleText.substring(0, endIndex);
                            String remainingText = subtitleParts[1].substring(endIndex + 1);
                            Component subtitleComponent = Announcements.parseMessageWithColor("§" + colorCode + subtitleText);
                            ClientboundSetSubtitleTextPacket subtitlePacket = new ClientboundSetSubtitleTextPacket(subtitleComponent);
                            player.connection.send(subtitlePacket);
                            textPart = remainingText;
                        }
                    }
                }
                component.append(Component.literal(textPart.trim()).setStyle(style));
            }
        }

        return component;
    }
    private static String formatUrl(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        return url;
    }
}