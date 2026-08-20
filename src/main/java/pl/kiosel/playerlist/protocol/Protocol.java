package pl.kiosel.playerlist.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import pl.kiosel.playerlist.util.RuntimeCompatibility;
import pl.kiosel.playerlist.util.Utils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("deprecation")
public final class Protocol {

    public static final WrappedChatComponent nullChat = WrappedChatComponent.fromJson("{\"text\":\"\"}");

    static final Set<Object> METAPACKETS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static final Method PLAYER_GET_PING = findPlayerPingMethod();

    private Protocol() {
    }

    public static boolean usesModernPlayerInfo() {
        return RuntimeCompatibility.usesModernPlayerInfo();
    }

    public static EnumWrappers.NativeGameMode convert(GameMode bukkit) {
        if (bukkit != null) {
            switch (bukkit) {
                case ADVENTURE:
                    return EnumWrappers.NativeGameMode.ADVENTURE;
                case CREATIVE:
                    return EnumWrappers.NativeGameMode.CREATIVE;
                case SPECTATOR:
                    return EnumWrappers.NativeGameMode.SPECTATOR;
                case SURVIVAL:
                    return EnumWrappers.NativeGameMode.SURVIVAL;
            }
        }
        return EnumWrappers.NativeGameMode.NOT_SET;
    }

    public static void headerFooter(Player target, String header, String footer) {
        if (target == null || !target.isOnline()) {
            return;
        }
        PacketContainer packet = getManager().createPacket(PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER);
        packet.getChatComponents().write(0,
                header == null ? nullChat : WrappedChatComponent.fromJson(Utils.fromLegacy(header)));
        packet.getChatComponents().write(1,
                footer == null ? nullChat : WrappedChatComponent.fromJson(Utils.fromLegacy(footer)));
        sendPacket(target, packet);
    }

    public static void infoBukkitPlayer(Player target, EnumWrappers.PlayerInfoAction action,
                                        Collection<Player> players) {
        if (cannotSend(target, players)) {
            return;
        }

        List<PlayerInfoData> entries = new ArrayList<>(players.size());
        for (Player player : players) {
            entries.add(createPlayerInfoData(
                    WrappedGameProfile.fromPlayer(player),
                    getPlayerPing(player),
                    convert(player.getGameMode()),
                    WrappedChatComponent.fromText(player.getPlayerListName())));
        }
        sendPlayerInfoUpdate(target, action, entries, false);
    }

    public static void infoPlayer(Player target, EnumWrappers.PlayerInfoAction action,
                                  Collection<ProtocolPlayer> players) {
        if (cannotSend(target, players)) {
            return;
        }

        List<PlayerInfoData> entries = new ArrayList<>(players.size());
        for (ProtocolPlayer player : players) {
            entries.add(createProtocolPlayerInfoData(player));
        }
        sendPlayerInfoUpdate(target, action, entries, true);
    }

    public static void infoRealPlayer(Player target, EnumWrappers.PlayerInfoAction action,
                                      Collection<WrappedGameProfile> players) {
        if (cannotSend(target, players)) {
            return;
        }

        List<PlayerInfoData> entries = new ArrayList<>(players.size());
        for (WrappedGameProfile player : players) {
            entries.add(createPlayerInfoData(
                    player,
                    0,
                    EnumWrappers.NativeGameMode.SURVIVAL,
                    nullChat));
        }
        sendPlayerInfoUpdate(target, action, entries, false);
    }

    public static void removeBukkitPlayers(Player target, Collection<Player> players) {
        if (cannotSend(target, players)) {
            return;
        }

        List<UUID> profileIds = new ArrayList<>(players.size());
        for (Player player : players) {
            profileIds.add(player.getUniqueId());
        }
        if (usesModernPlayerInfo()) {
            ModernPlayerInfoProtocol.sendRemove(target, profileIds);
            return;
        }

        List<PlayerInfoData> legacyEntries = new ArrayList<>(players.size());
        for (Player player : players) {
            legacyEntries.add(createPlayerInfoData(
                    WrappedGameProfile.fromPlayer(player),
                    getPlayerPing(player),
                    convert(player.getGameMode()),
                    WrappedChatComponent.fromText(player.getPlayerListName())));
        }
        sendPlayerInfoRemove(target, profileIds, legacyEntries);
    }

