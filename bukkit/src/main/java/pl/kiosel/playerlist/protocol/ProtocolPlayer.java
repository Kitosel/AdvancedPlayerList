package pl.kiosel.playerlist.protocol;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import pl.kiosel.playerlist.internal.UUIDSet;
import pl.kiosel.playerlist.tablist.Tablist;
import org.bukkit.GameMode;
import pl.kiosel.rosacore.nms.api.tablist.TabListCell;
import pl.kiosel.rosacore.nms.api.tablist.TabListSkin;

public class ProtocolPlayer {

    @Getter private String displayName;
    @Setter private TabListSkin skin;
    @Setter private int ping;
    @Setter private GameMode gameMode;
    private final int index;
    private final Tablist tablist;
    
    public ProtocolPlayer(Tablist tablist, int index) {
        this.displayName = "";
        this.tablist = tablist;
        this.index = index;
    }

    public GameMode getGameMode() {
        return this.dirty() ? GameMode.SPECTATOR : ((this.gameMode == null) ? GameMode.CREATIVE : this.gameMode);
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
    
    public TabListCell toCell(boolean listed) {
        TabListSkin renderedSkin = dirty() ? TabListSkin.fromPlayer(tablist.getPlayer()) : skin;
        return TabListCell.profile(
                getUniqueId(),
                getProfileName(),
                displayName,
                ping,
                renderedSkin,
                getGameMode(),
                listed);
    }

    public String getProfileName() {
        return dirty() ? tablist.getPlayer().getName() : ' ' + String.valueOf(UUIDSet.getPrefix(index));
    }
    
    public UUID getUniqueId() {
        return dirty() ? this.tablist.getPlayer().getUniqueId() : UUIDSet.getSet().getUniqueId(index, tablist);
    }
    
    public void setDisplayName(String display) {
        this.displayName = ((display == null) ? "" : display);
    }

}