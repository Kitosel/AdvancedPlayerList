package pl.kiosel.playerlist.placeholder;

import org.junit.jupiter.api.Test;
import pl.kiosel.playerlist.model.Evaluator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceholderManagerTest {

    @Test
    void preservesUnknownPlaceholdersAndResolvesKnownOnes() {
        String result = PlaceholderManager.extractString(
                "before {known} {unknown} after",
                name -> "known".equals(name) ? "value" : null);

        assertEquals("before value {unknown} after", result);
    }

    @Test
    void recognizesOnlyActualUnresolvedPlaceholderSyntax() {
        assertTrue(PlaceholderManager.containsUnresolvedPlaceholder("{server.online_players}"));
        assertTrue(PlaceholderManager.containsUnresolvedPlaceholder("{village_name}"));
        assertTrue(PlaceholderManager.containsUnresolvedPlaceholder("%player_name%"));
        assertTrue(PlaceholderManager.containsUnresolvedPlaceholder(
                "%player_has_permission_server.view.{world_name}%"));
        assertFalse(PlaceholderManager.containsUnresolvedPlaceholder("ordinary text"));
        assertFalse(PlaceholderManager.containsUnresolvedPlaceholder("100% complete"));
        assertFalse(PlaceholderManager.containsUnresolvedPlaceholder("{invalid placeholder}"));
    }

	@Test
	void unresolvedConditionIsRejectedBeforeItReachesJavaScriptEngine() {
		assertFalse(Evaluator.meetCriteria(null, ExtraData.DATA_PLAYER, null,
				"{missing_placeholder} && !true"));
	}

	@Test
	void detectsRegisteredParameterizedIdentifierConflicts() {
		ParameterizedPlaceholder placeholder = new ParameterizedPlaceholder() {
			@Override
			public boolean accept(String name) {
				return "village".equals(name);
			}

			@Override
			public String provide(String name, String parameter, ExtraData data) {
				return "test";
			}

			@Override
			public void onRegistered() {
			}

			@Override
			public void onUnregistered() {
			}
		};

		PlaceholderManager.register(placeholder);
		try {
			assertTrue(PlaceholderManager.hasParameterizedPlaceholder("village"));
			assertFalse(PlaceholderManager.hasParameterizedPlaceholder("another"));
		} finally {
			PlaceholderManager.unregister(placeholder);
		}
	}

    @Test
    void resolvesMixedPercentAndBracePlaceholdersInBothDirections() {
        SimplePlaceholder percent = new SimplePlaceholder() {
            @Override
            public String replace(String text, ExtraData data) {
                return text
                        .replace("%player_name%", "Alex")
                        .replace("%permission.world%", "true");
            }

            @Override
            public boolean repeatAfterParameterizedPlaceholders() {
                return true;
            }

            @Override
            public void onRegistered() {
            }

            @Override
            public void onUnregistered() {
            }
        };
        ParameterizedPlaceholder nativePlaceholder = new ParameterizedPlaceholder() {
            @Override
            public boolean accept(String name) {
                return "decorate".equals(name) || "world".equals(name);
            }

            @Override
            public String provide(String name, String parameter, ExtraData data) {
                if ("decorate".equals(name)) return "[" + parameter + "]";
                return "name".equals(parameter) ? "world" : null;
            }

            @Override
            public void onRegistered() {
            }

            @Override
            public void onUnregistered() {
            }
        };

        PlaceholderManager.register(percent, nativePlaceholder);
        try {
            assertEquals("[Alex]", PlaceholderManager.replace(
                    "{decorate_%player_name%}", new ExtraData()));
            assertEquals("true", PlaceholderManager.replace(
                    "%permission.{world_name}%", new ExtraData()));
        } finally {
            PlaceholderManager.unregister(percent);
            PlaceholderManager.unregister(nativePlaceholder);
        }
    }
}
