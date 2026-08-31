package pl.kiosel.playerlist.bridge.proxy;

import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import pl.kiosel.playerlist.bridge.BridgeProtocol;
import pl.kiosel.playerlist.bridge.BridgeMessages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class BungeePlayerList extends Plugin implements Listener, ProxyBridgeCore.Platform {

    private final Map<String, String> channels = new ConcurrentHashMap<>();
    private ProxyBridgeCore core;

    @Override
    public void onEnable() {
        core = new ProxyBridgeCore(this);
        getProxy().registerChannel(BridgeProtocol.MODERN_CHANNEL);
        getProxy().registerChannel(BridgeProtocol.LEGACY_CHANNEL);
        getProxy().getPluginManager().registerListener(this, this);
        getProxy().getScheduler().schedule(this, this::refresh, 0L, 2L, TimeUnit.SECONDS);
        getLogger().info("AdvancedPlayerList bridge enabled for BungeeCord");
    }

    @Override
    public void onDisable() {
        getProxy().getScheduler().cancel(this);
        getProxy().getPluginManager().unregisterListeners(this);
        getProxy().unregisterChannel(BridgeProtocol.MODERN_CHANNEL);
        getProxy().unregisterChannel(BridgeProtocol.LEGACY_CHANNEL);
        channels.clear();
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!isBridgeChannel(event.getTag())) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getSender() instanceof Server)) {
            return;
        }
        Server source = (Server) event.getSender();
        String serverName = source.getInfo().getName();
        channels.put(serverName, event.getTag());
        core.receive(serverName, event.getData());
    }

    private void refresh() {
        for (ServerInfo server : getProxy().getServers().values()) {
            server.ping((result, error) -> {
                ServerPing.Players players = result == null ? null : result.getPlayers();
                core.updateStatus(server.getName(), error == null && result != null,
                        players == null ? -1 : players.getMax());
            });
        }
        core.broadcast();
    }

    private boolean isBridgeChannel(String channel) {
        return BridgeProtocol.MODERN_CHANNEL.equals(channel)
                || BridgeProtocol.LEGACY_CHANNEL.equals(channel);
    }

    @Override
    public Collection<String> serverNames() {
        return new ArrayList<>(getProxy().getServers().keySet());
    }

    @Override
    public int playerCount(String serverName) {
        ServerInfo server = getProxy().getServerInfo(serverName);
        return server == null ? 0 : server.getPlayers().size();
    }

    @Override
    public List<BridgeMessages.PlayerData> players() {
        List<BridgeMessages.PlayerData> result = new ArrayList<>();
        for (ProxiedPlayer player : getProxy().getPlayers()) {
            Server server = player.getServer();
            result.add(new BridgeMessages.PlayerData(
                    player.getName(),
                    player.getUniqueId().toString(),
                    server == null ? "" : server.getInfo().getName(),
                    player.getPing()));
        }
        return result;
    }

    @Override
    public boolean sendFrame(String serverName, byte[] frame) {
        ServerInfo server = getProxy().getServerInfo(serverName);
        String channel = channels.get(serverName);
        return server != null && channel != null && server.sendData(channel, frame, true);
    }

    @Override
    public void warning(String message, Throwable throwable) {
        getLogger().log(Level.WARNING, message, throwable);
    }
}
