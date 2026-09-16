package pl.kiosel.playerlist.placeholder.parameterized;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.bridge.BridgeMessages;
import pl.kiosel.playerlist.model.WorldGroup;
import pl.kiosel.playerlist.placeholder.ExtraData;
import pl.kiosel.playerlist.placeholder.ParameterizedPlaceholder;
import pl.kiosel.rosacore.version.Version;

import java.util.Locale;

public class PlayerParameterized implements ParameterizedPlaceholder {

    @Override
    public boolean accept(String placeholder) {
        return "player".equals(placeholder);
    }

    @Override
    public void onRegistered() {
    }
    
    @Override
    public void onUnregistered() {
    }
    
    @Override
    public String provide(String placeholder, String param, ExtraData data) {
        if (param == null) {
            return null;
        }
        if (!placeholder.equals("player")) {
            return null;
        }

        Object player = data.get(ExtraData.DATA_PLAYER);
        if (player instanceof BridgeMessages.PlayerData) {
            BridgeMessages.PlayerData networkPlayer = (BridgeMessages.PlayerData) player;
            switch (param.toLowerCase(Locale.ROOT)) {
                case "name":
                case "displayname":
                case "listname":
                    return networkPlayer.getName();
                case "uuid":
                    return networkPlayer.getUniqueId();
                case "ping":
                    return Integer.toString(networkPlayer.getPing());
                case "server":
                    return networkPlayer.getServer();
                case "is_online":
                    return "true";
            }
        } else if (player instanceof OfflinePlayer) {
            final OfflinePlayer op = (OfflinePlayer)player;
            String normalizedParam = param.toLowerCase(Locale.ROOT);
            switch (normalizedParam) {
                case "name":
                    return op.getName();
                case "displayname":
                    return player instanceof Player ? ((Player) player).getDisplayName() : op.getName();
                case "listname":
                    return player instanceof Player ? ((Player) player).getPlayerListName() : op.getName();
                case "uuid":
                    return op.getUniqueId().toString();
                case "ping":
                    if (player instanceof Player) {
                        int ping = AdvancedPlayerList.getInstance().getNMS()
                                .getNmsServer().getPlayerPing((Player) player);
                        return Integer.toString(Math.max(0, ping));
                    }
                    break;
                case "is_online":
                    return String.valueOf(op.isOnline());
                case "is_banned":
                    return String.valueOf(op.isBanned());
                case "is_whitelisted":
                    return String.valueOf(op.isWhitelisted());
                case "is_op":
                    return String.valueOf(op.isOp());
            }
            if (op.isOnline()) {
                Player onlineplayer = op.getPlayer();
                switch (normalizedParam) {
					case "biome":
                        return getBiome(onlineplayer);
                    case "level":
                        return String.valueOf(onlineplayer.getLevel());
                    case "exp":
                        return String.valueOf(onlineplayer.getExp());
                }
            }
            if (normalizedParam.startsWith("has_permission_")) {
                String permission = param.substring("has_permission_".length());
                return String.valueOf(player instanceof Player
                        && !permission.isEmpty()
                        && ((Player) player).hasPermission(permission));
            }
        }
        String[] par = param.split(":", 2);
        if (par.length == 2 && par[0].equals("worldgroup") && player instanceof Player) {
            for (MetadataValue value : ((Player)player).getMetadata("playerGroup:" + par[1])) {
                Object val = value.value();
                if (val instanceof WorldGroup) {
                    return ((WorldGroup)val).getName();
                }
            }
        }
        if ("world".equals(param) && player instanceof Player) {
            for (MetadataValue value : ((Player)player).getMetadata("playerGroup")) {
                Object val = value.value();
                if (val instanceof WorldGroup) {
                    return ((WorldGroup)val).getName();
                }
            }
            return ((Player) player).getWorld().getName();
        }
        return null;
    }

    private String getBiome(Player player) {
        if (Version.isServerVersionAtLeast(Version.V1_21_3)) {
            return player.getLocation().getBlock().getBiome().name();
        }

        return String.valueOf(player.getLocation().getBlock().getBiome());
    }
}
