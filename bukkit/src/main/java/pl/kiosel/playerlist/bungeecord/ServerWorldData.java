package pl.kiosel.playerlist.bungeecord;

import lombok.Getter;

@Getter
public final class ServerWorldData {
    private final String serverName;
    private final String name;
    private final int playerCount;

    public ServerWorldData(String serverName, String name, int playerCount) {
        this.serverName = serverName;
        this.name = name;
        this.playerCount = playerCount;
    }
}
