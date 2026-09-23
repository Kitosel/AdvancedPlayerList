package pl.kiosel.playerlist.event;

import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import pl.kiosel.playerlist.util.FakePlayer;

public class FakePlayerEvent extends Event {

	private static final HandlerList handlers = new HandlerList();

	@Getter private final FakePlayer fakePlayer;

	public FakePlayerEvent(FakePlayer fakePlayer) {
		this.fakePlayer = fakePlayer;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return handlers;
	}
}
