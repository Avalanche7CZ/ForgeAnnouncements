package avalanche7.net.forgeannouncements.commands;

import avalanche7.net.forgeannouncements.configs.AnnouncementsConfigHandler;
import avalanche7.net.forgeannouncements.utils.Annoucements;
import avalanche7.net.forgeannouncements.utils.PermissionsHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.concurrent.TimeUnit;

@Mod.EventBusSubscriber(modid = "forgeannouncements")
public class AnnouncementsCommand {


    public static int broadcastTitle(CommandContext<CommandSourceStack> context, String title, String subtitle) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        if (!source.hasPermission(2)) {
            source.sendFailure(Component.literal("You do not have permission to use this command."));
            return 0;
        }

        MutableComponent titleComponent = Annoucements.parseMessageWithColor(title);
        MutableComponent subtitleComponent = subtitle != null ? Annoucements.parseMessageWithColor(subtitle) : null;

        source.getServer().getPlayerList().getPlayers().forEach(player -> {
            player.connection.send(new ClientboundClearTitlesPacket(false));
            player.connection.send(new ClientboundSetTitleTextPacket(titleComponent));
            if (subtitleComponent != null) {
                player.connection.send(new ClientboundSetSubtitleTextPacket(subtitleComponent));
            }
        });

        return 1;
    }

    public static int broadcastMessage(CommandContext<CommandSourceStack> context, String type) throws CommandSyntaxException {
        String message = StringArgumentType.getString(context, "message");
        CommandSourceStack source = context.getSource();

        if (!source.hasPermission(2)) {
            source.sendFailure(Component.literal("You do not have permission to use this command."));
            return 0;
        }
        MutableComponent broadcastMessage = Component.literal("");

        String[] words = message.split(" ");
        for (String word : words) {
            if (word.startsWith("http://") || word.startsWith("https://")) {
                Style style = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, word));
                broadcastMessage.append(" ").append(Component.literal(word).setStyle(style));
            } else {
                MutableComponent coloredWord = Annoucements.parseMessageWithColor(word);
                broadcastMessage.append(" ").append(coloredWord);
            }
        }
        int requiredPermissionLevel;
        switch (type) {
            case "broadcast":
                requiredPermissionLevel = PermissionsHandler.BROADCAST_PERMISSION_LEVEL;
                boolean headerFooter = BoolArgumentType.getBool(context, "header_footer");
                if (headerFooter) {
                    String header = AnnouncementsConfigHandler.CONFIG.header.get();
                    String footer = AnnouncementsConfigHandler.CONFIG.footer.get();
                    MutableComponent headerMessage = Annoucements.parseMessageWithColor(header);
                    MutableComponent footerMessage = Annoucements.parseMessageWithColor(footer);
                    MutableComponent finalBroadcastMessage = broadcastMessage;
                    source.getServer().getPlayerList().getPlayers().forEach(player -> {
                        player.sendSystemMessage(headerMessage);
                        player.sendSystemMessage(finalBroadcastMessage);
                        player.sendSystemMessage(footerMessage);
                    });
                } else {
                    MutableComponent finalBroadcastMessage1 = broadcastMessage;
                    source.getServer().getPlayerList().getPlayers().forEach(player -> {
                        player.sendSystemMessage(finalBroadcastMessage1);
                    });
                }
                break;
            case "actionbar":
                requiredPermissionLevel = PermissionsHandler.ACTIONBAR_PERMISSION_LEVEL;
                MutableComponent finalBroadcastMessage2 = broadcastMessage;
                source.getServer().getPlayerList().getPlayers().forEach(player -> {
                    player.connection.send(new ClientboundSetActionBarTextPacket(finalBroadcastMessage2));
                });
                break;
            case "title":
                requiredPermissionLevel = PermissionsHandler.TITLE_PERMISSION_LEVEL;
                int separatorIndex = java.util.Arrays.asList(words).indexOf("-");
                String title, subtitle;
                if (separatorIndex == -1 || separatorIndex == 0 || separatorIndex == words.length - 1) {
                    title = String.join(" ", java.util.Arrays.copyOfRange(words, 0, words.length));
                    subtitle = "";
                } else {
                    title = String.join(" ", java.util.Arrays.copyOfRange(words, 0, separatorIndex));
                    subtitle = String.join(" ", java.util.Arrays.copyOfRange(words, separatorIndex + 1, words.length));
                }
                MutableComponent titleComponent = Annoucements.parseMessageWithColor(title);
                MutableComponent subtitleComponent = Annoucements.parseMessageWithColor(subtitle);
                source.getServer().getPlayerList().getPlayers().forEach(player -> {
                    player.connection.send(new ClientboundClearTitlesPacket(false));
                    player.connection.send(new ClientboundSetTitleTextPacket(titleComponent));
                    player.connection.send(new ClientboundSetSubtitleTextPacket(subtitleComponent));
                });
                break;
            case "bossbar":
                requiredPermissionLevel = PermissionsHandler.BOSSBAR_PERMISSION_LEVEL;
                String color = StringArgumentType.getString(context, "color");
                int interval = IntegerArgumentType.getInteger(context, "interval");
                BossEvent.BossBarColor bossBarColor;
                try {
                    bossBarColor = BossEvent.BossBarColor.valueOf(color.toUpperCase());
                } catch (IllegalArgumentException e) {
                    source.sendFailure(Component.literal("Invalid color: " + color));
                    return 0;
                }
                ServerBossEvent bossEvent = new ServerBossEvent(broadcastMessage, bossBarColor, BossEvent.BossBarOverlay.PROGRESS);
                source.getServer().getPlayerList().getPlayers().forEach(player -> {
                    bossEvent.addPlayer(player);
                });
                Annoucements.scheduler.schedule(() -> {
                    source.getServer().getPlayerList().getPlayers().forEach(player -> {
                        bossEvent.removePlayer(player);
                    });
                }, interval, TimeUnit.SECONDS);
                break;
            default:
                source.sendFailure(Component.literal("Invalid message type: " + type));
                return 0;
        }

        if (!source.hasPermission(requiredPermissionLevel)) {
            source.sendFailure(Component.literal("You do not have permission to use this command."));
            return 0;
        }

        return 1;
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("forgeannouncements")
                        .then(Commands.literal("broadcast")
                                .then(Commands.argument("header_footer", BoolArgumentType.bool())
                                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                                .executes(context -> broadcastMessage(context, "broadcast")))))
                        .then(Commands.literal("actionbar")
                                .then(Commands.argument("message", StringArgumentType.greedyString())
                                        .executes(context -> broadcastMessage(context, "actionbar"))))
                        .then(Commands.literal("title")
                                .then(Commands.argument("titleAndSubtitle", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            String titleAndSubtitle = StringArgumentType.getString(context, "titleAndSubtitle");
                                            String[] parts = titleAndSubtitle.split(" - ", 2);
                                            String title = parts[0];
                                            String subtitle = parts.length > 1 ? parts[1] : null;
                                            return broadcastTitle(context, title, subtitle);
                                        })))
                        .then(Commands.literal("bossbar")
                                .then(Commands.argument("interval", IntegerArgumentType.integer())
                                        .then(Commands.argument("color", StringArgumentType.word())
                                                .then(Commands.argument("message", StringArgumentType.greedyString())
                                                        .executes(context -> broadcastMessage(context, "bossbar"))))))
        );
    }
}
