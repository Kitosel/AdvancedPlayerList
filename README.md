![apl title](https://cdn.modrinth.com/data/cached_images/670718ef7660f847908e3daec983b4de3c544c4b.png)
# AdvancedPlayerList

Advanced and highly configurable tab-list plugin for Bukkit, Spigot and Paper (and its forks) servers.
It is designed to work with both legacy and modern Minecraft server versions.

## Features

- Custom tablist layouts, columns, headers and footers
- All 80 slots are customizable
- A per world layout
- Animated and multiline text
- Player, world and server-list handlers
- Creating different profiles and switching between them on the fly
- Velocity and BungeeCord network bridge
- Folia scheduler support
- Fake and offline player support
- Dynamic Slots
- Custom Placeholders using JavaScript
- PlaceholderAPI integration
- Public API for placeholders supplied by other plugins
- Supports versions 1.8 through 26.2

## Requirements

- [PlaceholderAPI](https://modrinth.com/plugin/placeholderapi) — optional (but highly recommended)

## Installation

1. Optionally install PlaceholderAPI.
2. Put `AdvancedPlayerList.jar` in the server's `plugins` directory.
3. Restart the server and edit the generated configuration files.

## Velocity and BungeeCord

The proxy bridge is a separate small JAR that works on both Velocity and BungeeCord.

1. Put `AdvancedPlayerListBridge-1.2.0.jar` in the proxy's `plugins` directory.
2. Put the normal AdvancedPlayerList JAR on every backend server.
3. Set `bridge.enabled: true` in `config.yml` on every backend.
4. Restart the proxy and all backend servers. Use `/tab diag` to verify the connection.

The bridge supplies `NETWORK_PLAYER_LIST`, `SERVER_LIST`, `NETWORK_WORLD_LIST`
and `SERVER_REMOTE_HANDLER`. `BUNGEECORD_PLAYER_LIST` remains as a legacy alias.
See [BRIDGE.md](BRIDGE.md) for configuration examples and limitations.

## Folia

The plugin is marked as Folia-compatible and schedules each player's tab list on
that player's entity scheduler. Global and delayed work uses RosaCore's Folia-aware
scheduler. PlaceholderAPI expansions used by your configuration
must also support your Folia build.

## Placeholders

PlaceholderAPI uses percent signs:

```text
%player_name%
%vault_eco_balance%
```

AdvancedPlayerList handlers and custom placeholders use braces:

```text
{players}
{worldPlayers}
{village_name}
```

See [API.md](API.md) for information about registering placeholders from another plugin.

## Building

RosaCore `0.2.0` must be installed in the local Maven repository. Then run:

```shell
mvn clean package
```

This builds exactly two plugin JARs:

- `bukkit/target/AdvancedPlayerList-1.2.0.jar` for Bukkit, Spigot, Paper and Folia,
- `proxy/target/AdvancedPlayerListBridge-1.2.0.jar` for Velocity or BungeeCord.

You can also build only one plugin with `mvn -pl bukkit package` or
`mvn -pl proxy package`.

## License

Copyright (C) 2026 Kiosel.

This project is licensed under the [GNU General Public License v3.0 only](LICENSE).
