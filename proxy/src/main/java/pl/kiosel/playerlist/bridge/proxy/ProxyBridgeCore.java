package pl.kiosel.playerlist.bridge.proxy;

import pl.kiosel.playerlist.bridge.BridgeProtocol;
import pl.kiosel.playerlist.bridge.BridgeFrameCodec;
import pl.kiosel.playerlist.bridge.BridgeMessages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class ProxyBridgeCore {

    interface Platform {
        Collection<String> serverNames();
        int playerCount(String serverName);
        List<BridgeMessages.PlayerData> players();
        boolean sendFrame(String serverName, byte[] frame);
        void warning(String message, Throwable throwable);
    }

    private static final long STATUS_TIMEOUT_MILLIS = 10_000L;
    private static final long REQUEST_TIMEOUT_MILLIS = 30_000L;

    private final Platform platform;
    private final BridgeFrameCodec.Reassembler reassembler = new BridgeFrameCodec.Reassembler();
    private final Map<String, TimedSnapshot> snapshots = new ConcurrentHashMap<>();
    private final Map<String, ServerStatus> statuses = new ConcurrentHashMap<>();
    private final Map<String, PendingRequest> requests = new ConcurrentHashMap<>();

    ProxyBridgeCore(Platform platform) {
        this.platform = platform;
    }

    void receive(String sourceServer, byte[] frame) {
        try {
            byte[] packet = reassembler.accept(sourceServer, frame);
            if (packet == null) {
                return;
            }
            switch (BridgeProtocol.packetType(packet)) {
                case BridgeProtocol.SNAPSHOT:
                    snapshots.put(sourceServer,
                            new TimedSnapshot(BridgeProtocol.decodeSnapshot(packet)));
                    break;
                case BridgeProtocol.REMOTE_REQUEST:
                    routeRequest(sourceServer, BridgeProtocol.decodeRemoteRequest(packet));
                    break;
                case BridgeProtocol.REMOTE_RESPONSE:
                    routeResponse(sourceServer, BridgeProtocol.decodeRemoteResponse(packet));
                    break;
                default:
                    throw new IOException("Unsupported backend bridge packet");
            }
        } catch (IOException | RuntimeException exception) {
            platform.warning("Rejected bridge message from " + sourceServer, exception);
        }
    }

    void updateStatus(String serverName, boolean online, int maxPlayers) {
        statuses.put(serverName, new ServerStatus(online, maxPlayers));
    }

    void broadcast() {
        cleanupRequests();
        try {
            byte[] packet = BridgeProtocol.encodeNetworkState(createState());
            for (String server : platform.serverNames()) {
                send(server, packet);
            }
        } catch (IOException exception) {
            platform.warning("Unable to create network state", exception);
        }
    }

    private BridgeMessages.NetworkState createState() {
        long now = System.currentTimeMillis();
        List<BridgeMessages.ServerState> servers = new ArrayList<>();
        for (String name : platform.serverNames()) {
            TimedSnapshot timedSnapshot = snapshots.get(name);
            BridgeMessages.Snapshot snapshot = timedSnapshot == null ? null : timedSnapshot.snapshot;
            ServerStatus status = statuses.get(name);
            int connected = platform.playerCount(name);
            boolean recentStatus = status != null && now - status.updatedAt <= STATUS_TIMEOUT_MILLIS;
            boolean recentSnapshot = timedSnapshot != null
                    && now - timedSnapshot.updatedAt <= STATUS_TIMEOUT_MILLIS;
            boolean online = connected > 0 || recentSnapshot || recentStatus && status.online;
            int maxPlayers = status != null && status.maxPlayers >= 0
                    ? status.maxPlayers
                    : snapshot == null ? -1 : snapshot.getMaxPlayers();
            List<BridgeMessages.WorldData> worlds = snapshot == null
                    ? Collections.emptyList()
                    : snapshot.getWorlds();
            servers.add(new BridgeMessages.ServerState(
                    name, online, connected, maxPlayers, worlds));
        }
        return new BridgeMessages.NetworkState(servers, platform.players());
    }

    private void routeRequest(String sourceServer, BridgeMessages.RemoteRequest request) throws IOException {
        String target = findServer(request.getTargetServer());
        if (target == null) {
            sendError(sourceServer, request.getRequestId(),
                    "Unknown target server " + request.getTargetServer());
            return;
        }

        requests.put(request.getRequestId(), new PendingRequest(sourceServer, target));
        if (!send(target, BridgeProtocol.encodeRemoteRequest(request))) {
            requests.remove(request.getRequestId());
            sendError(sourceServer, request.getRequestId(),
                    "Target server " + target + " has no active bridge connection");
        }
    }

    private void routeResponse(String sourceServer, BridgeMessages.RemoteResponse response) throws IOException {
        PendingRequest request = requests.get(response.getRequestId());
        if (request == null || !request.targetServer.equalsIgnoreCase(sourceServer)) {
            return;
        }
        if (!requests.remove(response.getRequestId(), request)) {
            return;
        }
        send(request.requesterServer, BridgeProtocol.encodeRemoteResponse(response));
    }

    private void sendError(String server, String requestId, String error) throws IOException {
        send(server, BridgeProtocol.encodeRemoteResponse(new BridgeMessages.RemoteResponse(
                requestId, error, Collections.emptyList())));
    }

    private boolean send(String server, byte[] packet) throws IOException {
        boolean sent = true;
        for (byte[] frame : BridgeFrameCodec.frame(packet)) {
            sent = platform.sendFrame(server, frame) && sent;
        }
        return sent;
    }

    private String findServer(String requested) {
        if (requested == null) {
            return null;
        }
        for (String server : platform.serverNames()) {
            if (server.equalsIgnoreCase(requested)) {
                return server;
            }
        }
        return null;
    }

    private void cleanupRequests() {
        long expired = System.currentTimeMillis() - REQUEST_TIMEOUT_MILLIS;
        requests.entrySet().removeIf(entry -> entry.getValue().createdAt < expired);
    }

    private static final class TimedSnapshot {
        private final BridgeMessages.Snapshot snapshot;
        private final long updatedAt = System.currentTimeMillis();

        private TimedSnapshot(BridgeMessages.Snapshot snapshot) {
            this.snapshot = snapshot;
        }
    }

    private static final class ServerStatus {
        private final boolean online;
        private final int maxPlayers;
        private final long updatedAt = System.currentTimeMillis();

        private ServerStatus(boolean online, int maxPlayers) {
            this.online = online;
            this.maxPlayers = maxPlayers;
        }
    }

    private static final class PendingRequest {
        private final String requesterServer;
        private final String targetServer;
        private final long createdAt = System.currentTimeMillis();

        private PendingRequest(String requesterServer, String targetServer) {
            this.requesterServer = requesterServer;
            this.targetServer = targetServer;
        }
    }
}
