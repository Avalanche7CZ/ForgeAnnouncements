package eu.avalanche7.forgeannouncements.utils;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
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
        if (Loader.isModLoaded("luckperms")) {
            checker = new LuckPermsChecker();
        } else if (Loader.isModLoaded("forgeessentials")) {
            checker = new ForgeEssentialsChecker();
        } else {
            checker = new ForgePermissionChecker();
        }
    }

    public static boolean hasPermission(EntityPlayerMP player, String permission) {
        return checker.hasPermission(player, permission);
    }

    public interface PermissionChecker {
        boolean hasPermission(EntityPlayerMP player, String permission);
    }

    public static class LuckPermsChecker implements PermissionChecker {
        @Override
        public boolean hasPermission(EntityPlayerMP player, String permission) {
            net.luckperms.api.LuckPerms api = net.luckperms.api.LuckPermsProvider.get();
            net.luckperms.api.model.user.User user = api.getUserManager().getUser(player.getUniqueID());

            if (user != null) {
                return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
            } else {
                return false;
            }
        }
    }

    public static class ForgeEssentialsChecker implements PermissionChecker {
        @Override
        public boolean hasPermission(EntityPlayerMP player, String permission) {
            //return com.forgeessentials.api.permissions.RegGroupManager.hasPermission(player, permission);
            return false;
        }
    }

    public static class ForgePermissionChecker implements PermissionChecker {
        @Override
        public boolean hasPermission(EntityPlayerMP player, String permission) {
            int permissionLevel = getPermissionLevel(permission);
            return player.canUseCommand(permissionLevel, "");
        }

        private int getPermissionLevel(String permission) {
            switch (permission) {
                case MENTION_EVERYONE_PERMISSION:
                    return MENTION_EVERYONE_PERMISSION_LEVEL;
                case MENTION_PLAYER_PERMISSION:
                    return MENTION_PLAYER_PERMISSION_LEVEL;
                default:
                    return 0;
            }
        }
    }
}
