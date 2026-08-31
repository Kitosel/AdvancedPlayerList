package pl.kiosel.playerlist.bridge;

import lombok.Getter;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.bungeecord.ServerData;
import pl.kiosel.playerlist.model.Diagnostics;
import pl.kiosel.playerlist.model.Ticker;
import pl.kiosel.playerlist.placeholder.ComplexPlaceholder;
import pl.kiosel.playerlist.placeholder.ComplexSession;
import pl.kiosel.playerlist.placeholder.ExtraData;
import pl.kiosel.playerlist.placeholder.PlaceholderManager;
import pl.kiosel.playerlist.placeholder.complex.RemoteComplex;
import pl.kiosel.rosacore.method.Grouping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class BridgeClient implements PluginMessageListener, Runnable, AutoCloseable {

    private static final int MAX_REMOTE_LINES = 80;
    private static final long REQUEST_TIMEOUT_MILLIS = 30_000L;

    private final AdvancedPlayerList plugin;
    private final BridgeFrameCodec.Reassembler reassembler = new BridgeFrameCodec.Reassembler();
    private final Map<String, PendingRemote> requests = new ConcurrentHashMap<>();
    @Getter
    private volatile List<BridgeMessages.PlayerData> players = Collections.emptyList();
    @Getter
    private volatile boolean enabled;
    private volatile String channel;
    private volatile int updateInterval = 40;
    private volatile int elapsedTicks;
    private volatile long lastStateAt;

    public BridgeClient(AdvancedPlayerList plugin) {
        this.plugin = plugin;
    }

    public synchronized void configure(boolean enabled, int updateInterval) {
        this.updateInterval = Math.max(20, updateInterval);
        if (enabled == this.enabled) {
            return;
        }
        if (enabled) {
            registerChannel();
            this.enabled = true;
            this.elapsedTicks = this.updateInterval;
            Ticker.register(this);
            plugin.log("&aProxy bridge enabled on channel &f" + channel);
        } else {
            close();
        }
    }

	public boolean isConnected() {
        return enabled && System.currentTimeMillis() - lastStateAt <= 10_000L;
    }

	public void requestUpdate() {
        elapsedTicks = updateInterval;
    }

    @Override
    public void run() {
        cleanupRequests();
        if (++elapsedTicks >= updateInterval) {
            elapsedTicks = 0;
            sendSnapshot();
        }
    }

    public void requestRemote(RemoteComplex owner, String server, String handler) {
        if (!enabled || owner == null || server == null || handler == null) {
            return;
        }
        for (PendingRemote request : requests.values()) {
            if (request.owner == owner && request.server.equalsIgnoreCase(server)) {
                return;
            }
        }

        String requestId = UUID.randomUUID().toString();
        requests.put(requestId, new PendingRemote(owner, server));
        try {
            if (!send(BridgeProtocol.encodeRemoteRequest(
                    new BridgeMessages.RemoteRequest(requestId, server, handler)))) {
                requests.remove(requestId);
            }
        } catch (IOException exception) {
            requests.remove(requestId);
            record("Unable to create remote-handler request", exception);
        }
    }

    public void cancel(RemoteComplex owner) {
        requests.entrySet().removeIf(entry -> entry.getValue().owner == owner);
    }

    @Override
    public void onPluginMessageReceived(String incomingChannel, Player player, byte[] data) {
        if (!enabled || !incomingChannel.equals(channel)) {
            return;
        }
        try {
            byte[] packet = reassembler.accept("proxy", data);
            if (packet == null) {
                return;
            }
            switch (BridgeProtocol.packetType(packet)) {
                case BridgeProtocol.NETWORK_STATE:
                    applyState(BridgeProtocol.decodeNetworkState(packet));
                    break;
                case BridgeProtocol.REMOTE_REQUEST:
                    BridgeMessages.RemoteRequest request = BridgeProtocol.decodeRemoteRequest(packet);
                    plugin.getRosaScheduler().runForEntity(
                            player,
                            () -> answerRemoteRequest(player, request),
                            () -> { });
                    break;
                case BridgeProtocol.REMOTE_RESPONSE:
                    applyRemoteResponse(BridgeProtocol.decodeRemoteResponse(packet));
                    break;
                default:
                    throw new IOException("Unsupported proxy bridge packet");
            }
        } catch (IOException | RuntimeException exception) {
            record("Rejected proxy bridge message", exception);
        }
    }

    private void applyState(BridgeMessages.NetworkState state) {
        players = Collections.unmodifiableList(new ArrayList<>(state.getPlayers()));
        ServerData.replaceAll(state.getServers());
        lastStateAt = System.currentTimeMillis();
    }

    private void applyRemoteResponse(BridgeMessages.RemoteResponse response) {
        PendingRemote request = requests.remove(response.getRequestId());
        if (request == null) {
            return;
        }
        if (response.getError() != null && !response.getError().isEmpty()) {
            plugin.getRosaLogger().warning("Remote handler on " + request.server
                    + " failed: " + response.getError());
            return;
        }
        request.owner.update(request.server, response.getLines());
    }

    private void answerRemoteRequest(Player carrier, BridgeMessages.RemoteRequest request) {
        BridgeMessages.RemoteResponse response;
        try {
            ComplexPlaceholder placeholder = PlaceholderManager.getComplexPlaceholder(request.getHandler());
            if (placeholder == null) {
                response = new BridgeMessages.RemoteResponse(
                        request.getRequestId(),
                        "Unknown handler " + request.getHandler(),
                        Collections.emptyList());
            } else {
                ComplexSession session = placeholder.newSession(null, new Grouping());
                int size = Math.min(MAX_REMOTE_LINES, Math.max(0, session.getSize()));
                List<BridgeMessages.RemoteLine> lines = new ArrayList<>(size);
                for (int index = 0; index < size; index++) {
                    ExtraData data = session.getValue(index);
                    lines.add(data == null
                            ? new BridgeMessages.RemoteLine("", null, null, null)
                            : new BridgeMessages.RemoteLine(
                                    string(data.get(ExtraData.DATA_TEXT)),
                                    string(data.get(ExtraData.DATA_PING)),
                                    string(data.get(ExtraData.DATA_SKIN)),
                                    string(data.get(ExtraData.DATA_OPACITY))));
                }
                response = new BridgeMessages.RemoteResponse(
                        request.getRequestId(), null, lines);
            }
        } catch (Throwable throwable) {
            Diagnostics.record("Remote handler", throwable);
            response = new BridgeMessages.RemoteResponse(
                    request.getRequestId(), summarize(throwable), Collections.emptyList());
        }

        try {
            send(carrier, BridgeProtocol.encodeRemoteResponse(response));
        } catch (IOException exception) {
            record("Unable to create remote-handler response", exception);
        }
    }

    private void sendSnapshot() {
        Player carrier = firstPlayer();
        if (carrier == null) {
            return;
        }
        plugin.getRosaScheduler().runForEntity(carrier, () -> {
            List<BridgeMessages.WorldData> worlds = new ArrayList<>();
            for (World world : plugin.getServer().getWorlds()) {
                worlds.add(new BridgeMessages.WorldData(world.getName(), world.getPlayers().size()));
            }
            try {
                send(carrier, BridgeProtocol.encodeSnapshot(
                        new BridgeMessages.Snapshot(plugin.getServer().getMaxPlayers(), worlds)));
            } catch (IOException exception) {
                record("Unable to create backend snapshot", exception);
            }
        }, () -> { });
    }

    private boolean send(byte[] packet) throws IOException {
        Player carrier = firstPlayer();
        if (carrier == null) {
            return false;
        }
        plugin.getRosaScheduler().runForEntity(
                carrier,
                () -> sendUnchecked(carrier, packet),
                () -> { });
        return true;
    }

    private void send(Player carrier, byte[] packet) throws IOException {
        for (byte[] frame : BridgeFrameCodec.frame(packet)) {
            carrier.sendPluginMessage(plugin, channel, frame);
        }
    }

    private void sendUnchecked(Player carrier, byte[] packet) {
        try {
            send(carrier, packet);
        } catch (IOException exception) {
            record("Unable to frame bridge packet", exception);
        }
    }

    private Player firstPlayer() {
        Iterator<? extends Player> iterator = plugin.getServer().getOnlinePlayers().iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    private void registerChannel() {
        try {
            register(BridgeProtocol.MODERN_CHANNEL);
        } catch (IllegalArgumentException exception) {
            register(BridgeProtocol.LEGACY_CHANNEL);
        }
    }

    private void register(String selectedChannel) {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, selectedChannel);
        try {
            plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, selectedChannel, this);
            channel = selectedChannel;
        } catch (RuntimeException exception) {
            plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, selectedChannel);
            throw exception;
        }
    }

    private void cleanupRequests() {
        long expired = System.currentTimeMillis() - REQUEST_TIMEOUT_MILLIS;
        requests.entrySet().removeIf(entry -> entry.getValue().createdAt < expired);
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String summarize(Throwable throwable) {
        String message = throwable.getMessage();
        return throwable.getClass().getSimpleName()
                + (message == null || message.isEmpty() ? "" : ": " + message);
    }

    private void record(String message, Throwable throwable) {
        Diagnostics.record("Bridge", throwable);
        plugin.getRosaLogger().log(Level.WARNING, message, throwable);
    }

    @Override
    public synchronized void close() {
        if (!enabled && channel == null) {
            return;
        }
        enabled = false;
        Ticker.unregister(this);
        requests.clear();
        players = Collections.emptyList();
        ServerData.SERVERS.clear();
        if (channel != null) {
            plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, channel, this);
            plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, channel);
            channel = null;
        }
    }

    private static final class PendingRemote {
        private final RemoteComplex owner;
        private final String server;
        private final long createdAt = System.currentTimeMillis();

        private PendingRemote(RemoteComplex owner, String server) {
            this.owner = owner;
            this.server = server;
        }
    }
}
