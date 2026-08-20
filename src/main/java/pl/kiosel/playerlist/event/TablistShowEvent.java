package pl.kiosel.playerlist.event;

import lombok.Getter;
import pl.kiosel.playerlist.tablist.Tablist;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

public class TablistShowEvent extends Event implements Cancellable {

    private static final HandlerList handlerList = new HandlerList();
    private boolean cancel;
    @Getter private final Tablist tablist;
    
    public static HandlerList getHandlerList() { return TablistShowEvent.handlerList; }
    
    public TablistShowEvent(Tablist tab) {
        super(!Bukkit.isPrimaryThread());
        this.cancel = false;
        this.tablist = tab;
    }
    
    public @NotNull HandlerList getHandlers() { return TablistShowEvent.handlerList; }

	public boolean isCancelled() { return this.cancel; }
    public void setCancelled(boolean cancel) { this.cancel = cancel; }
}
