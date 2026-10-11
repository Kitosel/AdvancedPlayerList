package pl.kiosel.playerlist.config;

import pl.kiosel.rosacore.config.setting.SettingConstraints;
import pl.kiosel.rosacore.config.setting.SettingKey;
import pl.kiosel.rosacore.config.setting.SettingsSchema;

import java.util.Collections;
import java.util.List;

public class Settings {

	private Settings() {
	}

	public static final SettingKey<String> LOCALE = SettingKey.string("locale", "en_US")
			.constraint(SettingConstraints.nonBlank())
			.build();

	public static final SettingKey<Boolean> TABLIST_ENABLED = SettingKey.booleanValue("tablist-enabled", true)
			.build();

	public static final SettingKey<Boolean> UPDATE_CHECKER = SettingKey.booleanValue("update-checker", true)
			.description("Checks if new version is available")
			.build();

	public static final SettingKey<Boolean> TABLIST_WITH_COLUMNS = SettingKey.booleanValue("tablist-with-columns", true)
			.description("Should the table have columns, or can it be a simple tablist with only a header, and a footer?")
			.build();

	public static final SettingKey<Boolean> PLAYER_TOGGLE_TABLIST = SettingKey.booleanValue("player-can-toggle-tablist", true)
			.description("Allow players to turn on and off tablist by its self")
			.build();

	public static final SettingKey<String> PROFILE_ACTIVE = SettingKey.string("profiles.active", "default")
			.build();

	public static final SettingKey<String> PROFILE_PREVIOUS = SettingKey.string("profiles.previous", "default")
			.build();

	public static final SettingKey<String> ECONOMY = SettingKey.string("economy", "Vault")
			.description("Preferred Economy (Vault,Essentials,PlayerPoints)")
			.build();

	public static final SettingKey<Integer> TASK_INTERVAL = SettingKey.integer("task-interval", 20)
			.description("Global update interval in ticks (20 ticks = 1 second).")
			.build();

	public static final SettingKey<Boolean> BRIDGE_ENABLED = SettingKey.booleanValue("bridge.enabled", false)
			.build();

	public static final SettingKey<Integer> BRIDGE_UPDATE = SettingKey.integer("bridge.update-interval", 40)
			.build();

	public static final SettingKey<List<String>> WORLD_BLACKLIST = SettingKey.stringList("worldBlacklist", Collections.singletonList("NoTablistWorld"))
			.description("Players in these worlds will not receive the custom tab list.")
			.build();

	public static final SettingKey<String> MINESKIN_API_KEY = SettingKey.string("skin.mineskin-api-key", "")
			.build();

	public static final SettingKey<Boolean> OFFLINE_PLAYERS_ENABLE = SettingKey.booleanValue("offline-players.enabled", false)
			.build();

	public static final SettingKey<String> OFFLINE_PLAYERS_PURGETIME = SettingKey.string("offline-players.purgetime", "30d")
			.build();

	public static final SettingsSchema SCHEMA = SettingsSchema.builder()
			.addAll(
					LOCALE, UPDATE_CHECKER, ECONOMY,
					TABLIST_ENABLED, TABLIST_WITH_COLUMNS, PLAYER_TOGGLE_TABLIST,
					PROFILE_ACTIVE, PROFILE_PREVIOUS,
					TASK_INTERVAL, BRIDGE_ENABLED, BRIDGE_UPDATE,
					WORLD_BLACKLIST, MINESKIN_API_KEY,
					OFFLINE_PLAYERS_ENABLE, OFFLINE_PLAYERS_PURGETIME
			)
			.build();
}
