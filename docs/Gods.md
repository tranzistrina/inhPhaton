# inhPhaton Paper 26.2

`inhPhaton` implements the first complete runtime of the Gods technical specification.

## Features

- per-player phantom mobs with UUID, type, position, display name, owner and observers;
- `VISIBLE`-style default visibility, `HIDDEN` via `hideall`, `ONLY` via `showonly`, and `EXCEPT` via `/god visibility except`;
- move/show/hide/remove without restarting;
- personal particles and sounds through the public Bukkit service API;
- observer refresh after join, teleport and world change;
- TTL cleanup and limits per owner/global;
- YAML configuration and persisted phantom definitions in `plugins/Gods/phantoms.yml`;
- debug logging and active-object listing;
- no NMS in the public API; future packet-only transport can replace the internal implementation.

The current transport uses ordinary Bukkit entities, marks them non-persistent, hides them from every online player and shows them only to configured observers. This is intentionally stable on Paper 26.2 and avoids exposing a phantom to non-observers. A packet-only backend can be added behind the same service API when PacketEvents compatibility is confirmed.

## Commands

```text
/god phantom create <entity-type> [observer] [ttl-seconds]
/god phantom remove <phantom-uuid>
/god phantom move <phantom-uuid> <x> <y> <z>
/god phantom show <phantom-uuid> <player>
/god phantom hide <phantom-uuid> <player>
/god phantom showonly <phantom-uuid> <player>
/god phantom hideall <phantom-uuid>
/god visibility only <entity-uuid-or-runtime-id> <player>
/god visibility except <entity-uuid-or-runtime-id> <player>
/god visibility hidden <entity-uuid-or-runtime-id>
/god list
/god debug [true|false]
/god reload
```

`phantom create` is currently an in-game command: it creates the mob at the executing player's position. The optional observer defaults to the executing player. TTL `0` means no expiry unless a default is configured.

## API

Other plugins obtain the service with Bukkit's `ServicesManager`:

```java
GodsApi gods = Bukkit.getServicesManager()
    .load(ru.inh.gods.GodsApi.class);
```

The API contains `createPhantom`, `removePhantom`, `movePhantom`, `showTo`, `hideFrom`, `showOnlyTo`, `hideFromAll` and `playPersonalEffect`.

## Configuration

- `limits.max-phantoms-per-player`
- `limits.max-phantoms-global`
- `limits.default-ttl-seconds`
- `limits.load-radius`
- `cleanup.automatic`
- `persistence.restore-after-restart`
- `logging.debug`
