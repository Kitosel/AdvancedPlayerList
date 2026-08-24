package pl.kiosel.playerlist.protocol;

import java.util.UUID;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import lombok.Getter;
import pl.kiosel.playerlist.internal.UUIDSet;
import pl.kiosel.playerlist.tablist.Tablist;
import org.bukkit.GameMode;

public class ProtocolPlayer {

    @Getter private String displayName;
    @Getter private final WrappedGameProfile appended;
    private GameMode mode;
    private int ping;
    private final int index;
    private final Tablist tablist;
    
    public ProtocolPlayer(Tablist tablist, int index) {
        this.displayName = "";
        this.tablist = tablist;
        this.index = index;
        this.appended = new WrappedGameProfile(UUIDSet.getSet().get(index), null);
    }

    public GameMode getGameMode() {
        return this.dirty() ? GameMode.SPECTATOR : ((this.mode == null) ? GameMode.CREATIVE : this.mode);
    }

	public int getLatency() { return this.ping; }
    
    public boolean dirty() {
        return isSpectatorViewerSlot(
                Protocol.usesModernPlayerInfo(),
                this.tablist.isSpectator(),
                this.tablist.getLastLine(),
                this.index);
    }

    static boolean isSpectatorViewerSlot(boolean modernPlayerInfo, boolean spectator, int lastLine, int index) {
        return !modernPlayerInfo && spectator && lastLine >= 0 && lastLine == index;
    }
    
    public WrappedGameProfile getProfile() {
        if (dirty())
            return WrappedGameProfile.fromPlayer(this.tablist.getPlayer());

        WrappedGameProfile profile = new WrappedGameProfile(getUniqueId(), ' ' + String.valueOf(UUIDSet.getPrefix(index)));
        profile.getProperties().putAll(appended.getProperties());
        return profile;
    }
    
    public UUID getUniqueId() {
        return dirty() ? this.tablist.getPlayer().getUniqueId() : UUIDSet.getSet().getUniqueId(index, tablist);
    }
    
    public void setDisplayName(String display) {
        this.displayName = ((display == null) ? "" : display);
    }
    
    public void setGameMode(GameMode mode) {
        this.mode = mode;
    }
    
    public void setLatency(int ping) {
        this.ping = ping;
    }
}