    public static void removePlayers(Player target, Collection<ProtocolPlayer> players) {
        if (cannotSend(target, players)) {
            return;
        }

        List<UUID> profileIds = new ArrayList<>(players.size());
        for (ProtocolPlayer player : players) {
            profileIds.add(player.getUniqueId());
        }
        if (usesModernPlayerInfo()) {
            ModernPlayerInfoProtocol.sendRemove(target, profileIds);
            return;
        }

        List<PlayerInfoData> legacyEntries = new ArrayList<>(players.size());
        for (ProtocolPlayer player : players) {
            legacyEntries.add(createProtocolPlayerInfoData(player));
        }
        sendPlayerInfoRemove(target, profileIds, legacyEntries);
    }

    public static void removeRealPlayerIds(Player target, Collection<UUID> profileIds) {
        if (cannotSend(target, profileIds)) {
            return;
        }
        if (usesModernPlayerInfo()) {
            ModernPlayerInfoProtocol.sendRemove(target, profileIds);
            return;
        }

        List<PlayerInfoData> legacyEntries = new ArrayList<>(profileIds.size());
        for (UUID profileId : profileIds) {
            legacyEntries.add(createPlayerInfoData(
                    new WrappedGameProfile(profileId, ""),
                    0,
                    EnumWrappers.NativeGameMode.SURVIVAL,
                    nullChat));
        }
        sendPlayerInfoUpdate(
                target,
                EnumWrappers.PlayerInfoAction.REMOVE_PLAYER,
                legacyEntries,
                false);
    }

    static List<PlayerInfoData> readPlayerInfoEntries(PacketContainer packet) {
        if (usesModernPlayerInfo()) {
            return ModernPlayerInfoProtocol.readEntries(packet);
        }

        List<PlayerInfoData> entries = packet.getPlayerInfoDataLists().readSafely(0);
        return entries == null ? Collections.<PlayerInfoData>emptyList() : entries;
    }

    static Set<EnumWrappers.PlayerInfoAction> readModernPlayerInfoActions(PacketContainer packet) {
        return ModernPlayerInfoProtocol.readActions(packet);
    }

    static EnumWrappers.PlayerInfoAction readLegacyPlayerInfoAction(PacketContainer packet) {
        return packet.getPlayerInfoAction().readSafely(0);
    }

    private static PlayerInfoData createProtocolPlayerInfoData(ProtocolPlayer player) {
        WrappedChatComponent displayName = player.getDisplayName() == null
                ? nullChat
                : WrappedChatComponent.fromJson(Utils.fromLegacy(player.getDisplayName()));
        return createPlayerInfoData(
                player.getProfile(),
                player.getLatency(),
                convert(player.getGameMode()),
                displayName);
    }

    private static PlayerInfoData createPlayerInfoData(WrappedGameProfile profile, int latency,
                                                        EnumWrappers.NativeGameMode gameMode,
                                                        WrappedChatComponent displayName) {
        return new PlayerInfoData(profile, latency, gameMode, displayName);
    }

    private static void sendPlayerInfoUpdate(Player target, EnumWrappers.PlayerInfoAction action,
                                             List<PlayerInfoData> entries, boolean metadataPacket) {
        PacketContainer packet = getManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
        if (usesModernPlayerInfo()) {
            ModernPlayerInfoProtocol.writeUpdate(packet, action, entries);
        } else {
            packet.getPlayerInfoAction().write(0, action);
            packet.getPlayerInfoDataLists().write(0, entries);
        }

        if (metadataPacket && (action == EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME
                || action == EnumWrappers.PlayerInfoAction.UPDATE_LATENCY)) {
            METAPACKETS.add(packet.getHandle());
        }
        sendPacket(target, packet);
    }

    private static void sendPlayerInfoRemove(Player target, Collection<UUID> profileIds,
                                             List<PlayerInfoData> legacyEntries) {
        if (usesModernPlayerInfo()) {
            ModernPlayerInfoProtocol.sendRemove(target, profileIds);
            return;
        }

        sendPlayerInfoUpdate(
                target,
                EnumWrappers.PlayerInfoAction.REMOVE_PLAYER,
                legacyEntries,
                false);
    }

    private static Method findPlayerPingMethod() {
        try {
            return Player.class.getMethod("getPing");
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static int getPlayerPing(Player player) {
        if (PLAYER_GET_PING == null) {
            return 0;
        }
        try {
            return ((Number) PLAYER_GET_PING.invoke(player)).intValue();
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            return 0;
        }
    }

    private static boolean cannotSend(Player target, Collection<?> entries) {
        return target == null || !target.isOnline() || entries == null || entries.isEmpty();
    }

    public static ProtocolManager getManager() {
        return ProtocolLibrary.getProtocolManager();
    }

    public static void sendPacket(Player player, PacketContainer packet) {
        try {
            getManager().sendServerPacket(player, packet);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to send packet to " + player.getName(), exception);
        }
    }
}
