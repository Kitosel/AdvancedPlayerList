package pl.kiosel.playerlist.listener;

import pl.kiosel.playerlist.config.Lang;
import pl.kiosel.playerlist.config.Settings;
import pl.kiosel.playerlist.model.Ticker;
import pl.kiosel.playerlist.tablist.Tablist;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.rosacore.compatibility.ZSound;
import pl.kiosel.rosacore.listener.RosaListener;

import java.util.List;

public class PlayerListener extends RosaListener {

    private final AdvancedPlayerList plugin;

    public PlayerListener(AdvancedPlayerList plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        List<String> blacklist = plugin.getSettings().get(Settings.WORLD_BLACKLIST);
        Player player = event.getPlayer();

        Ticker.delay(player, () -> {
            if (!plugin.getTablistManager().isTablistEnabled() || blacklist.contains(player.getWorld().getName())) {
                plugin.getTablistManager().disableTablist(player);
            } else {
                plugin.getTablistManager().enableTablist(player);
            }
            if (plugin.getBridgeClient() != null) {
                plugin.getBridgeClient().requestUpdate();
            }
        }, 0L);

        if (!plugin.getSettings().get(Settings.UPDATE_CHECKER)) return;
        Ticker.delay(player, () -> {
            String newVersion = plugin.getNewPluginVersion();
            if (newVersion == null || plugin.getDescription().getVersion().equals(newVersion)) return;

            if (!player.isOp() && !player.hasPermission("advancedplayerlist.manage.plugin")) return;

            ZSound.ENTITY_PLAYER_LEVELUP.play(player, 0.5f, 2f);
            getMessage().message(Lang.UPDATE).with("version", newVersion).sendPrefixed(player);
        }, 5*20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (plugin.getOfflinePlayerDatabase() != null && plugin.getSettings().get(Settings.OFFLINE_PLAYERS_ENABLE)) {
            plugin.getOfflinePlayerDatabase().add(event.getPlayer());
        }
        plugin.getTablistManager().disableTablist(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        Ticker.delay(player, () -> {
            if (!plugin.getTablistManager().isTablistEnabled()) {
                plugin.getTablistManager().disableTablist(player);
                return;
            }
            List<String> blacklist = plugin.getSettings().get(Settings.WORLD_BLACKLIST);

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
