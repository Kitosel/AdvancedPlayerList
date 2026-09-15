package pl.kiosel.playerlist.protocol;

import pl.kiosel.rosacore.version.Version;

public final class Protocol {

	private Protocol() {
	}

	public static boolean usesModernPlayerInfo() {
		return Version.isServerVersionAbove(Version.V1_19_2);
	}
}
