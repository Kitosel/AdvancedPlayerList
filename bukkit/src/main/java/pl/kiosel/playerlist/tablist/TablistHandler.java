package pl.kiosel.playerlist.tablist;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import pl.kiosel.playerlist.internal.LineData;
import pl.kiosel.playerlist.internal.UUIDSet;
import pl.kiosel.playerlist.model.Ticker;
import pl.kiosel.playerlist.protocol.Protocol;
import pl.kiosel.rosacore.nms.api.tablist.TabList;
import pl.kiosel.rosacore.nms.api.tablist.TabListCell;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    public void addPlayer(Player player) {
        if (player == null || removeQueue.containsKey(player.getUniqueId())) {
            return;
        }
        TabList nativeTabList = tablist.getNativeTabList();
        if (nativeTabList != null) {
            nativeTabList.showRealPlayer(player);
            Player viewer = tablist.getPlayer();
            if (Protocol.usesModernPlayerInfo() && viewer.getUniqueId().equals(player.getUniqueId())) {
                nativeTabList.hideRealPlayer(viewer);
            } else if (!UUIDSet.getSet().contains(tablist, player.getUniqueId(), viewer)) {
                removePlayer(player.getUniqueId());
            }
        }
    }

    public void addPlayers() {
        Player viewer = tablist.getPlayer();
        TabList nativeTabList = tablist.getNativeTabList();
        if (nativeTabList == null) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (viewer == player || viewer.canSee(player)) {
                nativeTabList.showRealPlayer(player);
            }
        }
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
        TabList nativeTabList = tablist.getNativeTabList();
        if (nativeTabList != null) {
            for (UUID profileId : expired) nativeTabList.hideRealPlayer(profileId);
        }
    }

    private void updateLayout(TablistLayout layout) {
        int lineCount = Math.min(Math.min(layout.getSize(), 80), tablist.lines.length);
        int linesPerColumn = Math.max(1, TablistLayout.lines(layout.getSize()));
        boolean[] hideEmptyColumn = calculateConfiguredEmptyColumns(layout, lineCount, linesPerColumn);
        boolean[] contentEmptyColumn = calculateContentEmptyColumns(layout, lineCount, linesPerColumn);

        TabList nativeTabList = tablist.getNativeTabList();
        if (nativeTabList == null) {
            return;
        }
        boolean changed = nativeTabList.getActiveCellCount() != lineCount;
        nativeTabList.setActiveCellCount(lineCount);

        for (int index = 0; index < lineCount; index++) {
            LineData data = layout.getLine(index);
            TablistLine line = tablist.getLine(index);
            applyLineData(line, data);

            int column = Math.min(hideEmptyColumn.length - 1, index / linesPerColumn);
            if (!line.getUnsafe().dirty() && contentEmptyColumn[column] && hideEmptyColumn[column]) {
                line.hide();
            } else {
                line.show();
            }

            TabListCell nextCell = line.getUnsafe().toCell(line.isShown());
            if (!Objects.equals(nativeTabList.getCell(index), nextCell)) {
                nativeTabList.setCell(index, nextCell);
                changed = true;
            }
            line.resetUpdateFlags();
        }
        if (changed) {
            nativeTabList.send();
        }
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

}
