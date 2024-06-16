package avalanche7.net.forgeannouncements.utils;

import avalanche7.net.forgeannouncements.configs.MentionConfigHandler;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.Util;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = "forgeannouncements")
public class Mentions {

    @SubscribeEvent
    public static void onChatMessage(ServerChatEvent event) {
        String mentionSymbol = MentionConfigHandler.MENTION_SYMBOL.get();
        String message = event.getMessage();
        ServerPlayer sender = event.getPlayer();
        Level world = sender.getLevel();
        List<ServerPlayer> players = world.getServer().getPlayerList().getPlayers();

        boolean mentionEveryone = message.contains(mentionSymbol + "everyone");

        if (mentionEveryone) {
            boolean hasPermission = PermissionsHandler.hasPermission(sender, PermissionsHandler.MENTION_EVERYONE_PERMISSION);
            boolean hasPermissionLevel = sender.hasPermissions(PermissionsHandler.MENTION_EVERYONE_PERMISSION_LEVEL);
            if (!hasPermission && !hasPermissionLevel) {
                sender.sendMessage(new TextComponent("You do not have permission to mention everyone."), Util.NIL_UUID);
                return;
            }
            System.out.println("Mention everyone detected");
            notifyEveryone(players, sender, message);
            event.setCanceled(true);
        } else {
            for (ServerPlayer player : players) {
                String mention = mentionSymbol + player.getName().getString();
                if (message.contains(mention)) {
                    boolean hasPermission = PermissionsHandler.hasPermission(sender, PermissionsHandler.MENTION_PLAYER_PERMISSION);
                    boolean hasPermissionLevel = sender.hasPermissions(PermissionsHandler.MENTION_PLAYER_PERMISSION_LEVEL);
                    if (!hasPermission && !hasPermissionLevel) {
                        sender.sendMessage(new TextComponent("You do not have permission to mention players."), Util.NIL_UUID);
                        return;
                    }
                    System.out.println("Mention player detected: " + player.getName().getString());
                    notifyPlayer(player, sender, message);
                    message = message.replaceFirst(mention, ""); // Remove the mention from the message
                    event.setComponent(new TextComponent(message)); // Set the new message
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
        player.displayClientMessage(new TextComponent(chatMessage), false);
        player.connection.send(new ClientboundSetTitleTextPacket(new TextComponent(titleMessage)));
        player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}



