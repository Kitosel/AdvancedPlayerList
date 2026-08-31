package pl.kiosel.playerlist.tablist;

import org.bukkit.entity.Player;

public abstract class TablistLayoutHandler {

    private final TablistLayoutHandler parent;
    
    public TablistLayoutHandler(TablistLayoutHandler parent) {
        this.parent = parent;
    }
    
    public final TablistLayoutHandler getParent() {
        return this.parent;
    }
    
    public abstract void handle(Player p0, TablistLayout p1);
    
    final void handleLayout(Player viewer, TablistLayout layout) {
        if (this.parent != null) {
            this.parent.handle(viewer, layout);
        }
        this.handle(viewer, layout);
    }
}