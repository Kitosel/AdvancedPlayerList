package pl.kiosel.playerlist.config;

import lombok.Getter;
import pl.kiosel.rosacore.message.MessageKey;

public enum Lang implements MessageKey {

	TABLIST_ENABLE("command.admin.tablist-enable"),
	TABLIST_DISABLE("command.admin.tablist-disable"),
	TABLIST_ALREADY_ENABLED("command.admin.tablist-already-enabled"),
	TABLIST_ALREADY_DISABLED("command.admin.tablist-already-disabled"),
	RELOAD("command.admin.reload"),
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

	MAX_CHARACTERS("command.fakeplayer.max-characters"),
	ALREADY_EXISTS("command.fakeplayer.already-exists"),
	SPAWN("command.fakeplayer.spawn"),
	REMOVED("command.fakeplayer.removed"),
	NOT_EXISTS("command.fakeplayer.not-exists"),
	ADDED_PLACEHOLDER("command.fakeplayer.added-placeholder"),
	REMOVED_PLACEHOLDER("command.fakeplayer.removed-placeholder"),
	LIST("command.fakeplayer.list"),;

	@Getter private final String path;

	Lang(String path) {
		this.path = path;
	}
}