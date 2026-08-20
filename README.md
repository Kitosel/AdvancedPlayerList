# AdvancedPlayerList

Advanced and highly configurable tab-list plugin for Bukkit, Spigot and Paper servers.
It is designed to work with both legacy and modern Minecraft server versions.

## Features

- Custom tab-list layouts, columns, headers and footers
- Animated and multiline text
- Player, world and server-list handlers
- Fake and offline player support
- PlaceholderAPI integration
- Public API for placeholders supplied by other plugins
- Compatibility logic for legacy and modern server internals

## Requirements

- **ProtocolLib** — required
- **PlaceholderAPI** — optional (but highly recommended)

## Installation

1. Install ProtocolLib and optionally PlaceholderAPI.
2. Put `AdvancedPlayerList.jar` in the server's `plugins` directory.
3. Restart the server and edit the generated configuration files.

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

RosaCore `0.1.0` must be installed in the local Maven repository. Then run:

```shell
mvn clean package
```

The compiled plugin will be created in the `target` directory.

## License

Copyright (C) 2026 Kiosel.

This project is licensed under the [GNU General Public License v3.0 only](LICENSE).
