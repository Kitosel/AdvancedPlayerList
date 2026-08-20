package pl.kiosel.playerlist.event;

import lombok.Getter;
import lombok.Setter;
import pl.kiosel.playerlist.tablist.Tablist;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

public class TablistHeaderChangeEvent extends Event implements Cancellable {

    private static final HandlerList handlerList = new HandlerList();
    private boolean cancel;
    private final Tablist tablist;
    @Setter
    @Getter
    private String text;
    
    public static HandlerList getHandlerList() { return TablistHeaderChangeEvent.handlerList; }
    
    public TablistHeaderChangeEvent(Tablist tablist, String text) {
        super(!Bukkit.isPrimaryThread());
        this.cancel = false;
        this.tablist = tablist;
        this.text = text;
    }

    public @NotNull HandlerList getHandlers() { return TablistHeaderChangeEvent.handlerList; }
    public Tablist getLine() { return this.tablist; }

	public boolean isCancelled() { return this.cancel; }
    public void setCancelled(boolean cancel) { this.cancel = cancel; }
}
