package pl.kiosel.playerlist.listener;

import pl.kiosel.playerlist.model.Ticker;
import pl.kiosel.playerlist.tablist.Tablist;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.kiosel.playerlist.AdvancedPlayerList;

import java.util.List;

public class PlayerListener implements Listener {

    private final AdvancedPlayerList plugin;

    public PlayerListener(AdvancedPlayerList plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        List<String> blacklist = plugin.getConfigFile().getStringList("worldBlacklist");
        Player player = event.getPlayer();

        Ticker.delay(() -> {
            if (!plugin.getTablistManager().isTablistEnabled() || blacklist.contains(player.getWorld().getName())) {
                plugin.getTablistManager().disableTablist(player);
            } else {
                plugin.getTablistManager().enableTablist(player);
            }
        }, 0L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (plugin.getOfflinePlayerDatabase() != null && plugin.getConfigFile().getBoolean("offline-players.enable")) {
            plugin.getOfflinePlayerDatabase().add(event.getPlayer());
        }
        Ticker.delay(() -> plugin.getTablistManager().disableTablist(event.getPlayer()), 0L);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        Ticker.delay(() -> {
            if (!plugin.getTablistManager().isTablistEnabled()) {
                plugin.getTablistManager().disableTablist(player);
                return;
            }
            List<String> blacklist = plugin.getConfigFile().getStringList("worldBlacklist");

            Tablist tablist = plugin.getTablistManager().getTablist(player);
            if (tablist == null || tablist.getPlugin() == plugin) {
                if (blacklist.contains(player.getWorld().getName())) {
                    plugin.getTablistManager().disableTablist(player);
                } else if (tablist == null) {
                    plugin.getTablistManager().enableTablist(player);
                } else {
                    tablist.setLayout(plugin.getLayouts().getOrDefault(player.getWorld(), plugin.globalLayout));
                    tablist.setDisplay(plugin.getDisplays().getOrDefault(player.getWorld(), plugin.globalDisplay));
                }
            }
        }, 0L);
    }
}
