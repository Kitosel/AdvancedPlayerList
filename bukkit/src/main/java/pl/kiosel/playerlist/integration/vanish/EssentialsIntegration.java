package pl.kiosel.playerlist.integration.vanish;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.kiosel.playerlist.integration.VanishIntegration;

public class EssentialsIntegration implements VanishIntegration {

	@Override
	public boolean isVanished(Player player) {
		Essentials ess = getEssentials();

		if (ess == null) {
			player.sendMessage("EssentialsX has been not installed or enabled properly!");
			return false;
		}

		User user = ess.getUser(player);

		if (user != null) {
			return user.isHidden();
		}
		return false;
	}

	private Essentials getEssentials() {
		return (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
	}
}