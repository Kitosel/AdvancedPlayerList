package pl.kiosel.playerlist.placeholder.parameterized;

import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.bungeecord.ServerData;
import pl.kiosel.playerlist.model.Ticker;
import pl.kiosel.playerlist.model.WorldGroup;
import pl.kiosel.playerlist.placeholder.ExtraData;
import pl.kiosel.playerlist.placeholder.ParameterizedPlaceholder;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import java.util.Locale;

public class InternalParameterized implements ParameterizedPlaceholder {

    @Override
    public boolean accept(String placeholder) {
        return "server".equals(placeholder) || "world".equals(placeholder) || "player".equals(placeholder) || "tablist".equals(placeholder);
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
        if ("server".equals(placeholder)) {
            ServerData server = data.get(ExtraData.DATA_SERVER);
            if (server != null) {
				switch (param) {
		            case "is_online":
						return String.valueOf(server.isOnline());
					case "name":
						return server.getServerName();
					case "player_count":
						return String.valueOf(server.getPlayerCount());
					case "max_players":
						return String.valueOf(server.getMaxPlayers());
				}
			} else if ("name".equals(param)) {
                return AdvancedPlayerList.getInstance().getName();
            }
        } else if ("tablist".equals(placeholder)) {
            if ("performanceMS".equals(param)) {
                return String.valueOf(Ticker.getAverageNanoSecondsPerTick() / 1000000L);
            }
            if ("performanceNS".equals(param)) {
                return String.valueOf(Ticker.getAverageNanoSecondsPerTick());
            }
        } else if ("world".equals(placeholder)) {
            Object worldData = data.get(ExtraData.DATA_WORLD);
            if (worldData instanceof World) {
                World world = (World)worldData;
                if ("name".equals(param)) {
                    return world.getName();
                }
                if ("player_count".equals(param)) {
                    return Integer.toString(world.getPlayers().size());
                }
            } else if (worldData instanceof WorldGroup) {
                WorldGroup group = (WorldGroup)worldData;
                if ("name".equals(param)) {
                    return group.getName();
                }
                if ("player_count".equals(param)) {
                    return Integer.toString(group.collectPlayers().size());
                }
            }
        } else if ("player".equals(placeholder)) {
            Object player = data.get(ExtraData.DATA_PLAYER);
            if (player instanceof OfflinePlayer) {
                final OfflinePlayer op = (OfflinePlayer)player;
                String normalizedParam = param.toLowerCase(Locale.ROOT);
                switch (normalizedParam) {
                    case "name":
                    case "displayname":
                        return op.getName();
                    case "uuid":
                        return op.getUniqueId().toString();
                    case "is_online":
                        return String.valueOf(op.isOnline());
                    case "is_banned":
                        return String.valueOf(op.isBanned());
                    case "is_whitelisted":
                        return String.valueOf(op.isWhitelisted());
                    case "is_op":
                        return String.valueOf(op.isOp());
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
            }
        }
        return null;
    }
}
