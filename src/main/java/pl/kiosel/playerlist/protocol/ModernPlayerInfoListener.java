package pl.kiosel.playerlist.protocol;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import org.bukkit.entity.Player;
import pl.kiosel.playerlist.internal.UUIDSet;
import pl.kiosel.playerlist.tablist.Tablist;

import java.util.List;
import java.util.Set;
import java.util.UUID;

final class ModernPlayerInfoListener {

    private ModernPlayerInfoListener() {
    }

    static void handle(PacketEvent event, PacketContainer packet, Tablist tablist, List<PlayerInfoData> entries) {
        Set<EnumWrappers.PlayerInfoAction> actions = Protocol.readModernPlayerInfoActions(packet);

        if (actions.contains(EnumWrappers.PlayerInfoAction.ADD_PLAYER)) {
            for (PlayerInfoData data : entries) {
                schedulePlayerRemoval(tablist, data.getProfileId(), event.getPlayer());
            }
        } else if (actions.contains(EnumWrappers.PlayerInfoAction.UPDATE_LISTED)) {
            for (PlayerInfoData data : entries) {
                UUID profileId = data.getProfileId();
                if (profileId != null
                        && !UUIDSet.getSet().contains(tablist, profileId, event.getPlayer())) {
                    tablist.getHandler().removeQueue(profileId);
                }
            }
        } else if ((actions.contains(EnumWrappers.PlayerInfoAction.UPDATE_LATENCY)
                || actions.contains(EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME))
                && !Protocol.METAPACKETS.remove(packet.getHandle())) {
            event.setCancelled(true);
        }
    }

    private static void schedulePlayerRemoval(Tablist tablist, UUID profileId, Player viewer) {
        if (profileId == null || UUIDSet.getSet().contains(tablist, profileId, viewer)) {
            return;
        }
        tablist.getHandler().removePlayer(profileId);
    }
}
