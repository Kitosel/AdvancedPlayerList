package pl.kiosel.playerlist.tablist;

import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.config.Settings;
import pl.kiosel.playerlist.model.Ticker;
import pl.kiosel.playerlist.placeholder.ExtraData;
import pl.kiosel.playerlist.placeholder.PlaceholderManager;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VanillaPlayerListFormatter {

    private final AdvancedPlayerList plugin;
    private final Map<UUID, String> originalNames = new ConcurrentHashMap<>();
    private final Set<UUID> originallyUnset = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> appliedNames = new ConcurrentHashMap<>();

    private volatile boolean enabled;
    private volatile int refreshTicks = 20;
    private volatile String format = "%player_name%";

    public VanillaPlayerListFormatter(AdvancedPlayerList plugin) {
        this.plugin = plugin;
    }

    public void configure(ConfigurationSection section) {
        if (section == null) {
            enabled = false;
            format = "%player_name%";
            refreshTicks = 20;
            return;
        }

        enabled = section.getBoolean("format-no-columns", false);
        format = section.getString("format", "%player_name%");
        if (format == null) {
            format = "%player_name%";
        }
        refreshTicks = Math.max(1, section.getInt("refreshTicks", 20));
    }

    public void update(Player player, boolean force) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (!isActive()) {
            restore(player);
            return;
        }
        if (!force && Ticker.tickTime() % refreshTicks != 0) {
            return;
        }

        UUID uniqueId = player.getUniqueId();
        rememberOriginalName(player);
        String next = PlaceholderManager.replace(format, new ExtraData()
                .put(ExtraData.DATA_PLAYER, player)
                .put(ExtraData.DATA_VIEWER, player));
        if (next == null) {
            next = "";
        }

        String current = player.getPlayerListName();
        if (!next.equals(current)) {
            player.setPlayerListName(next);
        }
        appliedNames.put(uniqueId, next);
    }

    public void restore(Player player) {
        if (player == null) {
            return;
        }
        UUID uniqueId = player.getUniqueId();
        String applied = appliedNames.remove(uniqueId);
        String original = originalNames.remove(uniqueId);
        boolean unset = originallyUnset.remove(uniqueId);

        if (applied == null || !Objects.equals(player.getPlayerListName(), applied)) {
            return;
        }
        player.setPlayerListName(unset ? null : original);
    }

    private boolean isActive() {
        return enabled && !plugin.getSettings().get(Settings.TABLIST_WITH_COLUMNS);
    }

    private void rememberOriginalName(Player player) {
        UUID uniqueId = player.getUniqueId();
        if (originalNames.containsKey(uniqueId) || originallyUnset.contains(uniqueId)) {
            return;
        }
        String original = player.getPlayerListName();
        if (original == null) {
            originallyUnset.add(uniqueId);
        } else {
            originalNames.put(uniqueId, original);
        }
    }
}
