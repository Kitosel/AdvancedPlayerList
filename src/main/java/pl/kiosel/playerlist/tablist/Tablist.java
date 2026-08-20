package pl.kiosel.playerlist.tablist;

import java.util.*;

import lombok.Getter;
import lombok.Setter;
import pl.kiosel.playerlist.internal.Tickable;
import pl.kiosel.playerlist.placeholder.PlaceholderManager;
import pl.kiosel.playerlist.internal.UUIDSet;
import pl.kiosel.playerlist.event.TablistHeaderChangeEvent;
import org.bukkit.Bukkit;
import pl.kiosel.playerlist.event.TablistFooterChangeEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import pl.kiosel.playerlist.placeholder.ExtraData;
import org.bukkit.GameMode;
import org.bukkit.plugin.Plugin;
import org.bukkit.entity.Player;
import pl.kiosel.playerlist.protocol.Protocol;
import pl.kiosel.playerlist.protocol.ProtocolPlayer;

public class Tablist implements Tickable {

    @Getter private final Player player;
    @Getter private final TablistHandler handler;
    @Getter private TablistLayout layout;
    @Getter private final Plugin plugin;
    @Getter private String header;
    @Getter private String footer;
    @Getter @Setter private TablistLayoutHandler layoutHandler;
    private final boolean offlinePlayer;
    private TablistDisplay display;
    protected TablistLine[] lines;
    private GameMode mode;
    
    public Tablist(Player player, Plugin plugin) {
        this.lines = new TablistLine[0];
        this.player = player;
        this.offlinePlayer = (player.getUniqueId().version() == 3);
        this.plugin = plugin;
        this.handler = new TablistHandler(this);
        this.mode = player.getGameMode();
    }

	public int getLastLine() {
        int size = (this.layout == null) ? 0 : this.layout.getSize();
        return size - 1;
    }

	public TablistLine getLine(int index) {
        return this.lines == null || index < 0 || index >= this.lines.length ? null : this.lines[index];
    }
	public TablistDisplay getTablistDisplay() { return this.display; }
    protected boolean isPlayerCracked() { return this.offlinePlayer; }
    
    private String replace(String s) {
        return PlaceholderManager.replace(s, new ExtraData().put(ExtraData.DATA_PLAYER, player));
    }
    
    public boolean isSpectator() {
        return this.mode == GameMode.SPECTATOR;
    }
    
    protected void updateGameMode() {
        if (this.mode != getPlayer().getGameMode()) {
            List<ProtocolPlayer> profiles = new ArrayList<>();
            if (this.lines != null) {
                for (int size = this.lines.length, i = 0; i < size; ++i) {
                    if (this.lines[i] != null) {
                        profiles.add(this.lines[i].getUnsafe());
                    }
                }
                Protocol.removePlayers(getPlayer(), profiles);
            }
            this.mode = getPlayer().getGameMode();
            if (this.isSpectator() && this.size() < 80) {
                Protocol.removeBukkitPlayers(getPlayer(), Collections.singletonList(this.getPlayer()));
            }
            if (this.lines != null) {
                Protocol.infoPlayer(getPlayer(), EnumWrappers.PlayerInfoAction.ADD_PLAYER, profiles);
            }
            updatePlayer();
        }
    }
    
    @Override
    public void run() {
        if (this.handler != null) {
            this.handler.tick();
        }
        if (this.display != null) {
            this.display.tick(getPlayer());
            if (this.optimize()) {
                String h = this.replace(this.display.getCurrentHeader());
                String f = this.replace(this.display.getCurrentFooter());
                this.setHeaderFooter(h, f);
            }
        }
        updateGameMode();
    }
    
    public int size() {
        return (this.layout == null) ? 0 : this.layout.getSize();
    }
    
    public void setDisplay(TablistDisplay display) {
        if (Objects.equals(display, this.display)) {
            return;
        }
        this.setTimeout(() -> {
            this.display = display;
            this.updateDisplay();
        }, 0);
    }
    
