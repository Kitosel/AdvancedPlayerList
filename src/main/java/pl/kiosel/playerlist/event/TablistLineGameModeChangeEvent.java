package pl.kiosel.playerlist.event;

import lombok.Getter;
import pl.kiosel.playerlist.tablist.TablistLine;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.jetbrains.annotations.NotNull;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

public class TablistLineGameModeChangeEvent extends Event implements Cancellable {

    private static final HandlerList handlerList = new HandlerList();
    private boolean cancel;
    private GameMode ping;
    @Getter private final TablistLine line;

    public static HandlerList getHandlerList() { return TablistLineGameModeChangeEvent.handlerList; }
    
    public TablistLineGameModeChangeEvent(TablistLine line, GameMode ping) {
        super(!Bukkit.isPrimaryThread());
        this.cancel = false;
        this.line = line;
        this.ping = ping;
    }
    
    public @NotNull HandlerList getHandlers() { return TablistLineGameModeChangeEvent.handlerList; }

	public GameMode getGameMode() { return this.ping; }
    public boolean isCancelled() { return this.cancel; }
    public void setCancelled(boolean cancel) { this.cancel = cancel; }
    public void setGameMode(GameMode mode) { this.ping = mode; }
}
