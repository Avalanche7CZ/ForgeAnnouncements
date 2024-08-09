package eu.avalanche7.forgeannouncements.utils;

import eu.avalanche7.forgeannouncements.configs.MainConfigHandler;
import eu.avalanche7.forgeannouncements.configs.MentionConfigHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = "forgeannouncements")
public class Mentions {
    @SubscribeEvent
    public static void onChatMessage(ServerChatEvent event) {

        if (!MainConfigHandler.CONFIG.mentionsEnable.get()) {
            DebugLogger.debugLog("Mention feature is disabled.");
            return;
        }

        String mentionSymbol = MentionConfigHandler.MENTION_SYMBOL.get();
        Component messageComponent = event.getMessage();
        String message = messageComponent.getString();
        ServerPlayer sender = event.getPlayer();
        Level world = sender.getLevel();
        List<ServerPlayer> players = world.getServer().getPlayerList().getPlayers();

        boolean mentionEveryone = message.contains(mentionSymbol + "everyone");

        if (mentionEveryone) {
            boolean hasPermission = PermissionsHandler.hasPermission(sender, PermissionsHandler.MENTION_EVERYONE_PERMISSION);
            boolean hasPermissionLevel = sender.hasPermissions(PermissionsHandler.MENTION_EVERYONE_PERMISSION_LEVEL);
            if (!hasPermission && !hasPermissionLevel) {
                sender.sendSystemMessage(Component.literal("You do not have permission to mention everyone."));
                return;
            }
             DebugLogger.debugLog("Mention everyone detected");
            notifyEveryone(players, sender, message);
            event.setCanceled(true);
        } else {
            for (ServerPlayer player : players) {
                String mention = mentionSymbol + player.getName().getString();
                if (message.contains(mention)) {
                    boolean hasPermission = PermissionsHandler.hasPermission(sender, PermissionsHandler.MENTION_PLAYER_PERMISSION);
                    boolean hasPermissionLevel = sender.hasPermissions(PermissionsHandler.MENTION_PLAYER_PERMISSION_LEVEL);
                    if (!hasPermission && !hasPermissionLevel) {
                        sender.sendSystemMessage(Component.literal("You do not have permission to mention players."));
                        return;
                    }
                    DebugLogger.debugLog("Mention player detected: " + player.getName().getString());
                    notifyPlayer(player, sender, message);
                    message = message.replaceFirst(mention, "");
                    event.setMessage(Component.literal(message));
                }
            }
        }
    }

    private static void notifyEveryone(List<ServerPlayer> players, ServerPlayer sender, String message) {
        String chatMessage = String.format(MentionConfigHandler.EVERYONE_MENTION_MESSAGE.get(), sender.getName().getString());
        String titleMessage = String.format(MentionConfigHandler.EVERYONE_TITLE_MESSAGE.get(), sender.getName().getString());

        for (ServerPlayer player : players) {
            sendMentionNotification(player, chatMessage, titleMessage);
        }
    }

    private static void notifyPlayer(ServerPlayer player, ServerPlayer sender, String message) {
        String chatMessage = String.format(MentionConfigHandler.INDIVIDUAL_MENTION_MESSAGE.get(), sender.getName().getString());
        String titleMessage = String.format(MentionConfigHandler.INDIVIDUAL_TITLE_MESSAGE.get(), sender.getName().getString());

        sendMentionNotification(player, chatMessage, titleMessage);
    }

    private static void sendMentionNotification(ServerPlayer player, String chatMessage, String titleMessage) {
        player.displayClientMessage(Component.literal(chatMessage), false);
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(titleMessage)));
        player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}