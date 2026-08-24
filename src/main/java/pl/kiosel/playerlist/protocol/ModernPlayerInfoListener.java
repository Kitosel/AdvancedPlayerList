package pl.kiosel.playerlist.protocol;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import org.bukkit.entity.Player;
import pl.kiosel.playerlist.internal.UUIDSet;
import pl.kiosel.playerlist.tablist.Tablist;

import java.util.List;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

final class ModernPlayerInfoListener {

    private ModernPlayerInfoListener() {
    }

    static void handle(PacketEvent event, PacketContainer packet, Tablist tablist, List<PlayerInfoData> entries) {
        Set<EnumWrappers.PlayerInfoAction> actions = Protocol.readModernPlayerInfoActions(packet);

        if (actions.contains(EnumWrappers.PlayerInfoAction.ADD_PLAYER)) {
            List<PlayerInfoData> rewritten = null;
            for (int index = 0; index < entries.size(); index++) {
                PlayerInfoData data = entries.get(index);
                if (event.getPlayer().getUniqueId().equals(data.getProfileId()) && data.isListed()) {
                    if (rewritten == null)
                        rewritten = new ArrayList<>(entries);
                    rewritten.set(index, ModernPlayerInfoProtocol.withListed(data, false));
                }
                schedulePlayerRemoval(tablist, data.getProfileId(), event.getPlayer());
            }
            if (rewritten != null)
                ModernPlayerInfoProtocol.writeEntries(packet, rewritten);
            return;
        }

        if (actions.contains(EnumWrappers.PlayerInfoAction.UPDATE_LISTED)) {
            List<PlayerInfoData> rewritten = null;
            for (int index = 0; index < entries.size(); index++) {
                PlayerInfoData data = entries.get(index);
                UUID profileId = data.getProfileId();
                if (event.getPlayer().getUniqueId().equals(profileId) && data.isListed()) {
                    if (rewritten == null)
                        rewritten = new ArrayList<>(entries);
                    rewritten.set(index, ModernPlayerInfoProtocol.withListed(data, false));
                }
                if (profileId != null && !UUIDSet.getSet().contains(tablist, profileId, event.getPlayer())) {
                    tablist.getHandler().removeQueue(profileId);
                }
            }
            if (rewritten != null) {
                ModernPlayerInfoProtocol.writeEntries(packet, rewritten);
            }
        }

        if (!Protocol.METAPACKETS.remove(packet.getHandle()) &&
                (actions.contains(EnumWrappers.PlayerInfoAction.UPDATE_LATENCY) || actions.contains(EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME))) {
            EnumSet<EnumWrappers.PlayerInfoAction> filtered = ModernPlayerInfoProtocol.withoutTabMetadata(actions);
            if (filtered.isEmpty()) {
                event.setCancelled(true);
            } else {
                ModernPlayerInfoProtocol.writeActions(packet, filtered);
            }
        }
    }

    private static void schedulePlayerRemoval(Tablist tablist, UUID profileId, Player viewer) {
        if (profileId == null || UUIDSet.getSet().contains(tablist, profileId, viewer))
            return;
        tablist.getHandler().removePlayer(profileId);
    }
}
