package pl.kiosel.playerlist.bridge;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BridgeMessages {

    private BridgeMessages() {
    }

    private static <T> List<T> immutable(List<T> values) {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    @Getter
    public static final class Snapshot {
        private final int maxPlayers;
        private final List<WorldData> worlds;

        public Snapshot(int maxPlayers, List<WorldData> worlds) {
            this.maxPlayers = maxPlayers;
            this.worlds = immutable(worlds);
        }

	}

    @Getter
    public static final class WorldData {
        private final String name;
        private final int playerCount;

        public WorldData(String name, int playerCount) {
            this.name = name;
            this.playerCount = playerCount;
        }

	}

    @Getter
    public static final class ServerState {
        private final String name;
        private final boolean online;
        private final int playerCount;
        private final int maxPlayers;
        private final List<WorldData> worlds;

        public ServerState(String name, boolean online, int playerCount, int maxPlayers, List<WorldData> worlds) {
            this.name = name;
            this.online = online;
            this.playerCount = playerCount;
            this.maxPlayers = maxPlayers;
            this.worlds = immutable(worlds);
        }

	}

    @Getter
    public static final class PlayerData {
        private final String name;
        private final String uniqueId;
        private final String server;
        private final int ping;

        public PlayerData(String name, String uniqueId, String server, int ping) {
            this.name = name;
            this.uniqueId = uniqueId;
            this.server = server;
            this.ping = ping;
        }

	}

    @Getter
    public static final class NetworkState {
        private final List<ServerState> servers;
        private final List<PlayerData> players;

        public NetworkState(List<ServerState> servers, List<PlayerData> players) {
            this.servers = immutable(servers);
            this.players = immutable(players);
        }

	}

    @Getter
    public static final class RemoteRequest {
        private final String requestId;
        private final String targetServer;
        private final String handler;

        public RemoteRequest(String requestId, String targetServer, String handler) {
            this.requestId = requestId;
            this.targetServer = targetServer;
            this.handler = handler;
        }

	}

    @Getter
    public static final class RemoteLine {
        private final String text;
        private final String ping;
        private final String skin;
        private final String opacity;

        public RemoteLine(String text, String ping, String skin, String opacity) {
            this.text = text;
            this.ping = ping;
            this.skin = skin;
            this.opacity = opacity;
        }

	}

    @Getter
    public static final class RemoteResponse {
        private final String requestId;
        private final String error;
        private final List<RemoteLine> lines;

        public RemoteResponse(String requestId, String error, List<RemoteLine> lines) {
            this.requestId = requestId;
            this.error = error;
            this.lines = immutable(lines);
        }

	}
}
