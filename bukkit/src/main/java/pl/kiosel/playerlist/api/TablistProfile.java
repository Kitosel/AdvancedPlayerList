package pl.kiosel.playerlist.api;

import lombok.Getter;

import java.util.Objects;

@Getter
public final class TablistProfile {

	private final String id;
	private final String displayName;
	private final String globalConfiguration;
	private final String handlerConfiguration;

	public TablistProfile(String id, String displayName, String globalConfiguration,
						  String handlerConfiguration) {
		this.id = requireText(id, "id");
		this.displayName = requireText(displayName, "displayName");
		this.globalConfiguration = requireText(globalConfiguration, "globalConfiguration");
		this.handlerConfiguration = requireText(handlerConfiguration, "handlerConfiguration");
	}

	private static String requireText(String value, String field) {
		Objects.requireNonNull(value, field);
		if (value.trim().isEmpty()) {
			throw new IllegalArgumentException(field + " cannot be blank");
		}
		return value;
	}
}
