package pl.kiosel.playerlist.integration;

import org.bukkit.entity.Player;
import pl.kiosel.playerlist.AdvancedPlayerList;

public class IntegrationManager {

	private final AdvancedPlayerList plugin;
	private VanishIntegrationManager vim;

	public IntegrationManager(AdvancedPlayerList plugin) {
		this.plugin = plugin;
	}

	public void enable() {
		vim = new VanishIntegrationManager();
		vim.checkVanishPlugins();
	}

	public boolean isVanished(Player player) {
		return vim.isVanished(player);
	}
}
