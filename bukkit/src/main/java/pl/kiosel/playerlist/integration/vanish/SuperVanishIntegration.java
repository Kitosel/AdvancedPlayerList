package pl.kiosel.playerlist.integration.vanish;

import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import pl.kiosel.playerlist.integration.VanishIntegration;

public class SuperVanishIntegration implements VanishIntegration {

	@Override
	public boolean isVanished(Player player) {
		for (MetadataValue meta : player.getMetadata("vanished")) {
			if (meta.asBoolean()) return true;
		}
		return false;
	}
}
