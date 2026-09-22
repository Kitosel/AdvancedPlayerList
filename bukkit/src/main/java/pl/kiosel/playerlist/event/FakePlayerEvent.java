package pl.kiosel.playerlist.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import pl.kiosel.playerlist.util.FakePlayer;

public class FakePlayerEvent extends PlayerEvent {

	private static final HandlerList handlers = new HandlerList();

	public FakePlayerEvent(FakePlayer fakePlayer, Player player) {
		super(player);
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return handlers;
	}
}
