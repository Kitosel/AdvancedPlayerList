package pl.kiosel.playerlist.integration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import pl.kiosel.playerlist.integration.vanish.EssentialsIntegration;
import pl.kiosel.playerlist.integration.vanish.SuperVanishIntegration;

import java.util.ArrayList;
import java.util.List;

public class VanishIntegrationManager {

	private final List<VanishIntegration> activeIntegrations = new ArrayList<>();

	protected void checkVanishPlugins() {
		PluginManager pm = Bukkit.getPluginManager();

		if (pm.isPluginEnabled("SuperVanish") || pm.isPluginEnabled("PremiumVanish")) {
			activeIntegrations.add(new SuperVanishIntegration());
		}

		if (pm.isPluginEnabled("Essentials")) {
			activeIntegrations.add(new EssentialsIntegration());
		}
	}

	public boolean isVanished(Player player) {
		if (activeIntegrations.isEmpty()) {
			return false;
		}

		for (VanishIntegration integration : activeIntegrations) {
			if (integration.isVanished(player)) {
				return true;
			}
		}
		return false;
	}
}
