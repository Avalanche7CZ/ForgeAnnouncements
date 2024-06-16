package avalanche7.net.forgeannouncements.utils;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;



public class PermissionsHandler {

    public static final String MENTION_EVERYONE_PERMISSION = "forgeannouncements.mention.everyone";
    public static final String MENTION_PLAYER_PERMISSION = "forgeannouncements.mention.player";
    public static final int MENTION_EVERYONE_PERMISSION_LEVEL = 2;
    public static final int MENTION_PLAYER_PERMISSION_LEVEL = 2;
    public static final int BROADCAST_PERMISSION_LEVEL = 2;
    public static final int ACTIONBAR_PERMISSION_LEVEL = 2;
    public static final int TITLE_PERMISSION_LEVEL = 2;
    public static final int BOSSBAR_PERMISSION_LEVEL = 2;

    private static PermissionChecker checker;

    static {
        if (ModList.get().isLoaded("luckperms")) {
            checker = new LuckPermsChecker();
        } else if (ModList.get().isLoaded("forgeessentials")) {
            checker = new ForgeEssentialsChecker();
        } else {
            checker = new ForgePermissionChecker();
        }
    }


    public static boolean hasPermission(ServerPlayer player, String permission) {
        return checker.hasPermission(player, permission);
    }

    public interface PermissionChecker {
        boolean hasPermission(ServerPlayer player, String permission);
    }

    public static class LuckPermsChecker implements PermissionChecker {
        @Override
        public boolean hasPermission(ServerPlayer player, String permission) {
            net.luckperms.api.LuckPerms api = net.luckperms.api.LuckPermsProvider.get();
            net.luckperms.api.model.user.UserManager userManager = api.getUserManager();
            net.luckperms.api.model.user.User user = userManager.getUser(player.getUUID());

            if (user != null) {
                return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
            } else {
                return false;
            }
        }
    }

    public static class ForgeEssentialsChecker implements PermissionChecker {
        @Override
        public boolean hasPermission(ServerPlayer player, String permission) {
            return false;
        }
    }

    public static class ForgePermissionChecker implements PermissionChecker {
        @Override
        public boolean hasPermission(ServerPlayer player, String permission) {
            int permissionLevel = getPermissionLevel(permission);
            return player.hasPermissions(permissionLevel);
        }

        private int getPermissionLevel(String permission) {
            return 0;
        }
    }
}



