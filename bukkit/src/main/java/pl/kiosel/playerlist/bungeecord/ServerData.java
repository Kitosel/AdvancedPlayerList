package pl.kiosel.playerlist.bungeecord;

import lombok.Getter;
import pl.kiosel.playerlist.bridge.BridgeMessages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public final class ServerData {

    public static final Map<String, ServerData> SERVERS = new ConcurrentHashMap<>();

    private final int playerCount;
    private final int maxPlayers;
    private final boolean online;
    private final String serverName;
    private final List<ServerWorldData> worlds;

    private ServerData(BridgeMessages.ServerState state) {
        this.playerCount = state.getPlayerCount();
        this.maxPlayers = state.getMaxPlayers();
        this.online = state.isOnline();
        this.serverName = state.getName();
        this.worlds = new ArrayList<>();
        for (BridgeMessages.WorldData world : state.getWorlds()) {
            this.worlds.add(new ServerWorldData(
                    state.getName(), world.getName(), world.getPlayerCount()));
        }
    }

    public static void replaceAll(List<BridgeMessages.ServerState> states) {
        Map<String, ServerData> updated = new ConcurrentHashMap<>();
        for (BridgeMessages.ServerState state : states) {
            if (state.getName() != null) {
                updated.put(state.getName(), new ServerData(state));
            }
        }
        SERVERS.clear();
        SERVERS.putAll(updated);
    }
}
