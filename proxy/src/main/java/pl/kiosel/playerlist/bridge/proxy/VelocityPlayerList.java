package pl.kiosel.playerlist.bridge.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import org.slf4j.Logger;
import pl.kiosel.playerlist.bridge.BridgeProtocol;
import pl.kiosel.playerlist.bridge.BridgeMessages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = "advancedplayerlistbridge",
        name = "AdvancedPlayerListBridge",
        version = "1.1.0",
        authors = {"Kiosel"}
)
public final class VelocityPlayerList implements ProxyBridgeCore.Platform {

    private static final ChannelIdentifier MODERN =
            MinecraftChannelIdentifier.from(BridgeProtocol.MODERN_CHANNEL);
    private static final ChannelIdentifier LEGACY =
            new LegacyChannelIdentifier(BridgeProtocol.LEGACY_CHANNEL);

    private final ProxyServer proxy;
    private final Logger logger;
    private final Map<String, ChannelIdentifier> channels = new ConcurrentHashMap<>();
    private ProxyBridgeCore core;
    private ScheduledTask task;

    @Inject
    public VelocityPlayerList(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        core = new ProxyBridgeCore(this);
        proxy.getChannelRegistrar().register(MODERN, LEGACY);
        task = proxy.getScheduler().buildTask(this, this::refresh)
                .repeat(2L, TimeUnit.SECONDS)
                .schedule();
        logger.info("AdvancedPlayerList bridge enabled for Velocity");
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        if (task != null) {
            task.cancel();
            task = null;
        }
        proxy.getChannelRegistrar().unregister(MODERN, LEGACY);
        channels.clear();
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!MODERN.equals(event.getIdentifier()) && !LEGACY.equals(event.getIdentifier())) {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection)) {
            return;
        }
        ServerConnection source = (ServerConnection) event.getSource();
        String serverName = source.getServerInfo().getName();
        channels.put(serverName, event.getIdentifier());
        core.receive(serverName, event.getData());
    }

    private void refresh() {
        for (RegisteredServer server : proxy.getAllServers()) {
            server.ping().whenComplete((result, error) -> core.updateStatus(
                    server.getServerInfo().getName(),
                    error == null && result != null,
                    result == null || !result.getPlayers().isPresent()
                            ? -1
                            : result.getPlayers().get().getMax()));
        }
        core.broadcast();
    }

    @Override
    public Collection<String> serverNames() {
        List<String> result = new ArrayList<>();
        for (RegisteredServer server : proxy.getAllServers()) {
            result.add(server.getServerInfo().getName());
        }
        return result;
    }

    @Override
    public int playerCount(String serverName) {
        return proxy.getServer(serverName)
                .map(server -> server.getPlayersConnected().size())
                .orElse(0);
    }

    @Override
    public List<BridgeMessages.PlayerData> players() {
        List<BridgeMessages.PlayerData> result = new ArrayList<>();
        for (Player player : proxy.getAllPlayers()) {
            String server = player.getCurrentServer()
                    .map(connection -> connection.getServerInfo().getName())
                    .orElse("");
            long ping = player.getPing();
            result.add(new BridgeMessages.PlayerData(
                    player.getUsername(),
                    player.getUniqueId().toString(),
                    server,
                    ping > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, ping)));
        }
        return result;
    }

    @Override
    public boolean sendFrame(String serverName, byte[] frame) {
        ChannelIdentifier channel = channels.get(serverName);
        return channel != null && proxy.getServer(serverName)
                .map(server -> server.sendPluginMessage(channel, frame))
                .orElse(false);
    }

    @Override
    public void warning(String message, Throwable throwable) {
        logger.warn(message, throwable);
    }
}