    public void setFooter(String footer) {
        if (Objects.equals(this.footer, footer)) {
            return;
        }
        TablistFooterChangeEvent tfce = new TablistFooterChangeEvent(this, footer);
        Bukkit.getPluginManager().callEvent(tfce);
        if (tfce.isCancelled()) {
            return;
        }
        footer = tfce.getText();
        Protocol.headerFooter(player, header, footer);
        this.footer = footer;
    }
    
    public void setHeader(String header) {
        if (Objects.equals(this.header, header)) {
            return;
        }
        TablistHeaderChangeEvent thce = new TablistHeaderChangeEvent(this, header);
        Bukkit.getPluginManager().callEvent(thce);
        if (thce.isCancelled()) {
            return;
        }
        header = thce.getText();
        Protocol.headerFooter(player, header, this.footer);
        this.header = header;
    }
    
    public void setHeaderFooter(String header, String footer) {
        if (Objects.equals(this.header, header) && Objects.equals(this.footer, footer)) {
            return;
        }
        TablistHeaderChangeEvent thce = new TablistHeaderChangeEvent(this, header);
        Bukkit.getPluginManager().callEvent(thce);
        TablistFooterChangeEvent tfce = new TablistFooterChangeEvent(this, footer);
        Bukkit.getPluginManager().callEvent(tfce);
        boolean a = thce.isCancelled();
        boolean b = tfce.isCancelled();
        if (a && b) {
            return;
        }
        if (a) {
            header = this.header;
        } else {
            header = thce.getText();
        }
        if (b) {
            footer = this.footer;
        } else {
            footer = tfce.getText();
        }
        Protocol.headerFooter(player, header, footer);
        this.header = header;
        this.footer = footer;
    }
    
    public void setLayout(TablistLayout layout) {
        if (Objects.deepEquals(layout, this.layout)) {
            return;
        }
        if (this.lines != null) {
            List<UUID> players = new ArrayList<>();
            for (int i = 0; i < 80; ++i) {
                players.add(UUIDSet.getSet().get(i));
            }
            if (this.isSpectator() && layout != null && layout.getSize() < 80) {
                players.add(getPlayer().getUniqueId());
            }
            Protocol.removeRealPlayerIds(player, players);
        }
        this.layout = layout;
        updateLayout();
    }

	protected void updatePlayer() {
    }
    
    public void start() {
        this.updatePlayer();
        this.registerTask();
        this.handler.addPlayers();
    }
    
    public void stop() {
        unregisterTask();
        handler.clear();
        if (player.isOnline()) {
            if (lines != null) {
                int size = this.size();
                List<ProtocolPlayer> players = new ArrayList<>(size);
                for (int i = 0; i < size; ++i) {
                    if (this.lines[i] != null) {
                        players.add(this.lines[i].getUnsafe());
                    }
                }
                Protocol.removePlayers(getPlayer(), players);
            }
            Protocol.headerFooter(player, null, null);
            this.handler.addPlayers();
        }
    }
    
    public void updateDisplay() {
        if (display != null) {
            setHeaderFooter(replace(display.getCurrentHeader()), replace(display.getCurrentFooter()));
        } else {
            setHeaderFooter(null, null);
        }
    }
    
    public void updateLayout() {
        int size = (this.layout == null) ? 0 : this.layout.getSize();
        this.lines = new TablistLine[size];
        updateLines();
    }
    
    protected void updateLines() {
        int size = (this.layout == null) ? 0 : this.layout.getSize();
        for (int i = 0; i < size; ++i) {
            TablistLine line = this.lines[i];
            if (line == null) {
                TablistLine[] lines = this.lines;
                TablistLine tablistLine = new TablistLine(this, i);
                lines[i] = tablistLine;
                line = tablistLine;
            }
            line.show();
        }
        this.handler.quickUpdate();
        this.handler.tick();
    }
}
