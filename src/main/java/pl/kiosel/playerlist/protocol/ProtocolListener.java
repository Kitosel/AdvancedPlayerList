package pl.kiosel.playerlist.protocol;

import com.comphenix.protocol.reflect.FieldAccessException;
import pl.kiosel.playerlist.util.FakePlayer;
import pl.kiosel.playerlist.internal.UUIDSet;
import pl.kiosel.playerlist.tablist.Tablist;
import org.bukkit.entity.Player;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.EnumWrappers;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import java.util.UUID;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import java.util.ArrayList;
import com.comphenix.protocol.wrappers.WrappedServerPing;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.rosacore.RosaLogger;

@SuppressWarnings("deprecation")
public class ProtocolListener extends PacketAdapter {

    private final AdvancedPlayerList plugin;

    public ProtocolListener(AdvancedPlayerList plugin) {
        super(plugin, ListenerPriority.LOWEST, packetTypes());
        this.plugin = plugin;
        ProtocolLibrary.getProtocolManager().addPacketListener(this);
    }

    private static PacketType[] packetTypes() {
        PacketType playerSpawn = Protocol.usesModernPlayerInfo()
                ? PacketType.Play.Server.SPAWN_ENTITY
                : PacketType.Play.Server.NAMED_ENTITY_SPAWN;
        return new PacketType[]{
                playerSpawn,
                PacketType.Play.Server.RESPAWN,
                PacketType.Play.Server.PLAYER_INFO,
                PacketType.Status.Server.SERVER_INFO
        };
    }
    
    public void disable() {
        ProtocolLibrary.getProtocolManager().removePacketListener(this);
    }
    
    public void onPacketSending(PacketEvent e) {
        PacketContainer packet = e.getPacket();
        if (e.getPacketType() == PacketType.Status.Server.SERVER_INFO) {
            boolean playerCount = plugin.getConfigFile().getBoolean("fake-player.server-list.player-count");
            boolean playerList = plugin.getConfigFile().getBoolean("fake-player.server-list.player-list");

            if (playerCount || playerList) {
                StructureModifier<WrappedServerPing> pings = packet.getServerPings();
                WrappedServerPing ping = pings.read(0);

                if (playerList) {
                    List<WrappedGameProfile> profile = new ArrayList<>(ping.getPlayers());
                    for (final FakePlayer dp : plugin.getPlayerBank().getFakePlayers()) {
                        profile.add(new WrappedGameProfile(dp.getUniqueId(), dp.getName()));
                    }
                    ping.setPlayers(profile);
                }
                if (playerCount) {
                    ping.setPlayersOnline(ping.getPlayersOnline() + plugin.getPlayerBank().getFakePlayerCount());
                }
                pings.write(0, ping);
            }
        } else if (e.getPacketType() == (Protocol.usesModernPlayerInfo()
                ? PacketType.Play.Server.SPAWN_ENTITY
                : PacketType.Play.Server.NAMED_ENTITY_SPAWN)) {
            Tablist tablist = plugin.getTablistManager().getTablist(e.getPlayer());
            if (tablist == null) {
                return;
            }
            UUID uuid = packet.getUUIDs().read(0);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                tablist.getHandler().addPlayer(WrappedGameProfile.fromPlayer(player));
            }
        } else if (e.getPacketType() == PacketType.Play.Server.RESPAWN) {
            Tablist tablist = plugin.getTablistManager().getTablist(e.getPlayer());
            if (tablist == null) {
                return;
            }
            tablist.getHandler().addPlayer(WrappedGameProfile.fromPlayer(e.getPlayer()));
        } else if (e.getPacketType() == PacketType.Play.Server.PLAYER_INFO) {
            Tablist tablist = plugin.getTablistManager().getTablist(e.getPlayer());
            if (tablist == null) {
                return;
            }

            try {
                List<PlayerInfoData> list = Protocol.readPlayerInfoEntries(packet);
                if (Protocol.usesModernPlayerInfo()) {
                    ModernPlayerInfoListener.handle(e, packet, tablist, list);
                } else {
                    handleLegacyPlayerInfo(e, packet, tablist, list);
                }
            } catch (FieldAccessException | IllegalStateException x) {
                RosaLogger.getInstance().log(
                        Level.WARNING,
                        "Unable to read a PLAYER_INFO packet",
                        x);
            }
        }
    }

    private void handleLegacyPlayerInfo(PacketEvent event, PacketContainer packet,
                                        Tablist tablist, List<PlayerInfoData> entries) {
        EnumWrappers.PlayerInfoAction action = Protocol.readLegacyPlayerInfoAction(packet);
        if (action == EnumWrappers.PlayerInfoAction.ADD_PLAYER) {
            for (PlayerInfoData data : entries) {
                WrappedGameProfile profile = data.getProfile();
                if (profile != null) {
                    schedulePlayerRemoval(tablist, profile.getUUID(), event.getPlayer());
                }
            }
        } else if ((action == EnumWrappers.PlayerInfoAction.UPDATE_LATENCY
                || action == EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME)
                && !Protocol.METAPACKETS.remove(packet.getHandle())) {
            event.setCancelled(true);
        }
    }

    private void schedulePlayerRemoval(Tablist tablist, UUID profileId, Player viewer) {
        if (profileId == null || UUIDSet.getSet().contains(tablist, profileId, viewer)) {
            return;
        }
        tablist.getHandler().removePlayer(profileId);
    }

    public void undoRemove(UUID uuid) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Tablist tablist = plugin.getTablistManager().getTablist(p);
            if (tablist != null)
                tablist.getHandler().removeQueue(uuid);
        }
    }
}
