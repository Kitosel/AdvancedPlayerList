package pl.kiosel.playerlist.event;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import pl.kiosel.playerlist.tablist.TablistLine;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

public class TablistLineTextChangeEvent extends Event implements Cancellable {

    private static final HandlerList handlerList = new HandlerList();
    private boolean cancel;
    @Getter private final TablistLine line;
    @Setter @Getter private String text;
    
    public static HandlerList getHandlerList() { return TablistLineTextChangeEvent.handlerList; }
    
    public TablistLineTextChangeEvent(TablistLine line, String text) {
        super(!Bukkit.isPrimaryThread());
        this.cancel = false;
        this.line = line;
        this.text = text;
    }
    
    public @NotNull HandlerList getHandlers() { return TablistLineTextChangeEvent.handlerList; }

	public boolean isCancelled() { return this.cancel; }
    public void setCancelled(boolean cancel) { this.cancel = cancel; }
}
