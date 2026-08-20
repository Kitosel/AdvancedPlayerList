package pl.kiosel.playerlist.event;

import lombok.Getter;
import lombok.Setter;
import pl.kiosel.playerlist.model.skin.Skin;
import pl.kiosel.playerlist.tablist.TablistLine;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

public class TablistLineSkinChangeEvent extends Event implements Cancellable {

    private static final HandlerList handlerList = new HandlerList();
    private boolean cancel;
    @Getter private final TablistLine line;
    @Setter @Getter private Skin skin;
    
    public static HandlerList getHandlerList() { return TablistLineSkinChangeEvent.handlerList; }
    
    public TablistLineSkinChangeEvent(TablistLine line, Skin skin) {
        super(!Bukkit.isPrimaryThread());
        this.cancel = false;
        this.line = line;
        this.skin = skin;
    }
    
    public @NotNull HandlerList getHandlers() { return TablistLineSkinChangeEvent.handlerList; }

	public boolean isCancelled() { return this.cancel; }
    public void setCancelled(boolean cancel) { this.cancel = cancel; }
}
