package ru.inh.gods;


import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;

import java.util.Set;
import java.util.UUID;

/** Stable service API for other plugins; no NMS or packet classes are exposed. */
public final class GodsApi {
    private final PhantomManager manager;

    GodsApi(PhantomManager manager) {
        this.manager = manager;
    }

    public UUID createPhantom(EntityType type, Location location, UUID owner, Set<UUID> observers,
                               long ttlSeconds, String displayName) {
        return manager.createPhantom(type, location, owner, observers, ttlSeconds, displayName);
    }

    public boolean removePhantom(UUID phantomId, String reason) {
        return manager.removePhantom(phantomId, reason);
    }

    public boolean movePhantom(UUID phantomId, Location location) {
        return manager.movePhantom(phantomId, location);
    }

    public boolean showTo(UUID phantomId, UUID playerId) {
        return manager.showTo(phantomId, playerId);
    }

    public boolean hideFrom(UUID phantomId, UUID playerId) {
        return manager.hideFrom(phantomId, playerId);
    }

    public boolean showOnlyTo(UUID phantomId, Set<UUID> playerIds) {
        return manager.showOnlyTo(phantomId, playerIds);
    }

    public boolean hideFromAll(UUID phantomId) {
        return manager.hideFromAll(phantomId);
    }

    public void playPersonalEffect(UUID playerId, Particle particle, Location location,
                                   int count, Sound sound, float volume, float pitch) {
        manager.playPersonalEffect(playerId, particle, location, count, sound, volume, pitch);
    }


    public int activePhantomCount() {
        return manager.activeCount();
    }
}
