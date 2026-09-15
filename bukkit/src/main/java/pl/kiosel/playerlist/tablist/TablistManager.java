package pl.kiosel.playerlist.tablist;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.event.TablistShowEvent;
import pl.kiosel.playerlist.model.Ticker;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TablistManager {

	private final AdvancedPlayerList plugin;
	private static final String TABLIST_METADATA = "advancedplayerlist";
	private final Map<UUID, Tablist> tablists = new ConcurrentHashMap<>();
	@Getter
	private boolean tablistEnabled;

	public TablistManager(AdvancedPlayerList plugin) {
		this.plugin = plugin;
	}

	public void setTablist(Player player, Tablist tablist) {
		Tablist current = getTablist(player);
		if (current == tablist) {
			return;
		}
		if (current != null) {
			current.stop();
		}
		if (tablist == null) {
			tablists.remove(player.getUniqueId());
			player.removeMetadata(TABLIST_METADATA, plugin);
			return;
		}
		tablists.put(player.getUniqueId(), tablist);
		player.setMetadata(TABLIST_METADATA, new FixedMetadataValue(plugin, tablist));
	}

	public void setTablistEnabled(boolean enabled) {
		tablistEnabled = enabled;
		for (Player player : plugin.getServer().getOnlinePlayers()) {
			Runnable update = () -> {
				if (enabled) {
					enableTablist(player);
				} else {
					disableTablist(player);
				}
			};
			if (plugin.getRosaScheduler().isFolia()) {
				Ticker.delay(player, update, 0L);
			} else {
				update.run();
			}
		}
	}

	public boolean hasTablistEnabled(Player player) {
		return getTablist(player) != null && getTablist(player).isEnabled();
	}

	public void disableTablist(Player player) {
		Tablist tablist = tablists.remove(player.getUniqueId());
		player.removeMetadata(TABLIST_METADATA, plugin);
		if (tablist != null) {
			tablist.stop();
		}
		if (plugin.getProtocolListener() != null) {
			plugin.getProtocolListener().disable(player);
		}
	}

	public void enableTablist(Player player) {
		if (getTablist(player) != null) {
			return;
		}

		Tablist tablist = new Tablist(player, plugin);
		TablistShowEvent event = new TablistShowEvent(tablist);
		plugin.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return;
		}

		setTablist(player, tablist);
		if (plugin.getProtocolListener() != null) {
			plugin.getProtocolListener().enable(player);
		}
		tablist.start();
	}

	public Tablist getTablist(Player player) {
		return player == null ? null : tablists.get(player.getUniqueId());
	}
}
