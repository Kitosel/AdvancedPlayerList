package pl.kiosel.playerlist.placeholder.parameterized;

import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.bungeecord.ServerData;
import pl.kiosel.playerlist.bungeecord.ServerWorldData;
import pl.kiosel.playerlist.bridge.BridgeMessages;
import pl.kiosel.playerlist.model.Ticker;
import pl.kiosel.playerlist.model.WorldGroup;
import pl.kiosel.playerlist.placeholder.ExtraData;
import pl.kiosel.playerlist.placeholder.ParameterizedPlaceholder;
import org.bukkit.Bukkit;
import org.bukkit.World;
import pl.kiosel.rosacore.utils.NumberUtils;

public class InternalParameterized implements ParameterizedPlaceholder {

    @Override
    public boolean accept(String placeholder) {
        return "server".equals(placeholder) || "world".equals(placeholder) || "tablist".equals(placeholder);
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
                    case "tps":
					case "tps_1":
						return NumberUtils.formatTps(AdvancedPlayerList.getInstance().getNMS().getNmsServer().getTpsInLastMinute());
					case "is_online":
						return String.valueOf(server.isOnline());
					case "online":
					case "player_count":
						return String.valueOf(server.getPlayerCount());
					case "name":
						return server.getServerName();
					case "max_players":
						return String.valueOf(server.getMaxPlayers());
				}
            } else {
                switch (param) {
                    case "name":
                        return AdvancedPlayerList.getInstance().getServer().getServerName();
                    case "online":
                    case "player_count":
                        return String.valueOf(Bukkit.getOnlinePlayers().size());
                    case "max_players":
                        return String.valueOf(Bukkit.getMaxPlayers());
                    case "tps":
                    case "tps_1":
                        return NumberUtils.formatTps(AdvancedPlayerList.getInstance().getNMS().getNmsServer().getTpsInLastMinute());
                }
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
            if (worldData instanceof BridgeMessages.WorldData) {
                BridgeMessages.WorldData world = (BridgeMessages.WorldData) worldData;
                if ("name".equals(param)) {
                    return world.getName();
                }
                if ("player_count".equals(param)) {
                    return Integer.toString(world.getPlayerCount());
                }
            } else if (worldData instanceof ServerWorldData) {
                ServerWorldData world = (ServerWorldData) worldData;
				switch (param) {
		            case "name":
						return world.getName();
					case "player_count":
						return Integer.toString(world.getPlayerCount());
					case "server":
						return world.getServerName();
				}
			} else if (worldData instanceof World) {
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
        }
        return null;
    }
}
