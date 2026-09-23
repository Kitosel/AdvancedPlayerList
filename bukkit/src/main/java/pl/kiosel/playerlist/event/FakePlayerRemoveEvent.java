package pl.kiosel.playerlist.event;

import org.bukkit.event.Cancellable;
import pl.kiosel.playerlist.util.FakePlayer;

public class FakePlayerRemoveEvent extends FakePlayerEvent implements Cancellable {

	private boolean cancelled;

	public FakePlayerRemoveEvent(FakePlayer fakePlayer) {
		super(fakePlayer);
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		this.cancelled = cancel;
	}
}
