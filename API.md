# AdvancedPlayerList API

## Placeholder syntax

AdvancedPlayerList deliberately keeps its own placeholders separate from PlaceholderAPI:

| Source | Syntax | Example |
| --- | --- | --- |
| PlaceholderAPI | `%identifier_parameter%` | `%player_name%` |
| AdvancedPlayerList handler | `{handler}` | `{players}` |
| AdvancedPlayerList API or script | `{identifier_parameter}` | `{village_name}` |

Inside player-list handlers, `%player_name%` describes the listed player and
`%viewer_player_name%` describes the person viewing the tab list.

Add the AdvancedPlayerList JAR as a `provided` dependency and declare it in the integrating
plugin's `plugin.yml`:

```yaml
depend:
  - AdvancedPlayerList
```

Create a placeholder class. The identifier must not contain `_`, because the first underscore
separates the identifier from its parameter.

```java
package pl.kiosel.advancedvillages.placeholder;

import org.bukkit.entity.Player;
import pl.kiosel.playerlist.api.AdvancedPlaceholder;
import pl.kiosel.playerlist.api.PlaceholderContext;

public final class VillagePlaceholder implements AdvancedPlaceholder {

    @Override
    public String getIdentifier() {
        return "village";
    }

    @Override
    public String onRequest(PlaceholderContext context, String parameter) {
        Player player = context.getPlayer();
        if (player == null) return "";

        if ("name".equalsIgnoreCase(parameter)) {
            return findVillageName(player);
        }
        if ("members".equalsIgnoreCase(parameter)) {
            return String.valueOf(findVillageMemberCount(player));
        }
        return "";
    }

    private String findVillageName(Player player) {
        return "Example village";
    }

    private int findVillageMemberCount(Player player) {
        return 1;
    }
}
```

Register it from `AdvancedVillages#onEnable()`:

```java
AdvancedPlayerListAPI.get().register(this, new VillagePlaceholder());
```

This provides `{village_name}` and `{village_members}`. These are AdvancedPlayerList placeholders,
so they use braces rather than PlaceholderAPI's percent signs. Registrations are automatically removed
when their owning plugin is disabled. They can also be removed manually:

```java
AdvancedPlayerListAPI.get().unregister(this, "village");
```
