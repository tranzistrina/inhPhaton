package ru.inh.gods;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

final class Phantom {
    final UUID id;
    final EntityType type;
    final UUID owner;
    final Set<UUID> observers = new LinkedHashSet<>();
    Location location;
    String displayName;
    long expiresAt;
    Entity entity;

    Phantom(UUID id, EntityType type, Location location, UUID owner, Set<UUID> observers,
            long expiresAt, String displayName) {
        this.id = id;
        this.type = type;
        this.location = location.clone();
        this.owner = owner;
        this.observers.addAll(observers);
        this.expiresAt = expiresAt;
        this.displayName = displayName;
    }

    boolean isExpired() {
        return expiresAt > 0 && System.currentTimeMillis() >= expiresAt;
    }

    boolean owns(UUID playerId) {
        return owner != null && owner.equals(playerId);
    }
}
