package pl.kiosel.playerlist.placeholder.complex;

import org.bukkit.configuration.ConfigurationSection;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.bridge.BridgeClient;
import pl.kiosel.playerlist.bridge.BridgeMessages;
import pl.kiosel.playerlist.placeholder.ExtraData;

import java.util.Collections;

/**
 * Legacy class name retained for existing BUNGEECORD_PLAYER_LIST configurations.
 */
public final class BungeePlayerListComplex extends BukkitListComplex<BridgeMessages.PlayerData> {

    public BungeePlayerListComplex(ConfigurationSection section) {
        super(section, BridgeMessages.PlayerData.class, ExtraData.DATA_PLAYER, () -> {
            BridgeClient bridge = AdvancedPlayerList.getInstance().getBridgeClient();
            return bridge == null ? Collections.emptyList() : bridge.getPlayers();
        });
    }
}
