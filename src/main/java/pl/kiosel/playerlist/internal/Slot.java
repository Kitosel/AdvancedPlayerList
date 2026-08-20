package pl.kiosel.playerlist.internal;

import java.util.UUID;
import org.bukkit.entity.Player;
import pl.kiosel.playerlist.placeholder.ExtraData;

public class Slot extends ExtraData {

    public Slot display(String display) {
        this.put(ExtraData.DATA_TEXT, display);
        return this;
    }
    
    public Slot ping(int ping) {
        this.put(ExtraData.DATA_PING, String.valueOf(ping));
        return this;
    }
    
    public Slot skin(String name) {
        this.put(ExtraData.DATA_SKIN, name);
        return this;
    }
    
    public Slot skin(Player p) {
        this.put(ExtraData.DATA_SKIN, p.getUniqueId().toString());
        return this;
    }
    
    public Slot skin(UUID uniqueId) {
        this.put(ExtraData.DATA_SKIN, uniqueId.toString());
        return this;
    }
    
    public Slot unskin() {
        this.remove(ExtraData.DATA_SKIN);
        return this;
    }
    
    public Slot unping() {
        this.remove(ExtraData.DATA_PING);
        return this;
    }
    
    public Slot undisplay() {
        this.remove(ExtraData.DATA_TEXT);
        return this;
    }
}