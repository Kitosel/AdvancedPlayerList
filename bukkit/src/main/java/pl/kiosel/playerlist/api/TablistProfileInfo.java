package pl.kiosel.playerlist.api;

import lombok.Getter;

@Getter
public final class TablistProfileInfo {

	private final String id;
	private final String displayName;
	private final String owner;
	private final boolean active;

	public TablistProfileInfo(String id, String displayName, String owner, boolean active) {
		this.id = id;
		this.displayName = displayName;
		this.owner = owner;
		this.active = active;
	}

}
