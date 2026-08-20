package pl.kiosel.playerlist.tablist;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import pl.kiosel.playerlist.internal.LineData;
import pl.kiosel.playerlist.model.Ticker;
import pl.kiosel.playerlist.protocol.Protocol;
import pl.kiosel.playerlist.protocol.ProtocolPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TablistHandler {

    private static final int PLAYER_REMOVAL_DELAY_TICKS = 5;

    private final Map<UUID, Integer> removeQueue = new ConcurrentHashMap<>();
    private final Tablist tablist;
    private volatile boolean quickUpdate;

    TablistHandler(Tablist tablist) {
        this.tablist = tablist;
    }

    public void clear() {
        removeQueue.clear();
        quickUpdate = false;
    }

    public void addPlayer(WrappedGameProfile profile) {
        if (profile == null || removeQueue.containsKey(profile.getUUID())) {
            return;
        }
        Protocol.infoRealPlayer(tablist.getPlayer(),
                EnumWrappers.PlayerInfoAction.ADD_PLAYER, Collections.singletonList(profile));
    }

    public void addPlayers() {
        Player viewer = tablist.getPlayer();
        List<Player> visiblePlayers = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (viewer == player || viewer.canSee(player)) {
                visiblePlayers.add(player);
            }
        }
        Protocol.infoBukkitPlayer(viewer, EnumWrappers.PlayerInfoAction.ADD_PLAYER, visiblePlayers);
    }

    public void removePlayer(UUID uuid) {
        if (uuid != null) {
            removeQueue.putIfAbsent(uuid, PLAYER_REMOVAL_DELAY_TICKS);
        }
    }

    public void removeQueue(UUID uuid) {
        if (uuid != null) {
            removeQueue.remove(uuid);
        }
    }

    public void quickUpdate() {
        quickUpdate = true;
    }

    public void tick() {
        processRemoveQueue();
        if (!quickUpdate && !Ticker.optimize()) {
            return;
        }
        quickUpdate = false;

        TablistLayout sourceLayout = tablist.getLayout();
        if (sourceLayout == null) {
            return;
        }

        TablistLayout layout = sourceLayout.clone();
        if (tablist.getLayoutHandler() != null) {
            tablist.getLayoutHandler().handleLayout(tablist.getPlayer(), layout);
        }
        updateLayout(layout);
    }

    private void processRemoveQueue() {
        if (removeQueue.isEmpty()) {
            return;
        }

        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : removeQueue.entrySet()) {
            UUID uuid = entry.getKey();
            int remaining = entry.getValue();
            if (remaining <= 1) {
                if (removeQueue.remove(uuid, remaining)) {
                    expired.add(uuid);
                }
            } else {
                removeQueue.replace(uuid, remaining, remaining - 1);
            }
        }
        Protocol.removeRealPlayerIds(tablist.getPlayer(), expired);
    }

    private void updateLayout(TablistLayout layout) {
        int lineCount = Math.min(Math.min(layout.getSize(), 80), tablist.lines.length);
        int linesPerColumn = Math.max(1, TablistLayout.lines(layout.getSize()));
        boolean[] hideEmptyColumn = calculateConfiguredEmptyColumns(layout, lineCount, linesPerColumn);
        boolean[] contentEmptyColumn = calculateContentEmptyColumns(layout, lineCount, linesPerColumn);

        Set<ProtocolPlayer> addUpdates = new LinkedHashSet<>();
        Set<ProtocolPlayer> removeUpdates = new LinkedHashSet<>();
        List<ProtocolPlayer> nameUpdates = new ArrayList<>();
        List<ProtocolPlayer> latencyUpdates = new ArrayList<>();
        List<ProtocolPlayer> gameModeUpdates = new ArrayList<>();

        for (int index = 0; index < lineCount; index++) {
            LineData data = layout.getLine(index);
            TablistLine line = tablist.getLine(index);
            applyLineData(line, data);

            int column = Math.min(hideEmptyColumn.length - 1, index / linesPerColumn);
            if (contentEmptyColumn[column] && hideEmptyColumn[column]) {
                line.hide();
            } else {
                line.show();
            }

            collectUpdates(line, addUpdates, removeUpdates,
                    nameUpdates, latencyUpdates, gameModeUpdates);
            line.resetUpdateFlags();
        }

        nameUpdates.removeAll(addUpdates);
        latencyUpdates.removeAll(addUpdates);
        gameModeUpdates.removeAll(addUpdates);

        Player viewer = tablist.getPlayer();
        Protocol.removePlayers(viewer, removeUpdates);
        Protocol.infoPlayer(viewer, EnumWrappers.PlayerInfoAction.ADD_PLAYER, addUpdates);
        Protocol.infoPlayer(viewer, EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME, nameUpdates);
        Protocol.infoPlayer(viewer, EnumWrappers.PlayerInfoAction.UPDATE_LATENCY, latencyUpdates);
        Protocol.infoPlayer(viewer, EnumWrappers.PlayerInfoAction.UPDATE_GAME_MODE, gameModeUpdates);
    }

    private boolean[] calculateConfiguredEmptyColumns(TablistLayout layout, int lineCount, int linesPerColumn) {
        boolean[] empty = {true, true, true, true};
        for (int index = 0; index < lineCount; index++) {
            if (!layout.getLine(index).shouldHideEmpty()) {
                empty[Math.min(empty.length - 1, index / linesPerColumn)] = false;
            }
        }
        return empty;
    }

    private boolean[] calculateContentEmptyColumns(TablistLayout layout, int lineCount, int linesPerColumn) {
        boolean[] empty = {true, true, true, true};
        for (int index = 0; index < lineCount; index++) {
            String text = layout.getLine(index).getText();
            if (text != null && !ChatColor.stripColor(text).trim().isEmpty()) {
                empty[Math.min(empty.length - 1, index / linesPerColumn)] = false;
            }
        }
        return empty;
    }

    private void applyLineData(TablistLine line, LineData data) {
        line.setText(data.getText());
        line.setPing(data.getPing());
        line.setSkin(data.getSkin());
        line.setGameMode(data.getGameMode());
    }

    private void collectUpdates(TablistLine line,
                                Set<ProtocolPlayer> addUpdates,
                                Set<ProtocolPlayer> removeUpdates,
                                Collection<ProtocolPlayer> nameUpdates,
                                Collection<ProtocolPlayer> latencyUpdates,
                                Collection<ProtocolPlayer> gameModeUpdates) {
        ProtocolPlayer player = line.getUnsafe();

        if (line.updateRemove) {
            removeUpdates.add(player);
        }
        if (line.updateAdd) {
            addUpdates.add(player);
        }
        if (line.updateSkin && line.isShown() && !tablist.isPlayerCracked()) {
            if (!line.updateAdd) {
                removeUpdates.add(player);
            }
            addUpdates.add(player);
        }

        if (!line.isShown() || addUpdates.contains(player)) {
            return;
        }
        if (line.updateName) {
            nameUpdates.add(player);
        }
        if (line.updatePing) {
            latencyUpdates.add(player);
        }
        if (line.updateGameMode) {
            gameModeUpdates.add(player);
        }
    }
}
