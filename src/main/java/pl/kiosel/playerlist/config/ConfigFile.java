package pl.kiosel.playerlist.config;

import lombok.Getter;

public enum ConfigFile {

	CONFIG("config.yml"),
	HANDLER("handler.yml"),
	GLOBAL("global.yml"),
	FAKE_PLAYER("fakeplayers.bin"),
	SKIN_CACHE("skincache.tmp");

	@Getter private final String path;

	ConfigFile(String path) {
		this.path = path;
	}

}
