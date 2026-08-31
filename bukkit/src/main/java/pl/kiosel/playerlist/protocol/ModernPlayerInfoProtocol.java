package pl.kiosel.playerlist.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import org.bukkit.entity.Player;
import pl.kiosel.rosacore.version.Version;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class ModernPlayerInfoProtocol {

    private ModernPlayerInfoProtocol() {
    }

    static void writeUpdate(PacketContainer packet, EnumWrappers.PlayerInfoAction action, List<PlayerInfoData> entries) {
        packet.getPlayerInfoActions().write(0, actionsFor(action));

        StructureModifier<List<PlayerInfoData>> entryModifier = packet.getPlayerInfoDataLists();
        if (entryModifier.size() == 0) {
            throw new IllegalStateException("ProtocolLib did not expose PLAYER_INFO entry fields");
        }

        entryModifier.write(entryModifier.size() - 1, entries);
    }

    static List<PlayerInfoData> readEntries(PacketContainer packet) {
        StructureModifier<List<PlayerInfoData>> entries = packet.getPlayerInfoDataLists();
        if (entries.size() == 0) {
            return Collections.emptyList();
        }
        List<PlayerInfoData> result = entries.read(entries.size() - 1);
        return result == null ? Collections.emptyList() : result;
    }

    static void writeEntries(PacketContainer packet, List<PlayerInfoData> entries) {
        StructureModifier<List<PlayerInfoData>> modifier = packet.getPlayerInfoDataLists();
        if (modifier.size() == 0) {
            throw new IllegalStateException("ProtocolLib did not expose PLAYER_INFO entry fields");
        }
        modifier.write(modifier.size() - 1, entries);
    }

    static PlayerInfoData withListed(PlayerInfoData data, boolean listed) {
        return new PlayerInfoData(
                data.getProfileId(),
                data.getLatency(),
                listed,
                data.getGameMode(),
                data.getProfile(),
                data.getDisplayName(),
                data.getRemoteChatSessionData());
    }

    static Set<EnumWrappers.PlayerInfoAction> readActions(PacketContainer packet) {
        Set<EnumWrappers.PlayerInfoAction> actions = packet.getPlayerInfoActions().readSafely(0);
        return actions == null ? Collections.emptySet() : actions;
    }

    static EnumSet<EnumWrappers.PlayerInfoAction> withoutTabMetadata(Set<EnumWrappers.PlayerInfoAction> actions) {
        EnumSet<EnumWrappers.PlayerInfoAction> filtered = EnumSet.noneOf(EnumWrappers.PlayerInfoAction.class);
        if (actions != null) {
            filtered.addAll(actions);
        }
        filtered.remove(EnumWrappers.PlayerInfoAction.UPDATE_LATENCY);
        filtered.remove(EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME);
        return filtered;
    }

    static void writeActions(PacketContainer packet, Set<EnumWrappers.PlayerInfoAction> actions) {
        EnumSet<EnumWrappers.PlayerInfoAction> compatible = EnumSet.noneOf(EnumWrappers.PlayerInfoAction.class);
        if (actions != null) {
            compatible.addAll(actions);
        }
        packet.getPlayerInfoActions().write(0, compatible);
    }

    static void sendRemove(Player target, Collection<UUID> profileIds) {
        PacketContainer packet = Protocol.getManager().createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
        packet.getUUIDLists().write(0, new ArrayList<>(profileIds));
        Protocol.sendPacket(target, packet);
    }

    private static EnumSet<EnumWrappers.PlayerInfoAction> actionsFor(EnumWrappers.PlayerInfoAction action) {
        EnumSet<EnumWrappers.PlayerInfoAction> actions = EnumSet.of(action);
        if (action != EnumWrappers.PlayerInfoAction.ADD_PLAYER) {
            return actions;
        }

        actions.add(EnumWrappers.PlayerInfoAction.UPDATE_LISTED);
        actions.add(EnumWrappers.PlayerInfoAction.UPDATE_GAME_MODE);
        actions.add(EnumWrappers.PlayerInfoAction.UPDATE_LATENCY);
        actions.add(EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME);

        if (Version.isServerVersionAtLeast(Version.V1_21_2)) {
            addOptionalAction(actions, "UPDATE_LIST_ORDER");
        }
        if (Version.isServerVersionAtLeast(Version.V1_21_4)) {
            addOptionalAction(actions, "UPDATE_HAT");
        }
        return actions;
    }

    private static void addOptionalAction(Set<EnumWrappers.PlayerInfoAction> actions, String name) {
        try {
            actions.add(Enum.valueOf(EnumWrappers.PlayerInfoAction.class, name));
        } catch (IllegalArgumentException ignored) {
        }
    }
}
