package pl.kiosel.playerlist.placeholder.simple;

import org.bukkit.entity.Player;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.placeholder.ExtraData;
import pl.kiosel.playerlist.placeholder.SimplePlaceholder;

public class EconomySimple implements SimplePlaceholder {

	private final AdvancedPlayerList plugin;

	public EconomySimple(AdvancedPlayerList plugin) {
		this.plugin = plugin;
	}

	@Override
	public String replace(String text, ExtraData data) {
		if (text == null) {
			return "";
		}

		Object dataPlayer = data.get(ExtraData.DATA_PLAYER);
		if (dataPlayer instanceof Player) {
			Player player = (Player) dataPlayer;
			text = replaceToken(text, "economy_balance", String.valueOf(plugin.getHookManager().getEconomy().getBalance(player)));
		}
		return text;
	}

	@Override
	public void onRegistered() {

	}

	@Override
	public void onUnregistered() {

	}
}
