# AdvancedPlayerList proxy bridge

`AdvancedPlayerListBridge.jar` is one small proxy plugin that supports both
Velocity and BungeeCord. The normal AdvancedPlayerList plugin stays on every
Bukkit, Spigot, Paper or Folia backend.

## Installation

1. Install `AdvancedPlayerListBridge-1.2.0.jar` in the proxy `plugins` directory.
2. Install the normal AdvancedPlayerList JAR on every backend.
3. Enable the bridge on every backend:

```yaml
bridge:
  enabled: true
  update-interval: 40
```

4. Restart the proxy and all backends. `/tab diag` should no longer report that
   the proxy bridge is disconnected.

The bridge uses `advancedplayerlist:bridge` on modern servers and automatically
falls back to `APLBridge` on Minecraft 1.8-1.12. Large network states are split
into safe 30 KB frames and reassembled automatically.

## Network players

```yaml
networkPlayers:
  type: NETWORK_PLAYER_LIST
  text: '{player_name} &8[&7{player_server}&8] &7{player_ping}ms'
  sorter: '{player_name}'
  skin: '{player_uuid}'
  ping: '{player_ping}'
  header: '&e&lNETWORK PLAYERS'
  hideEmpty: true
```

Available fields are `{player_name}`, `{player_displayname}`, `{player_uuid}`,
`{player_ping}`, `{player_server}` and `{player_is_online}`. PlaceholderAPI cannot
evaluate a player object located on another backend, so network fields use braces.

`BUNGEECORD_PLAYER_LIST` is accepted as an alias for old configurations.

## Network servers

```yaml
networkServers:
  type: SERVER_LIST
  text: '{server_name} &7{server_player_count}/{server_max_players}'
  sorter: '{server_name}'
  condition: '{server_is_online}'
  header: '&e&lNETWORK SERVERS'
  hideEmpty: true
```

Available fields are `{server_name}`, `{server_is_online}`,
`{server_player_count}` and `{server_max_players}`.

## Network worlds

```yaml
networkWorlds:
  type: NETWORK_WORLD_LIST
  text: '{world_server}: {world_name} &7{world_player_count}'
  sorter: '{world_server}:{world_name}'
  header: '&e&lNETWORK WORLDS'
  hideEmpty: true
```

Available fields are `{world_server}`, `{world_name}` and
`{world_player_count}`.

## A handler from another backend

This example displays the local `players` handler rendered by the backend named
`survival`:

```yaml
survivalPlayers:
  type: SERVER_REMOTE_HANDLER
  server: survival
  handler: players
  header: '&e&lSURVIVAL'
  hideEmpty: true
  refreshTicks: 40
```

`server` can also be a list. Remote handlers are limited to 80 returned lines.
They are rendered on the target backend, so its native placeholders and
PlaceholderAPI expansions are available. Viewer-specific placeholders cannot be
used because the viewer may be connected to a different server.

## Transport notes

Minecraft plugin messages need an active player connection. A backend cannot send
its first snapshot until at least one player is connected to it. BungeeCord queues
proxy-to-backend messages; Velocity sends them when the backend connection exists.

Keep backend ports private or firewalled and allow players to connect through the
trusted proxy only. The proxy bridge marks its Velocity messages as handled and
cancels BungeeCord forwarding so clients cannot impersonate backend messages
through the proxy.
