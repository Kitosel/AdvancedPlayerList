package pl.kiosel.playerlist.tablist;

import lombok.Getter;
import lombok.Setter;
import pl.kiosel.playerlist.event.TablistLineTextChangeEvent;
import pl.kiosel.playerlist.event.TablistLineSkinChangeEvent;
import pl.kiosel.playerlist.event.TablistLinePingChangeEvent;
import org.bukkit.Bukkit;
import pl.kiosel.playerlist.event.TablistLineGameModeChangeEvent;
import java.util.Objects;
import org.bukkit.GameMode;
import pl.kiosel.playerlist.protocol.ProtocolPlayer;
import pl.kiosel.playerlist.model.skin.Skin;
import pl.kiosel.rosacore.nms.api.tablist.TabListSkin;

public class TablistLine {

    @Getter
	private final ProtocolPlayer player;
    private Skin skin;
    private GameMode mode;
    boolean updateName;
    boolean updatePing;
    boolean updateSkin;
    boolean skinDone;
    boolean updateAdd;
    boolean updateRemove;
    boolean updateGameMode;
    @Setter
	@Getter
	boolean shown;
    
    TablistLine(Tablist tablist, int index) {
        this.player = new ProtocolPlayer(tablist, index);
    }
    
    public void setGameMode(GameMode mode) {
        if (Objects.equals(mode, this.mode)) {
            return;
        }
        TablistLineGameModeChangeEvent event = new TablistLineGameModeChangeEvent(this, mode);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        mode = event.getGameMode();
        this.mode = mode;
        this.updateGameMode = true;
        this.player.setGameMode(mode);
    }

	public void show() {
        if (!isShown()) {
            this.updateAdd = true;
            setShown(true);
        }
    }
    
    public void hide() {
        if (isShown()) {
            updateRemove = true;
            setShown(false);
        }
    }

	public void setPing(int ping) {
        if (Objects.equals(ping, this.player.getLatency())) {
            return;
        }
        TablistLinePingChangeEvent event = new TablistLinePingChangeEvent(this, ping);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        ping = event.getPing();
        this.updatePing = true;
        this.player.setPing(ping);
    }
    
    public void setSkin(Skin skin) {
        if (Objects.equals(skin, this.skin)) {
            this.checkCompleted();
            return;
        }
        TablistLineSkinChangeEvent event = new TablistLineSkinChangeEvent(this, skin);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        skin = event.getSkin();
        if (skin == null) {
            this.updateSkin = true;
            this.skin = null;
            this.skinDone = false;
            this.player.setSkin(null);
            return;
        }
        if (Objects.equals(skin, this.skin)) {
            this.checkCompleted();
            return;
        }
        this.skin = skin;
        this.skinDone = false;
        this.checkCompleted();
    }
    
    protected void checkCompleted() {
        if (this.skinDone) {
            return;
        }
        if (this.skin != null && this.skin.getTexture() != null) {
            this.player.setSkin(TabListSkin.of(
                    this.skin.getTexture().getValue(),
                    this.skin.getTexture().getSignature()));
            this.updateSkin = true;
            this.skinDone = true;
        }
    }
    
    public void setText(String display) {
        if (display == null || display.trim().isEmpty()) {
            display = "";
        }
        if (Objects.equals(display, this.player.getDisplayName())) {
            return;
        }
        TablistLineTextChangeEvent event = new TablistLineTextChangeEvent(this, display);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        display = event.getText();
        this.updateName = true;
        this.player.setDisplayName(display);
    }

    void resetUpdateFlags() {
        updateName = false;
        updatePing = false;
        updateSkin = false;
        updateAdd = false;
        updateRemove = false;
        updateGameMode = false;
    }
}
