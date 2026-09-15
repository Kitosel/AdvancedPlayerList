package pl.kiosel.playerlist.protocol;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.internal.UUIDSet;
import pl.kiosel.playerlist.model.Diagnostics;
import pl.kiosel.playerlist.model.Ticker;
import pl.kiosel.playerlist.tablist.Tablist;
import pl.kiosel.playerlist.util.FakePlayer;
import pl.kiosel.rosacore.RosaLogger;
import pl.kiosel.rosacore.nms.api.packet.PlayerInfoPacketEvent;
import pl.kiosel.rosacore.nms.api.packet.PlayerRespawnPacketEvent;
import pl.kiosel.rosacore.nms.api.packet.PlayerSpawnPacketEvent;
import pl.kiosel.rosacore.nms.api.packet.TabPacketAction;
import pl.kiosel.rosacore.nms.api.packet.TabPacketEntry;
import pl.kiosel.rosacore.nms.api.packet.TabPacketInterceptor;
import pl.kiosel.rosacore.nms.api.packet.TabPacketListener;
import pl.kiosel.rosacore.nms.api.status.ServerStatusInterceptor;
import pl.kiosel.rosacore.nms.api.status.ServerStatusPacketEvent;
import pl.kiosel.rosacore.nms.api.status.ServerStatusSample;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class ProtocolListener {

    private final AdvancedPlayerList plugin;
    private final TabPacketInterceptor tabPackets;
    private final ServerStatusInterceptor serverStatus;
    private final Map<UUID, TabPacketInterceptor.Subscription> subscriptions = new ConcurrentHashMap<>();
    private ServerStatusInterceptor.Subscription statusSubscription;

    public ProtocolListener(AdvancedPlayerList plugin) {
        this.plugin = plugin;
        this.tabPackets = plugin.getNMS().getTabPacketInterceptor();
        this.serverStatus = plugin.getNMS().getServerStatusInterceptor();
        try {
            this.statusSubscription = serverStatus.subscribe(this::onServerStatus);
        } catch (RuntimeException exception) {
            Diagnostics.record("RosaCore server status interceptor", exception);
            RosaLogger.getInstance().log(Level.WARNING,
                    "Unable to enable fake players in the server-list response", exception);
        }
    }

    public void enable(Player viewer) {
        if (viewer == null || !viewer.isOnline()) return;
        disable(viewer);
        try {
            TabPacketInterceptor.Subscription subscription =
                    tabPackets.subscribe(viewer, new ViewerPacketListener(viewer));
            subscriptions.put(viewer.getUniqueId(), subscription);
        } catch (RuntimeException exception) {
            Diagnostics.record("RosaCore tab packet interceptor", exception);
            RosaLogger.getInstance().log(Level.WARNING,
                    "Unable to intercept tab packets for " + viewer.getName(), exception);
        }
    }

    public void disable(Player viewer) {
        if (viewer == null) return;
        TabPacketInterceptor.Subscription subscription = subscriptions.remove(viewer.getUniqueId());
        if (subscription != null) subscription.close();
    }

    public void disable() {
        for (TabPacketInterceptor.Subscription subscription : subscriptions.values()) {
            subscription.close();
        }
        subscriptions.clear();
        if (statusSubscription != null) {
            statusSubscription.close();
            statusSubscription = null;
        }
    }

    public void undoRemove(UUID uuid) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Tablist tablist = plugin.getTablistManager().getTablist(player);
            if (tablist != null) tablist.getHandler().removeQueue(uuid);
        }
    }

    private void onServerStatus(ServerStatusPacketEvent event) {
        if (plugin.getPlayerBank() == null || plugin.getConfigFile() == null) return;
        boolean playerCount = plugin.getConfigFile().getBoolean("fake-player.server-list.player-count");
        boolean playerList = plugin.getConfigFile().getBoolean("fake-player.server-list.player-list");
        if (playerCount) {
            event.setOnlinePlayers(event.getOnlinePlayers() + plugin.getPlayerBank().getFakePlayerCount());
        }
        if (playerList) {
            for (FakePlayer fakePlayer : plugin.getPlayerBank().getFakePlayers()) {
                event.addSample(new ServerStatusSample(fakePlayer.getUniqueId(), fakePlayer.getName()));
            }
        }
    }

    private void schedulePlayerRemoval(Tablist tablist, UUID profileId, Player viewer) {
        if (profileId == null || UUIDSet.getSet().contains(tablist, profileId, viewer)) return;
        tablist.getHandler().removePlayer(profileId);
    }

    private final class ViewerPacketListener implements TabPacketListener {
        private final Player viewer;

        private ViewerPacketListener(Player viewer) {
            this.viewer = viewer;
        }

        @Override
        public void onPlayerInfo(PlayerInfoPacketEvent event) {
            if (event.isRosaCorePacket()) return;
            Tablist tablist = plugin.getTablistManager().getTablist(viewer);
            if (tablist == null || tablist.getNativeTabList() == null) return;

            if (event.hasAction(TabPacketAction.ADD_PLAYER)) {
                boolean restoreHiddenSelf = false;
                for (TabPacketEntry entry : event.getEntries()) {
                    schedulePlayerRemoval(tablist, entry.getUniqueId(), viewer);
                    restoreHiddenSelf |= Protocol.usesModernPlayerInfo()
                            && viewer.getUniqueId().equals(entry.getUniqueId()) && entry.isListed();
                }
                if (restoreHiddenSelf) {
                    event.afterSend(() -> hideViewerAgain(tablist));
                }
                return;
            }

            if (event.hasAction(TabPacketAction.UPDATE_LISTED)) {
                boolean restoreHiddenSelf = false;
                for (TabPacketEntry entry : event.getEntries()) {
                    UUID profileId = entry.getUniqueId();
                    restoreHiddenSelf |= viewer.getUniqueId().equals(profileId) && entry.isListed();
                    if (profileId != null && !UUIDSet.getSet().contains(tablist, profileId, viewer)) {
                        if (entry.isListed()) {
                            tablist.getHandler().removePlayer(profileId);
                        } else {
                            tablist.getHandler().removeQueue(profileId);
                        }
                    }
                }
                if (restoreHiddenSelf) {
                    event.afterSend(() -> hideViewerAgain(tablist));
                }
            }

            if (!Protocol.usesModernPlayerInfo()
                    && (event.hasAction(TabPacketAction.UPDATE_LATENCY)
                    || event.hasAction(TabPacketAction.UPDATE_DISPLAY_NAME))) {
                event.setCancelled(true);
            }
        }

        @Override
        public void onPlayerSpawn(PlayerSpawnPacketEvent event) {
            if (event.isRosaCorePacket()) return;
            UUID spawnedId = event.getSpawnedUniqueId();
            Ticker.delay(viewer, () -> {
                if (plugin.getTablistManager() == null) return;
                Tablist tablist = plugin.getTablistManager().getTablist(viewer);
                if (tablist == null || tablist.getNativeTabList() == null) return;
                Player spawned = Bukkit.getPlayer(spawnedId);
                if (spawned != null) tablist.getHandler().addPlayer(spawned);
            }, 0L);
        }

        @Override
        public void onPlayerRespawn(PlayerRespawnPacketEvent event) {
            if (event.isRosaCorePacket()) return;
            Ticker.delay(viewer, () -> {
                if (plugin.getTablistManager() == null) return;
                Tablist tablist = plugin.getTablistManager().getTablist(viewer);
                if (tablist != null && tablist.getNativeTabList() != null) {
                    tablist.getHandler().addPlayer(viewer);
                }
            }, 0L);
        }

        private void hideViewerAgain(Tablist expected) {
            Tablist current = plugin.getTablistManager().getTablist(viewer);
            if (current == expected && current.getNativeTabList() != null) {
                current.getNativeTabList().hideRealPlayer(viewer);
            }
        }
    }
}
