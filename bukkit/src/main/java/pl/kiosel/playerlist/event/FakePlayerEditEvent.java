package pl.kiosel.playerlist.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import pl.kiosel.playerlist.util.FakePlayer;

public class FakePlayerEditEvent extends FakePlayerEvent implements Cancellable {

	private boolean cancelled;

	public FakePlayerEditEvent(FakePlayer fakePlayer, Player player) {
		super(fakePlayer, player);
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
