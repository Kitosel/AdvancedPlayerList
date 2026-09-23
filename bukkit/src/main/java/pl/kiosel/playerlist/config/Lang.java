package pl.kiosel.playerlist.config;

import lombok.Getter;
import pl.kiosel.rosacore.message.MessageKey;

public enum Lang implements MessageKey {

	UPDATE("new-version"),

	TABLIST_ENABLE("command.admin.tablist-enable"),
	TABLIST_DISABLE("command.admin.tablist-disable"),
	TABLIST_ALREADY_ENABLED("command.admin.tablist-already-enabled"),
	TABLIST_ALREADY_DISABLED("command.admin.tablist-already-disabled"),
	RELOAD("command.admin.reload"),
	TOGGLE_ENABLE("command.toggle-enable"),
	TOGGLE_DISABLE("command.toggle-disable"),
	CHECK_PLACEHOLDERS("command.admin.check-placeholders"),
	MISSING_PLACEHOLDERS("command.admin.missing-placeholders"),
	REQUIRE_PLACEHOLDER("command.admin.require-placeholderapi"),
	OFFLINEPLAYERS_IMPORT("command.admin.offlineplayers-import"),
	OFFLINEPLAYERS_IMPORTED("command.admin.offlineplayers-imported"),
	PLACEHOLDER_TEST("command.admin.placeholder-test"),
	CHANGED_SIZE("command.admin.changed-size"),
	ACTUAL_SIZE("command.admin.actual-size"),
	INVALID_SIZE("command.admin.invalid-size"),
	INVALID_NUMBER("command.admin.invalid-number"),
	PROFILE_USAGE("command.admin.profile.usage"),
	PROFILE_LIST("command.admin.profile.list"),
	PROFILE_STATUS("command.admin.profile.status"),
	PROFILE_ACTIVATED("command.admin.profile.activated"),
	PROFILE_RESTORED("command.admin.profile.restored"),
	PROFILE_ERROR("command.admin.profile.error"),

	MAX_CHARACTERS("command.fakeplayer.max-characters"),
	ALREADY_EXISTS("command.fakeplayer.already-exists"),
	SPAWN("command.fakeplayer.spawn"),
	REMOVED("command.fakeplayer.removed"),
	NOT_EXISTS("command.fakeplayer.not-exists"),
	ADDED_PLACEHOLDER("command.fakeplayer.added-placeholder"),
	REMOVED_PLACEHOLDER("command.fakeplayer.removed-placeholder"),
	HEAD_UPDATED("command.fakeplayer.head-updated"),
	SKIN_UPDATED("command.fakeplayer.skin-updated"),
	SKIN_REFRESHING("command.fakeplayer.skin-refreshing"),
	SKIN_REFRESHED("command.fakeplayer.skin-refreshed"),
	SKIN_DISABLED("command.fakeplayer.skin-disabled"),
	SKIN_NOT_FOUND("command.fakeplayer.skin-not-found"),
	INVALID_SKIN("command.fakeplayer.invalid-skin"),
	INVALID_BOOLEAN("command.fakeplayer.invalid-boolean"),
	LIST("command.fakeplayer.list"),;

	@Getter private final String path;

	Lang(String path) {
		this.path = path;
	}
}
