package avalanche7.net.forgeannouncements;

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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.concurrent.TimeUnit;

@Mod.EventBusSubscriber(modid = "forgeannouncements")
public class ForgeAnnouncementsCommand {

    public static int broadcastMessage(CommandContext<CommandSourceStack> context, String type) throws CommandSyntaxException {
        String message = StringArgumentType.getString(context, "message");
        CommandSourceStack source = context.getSource();

        if (!source.hasPermission(2)) {
            source.sendFailure(new TextComponent("You do not have permission to use this command."));
            return 0;
        }
        MutableComponent broadcastMessage = new TextComponent("");

        String[] words = message.split(" ");
        for (String word : words) {
            if (word.startsWith("http://") || word.startsWith("https://")) {
                Style style = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, word));
                broadcastMessage.append(" ").append(new TextComponent(word).setStyle(style));
            } else {
                MutableComponent coloredWord = Annoucements.parseMessageWithColor(word);
                broadcastMessage.append(" ").append(coloredWord);
            }
        }

        switch (type) {
            case "broadcast":
                boolean headerFooter = BoolArgumentType.getBool(context, "header_footer");
                if (headerFooter) {
                    String header = ModConfigHandler.CONFIG.header.get();
                    String footer = ModConfigHandler.CONFIG.footer.get();
                    MutableComponent headerMessage = Annoucements.parseMessageWithColor(header);
                    MutableComponent footerMessage = Annoucements.parseMessageWithColor(footer);
                    MutableComponent finalBroadcastMessage = broadcastMessage;
                    source.getServer().getPlayerList().getPlayers().forEach(player -> {
                        player.sendMessage(headerMessage, Util.NIL_UUID);
                        player.sendMessage(finalBroadcastMessage, Util.NIL_UUID);
                        player.sendMessage(footerMessage, Util.NIL_UUID);
                    });
                } else {
                    MutableComponent finalBroadcastMessage1 = broadcastMessage;
                    source.getServer().getPlayerList().getPlayers().forEach(player -> {
                        player.sendMessage(finalBroadcastMessage1, Util.NIL_UUID);
                    });
                }
                break;
            case "actionbar":
                MutableComponent finalBroadcastMessage2 = broadcastMessage;
                source.getServer().getPlayerList().getPlayers().forEach(player -> {
                    player.connection.send(new ClientboundSetActionBarTextPacket(finalBroadcastMessage2));
                });
                break;
            case "title":
                source.getServer().getPlayerList().getPlayers().forEach(player -> {
                    player.connection.send(new ClientboundClearTitlesPacket(false));
                    player.connection.send(new ClientboundSetTitleTextPacket(broadcastMessage));
                });
                break;
            case "bossbar":
                String color = StringArgumentType.getString(context, "color");
                int interval = IntegerArgumentType.getInteger(context, "interval");
                BossEvent.BossBarColor bossBarColor;
                try {
                    bossBarColor = BossEvent.BossBarColor.valueOf(color.toUpperCase());
                } catch (IllegalArgumentException e) {
                    source.sendFailure(new TextComponent("Invalid color: " + color));
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
                source.sendFailure(new TextComponent("Invalid message type: " + type));
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
                                .then(Commands.argument("message", StringArgumentType.greedyString())
                                        .executes(context -> broadcastMessage(context, "title"))))
                        .then(Commands.literal("bossbar")
                                .then(Commands.argument("color", StringArgumentType.word())
                                        .then(Commands.argument("interval", IntegerArgumentType.integer())
                                                .then(Commands.argument("message", StringArgumentType.greedyString())
                                                        .executes(context -> broadcastMessage(context, "bossbar"))))))
        );
    }
}
