package pl.kiosel.playerlist.api;

import lombok.Getter;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

@Getter
public final class PlaceholderContext {

    private final Object subject;
    private final Object viewer;

    PlaceholderContext(Object subject, Object viewer) {
        this.subject = subject;
        this.viewer = viewer;
    }

	public Player getPlayer() {
        return subject instanceof Player ? (Player) subject : null;
    }

    public OfflinePlayer getOfflinePlayer() {
        return subject instanceof OfflinePlayer ? (OfflinePlayer) subject : null;
    }

    public Player getViewingPlayer() {
        return viewer instanceof Player ? (Player) viewer : null;
    }
}
