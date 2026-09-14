# inhPhaton

Paper 26.2 plugin with per-player visibility, phantom mobs and personal effects.

## Features

- phantom entities with UUID, type, location, owner and observers;
- per-player show/hide/only/except visibility;
- movement, TTL cleanup and configurable limits;
- persistence and restore after restart;
- personal particles and sounds through the service API;
- administrator commands in Russian.

The stable Bukkit implementation hides server entities from non-observers and shows them only to selected players. The public API keeps transport details isolated for a future packet-only backend.

## Commands

```text
/inhPhaton phantom create <entity-type> [observer] [ttl-seconds]
/inhPhaton phantom remove <phantom-uuid>
/inhPhaton phantom move <phantom-uuid> <x> <y> <z>
/inhPhaton phantom show <phantom-uuid> <player>
/inhPhaton phantom hide <phantom-uuid> <player>
/inhPhaton phantom showonly <phantom-uuid> <player>
/inhPhaton phantom hideall <phantom-uuid>
/inhPhaton visibility only <entity-uuid-or-runtime-id> <player>
/inhPhaton visibility except <entity-uuid-or-runtime-id> <player>
/inhPhaton visibility hidden <entity-uuid-or-runtime-id>
/inhPhaton list
/inhPhaton debug [true|false]
/inhPhaton reload
```

The command is registered through Paper 26.2's `JavaPlugin#registerCommand` API, so it does not depend on YAML command declarations and works for Paper plugin loading. It is also available through the aliases `/god` and `/gods`.

## Build

From this directory:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@25 gradle clean jar --no-daemon
```

If the Paper server libraries are elsewhere:

```bash
PAPER_LIBRARIES_DIR=/path/to/server/libraries JAVA_HOME=/opt/homebrew/opt/openjdk@25 gradle clean jar --no-daemon
```

Output: `build/libs/inhPhaton-1.0.0.jar`.
