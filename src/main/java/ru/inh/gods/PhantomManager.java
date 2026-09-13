package ru.inh.gods;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

final class PhantomManager {
    private final JavaPlugin plugin;
    private final Map<UUID, Phantom> phantoms = new LinkedHashMap<>();
    private final File stateFile;
    private YamlConfiguration state;
    private int maxPerPlayer;
    private int maxGlobal;
    private long defaultTtlSeconds;
    private double loadRadius;
    private boolean automaticCleanup;
    private boolean debug;

    PhantomManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.stateFile = new File(plugin.getDataFolder(), "phantoms.yml");
    }

    void reloadSettings() {
        maxPerPlayer = Math.max(1, plugin.getConfig().getInt("limits.max-phantoms-per-player", 32));
        maxGlobal = Math.max(1, plugin.getConfig().getInt("limits.max-phantoms-global", 256));
        defaultTtlSeconds = Math.max(0, plugin.getConfig().getLong("limits.default-ttl-seconds", 0));
        loadRadius = Math.max(0, plugin.getConfig().getDouble("limits.load-radius", 128.0));
        automaticCleanup = plugin.getConfig().getBoolean("cleanup.automatic", true);
        debug = plugin.getConfig().getBoolean("logging.debug", false);
    }

    void start() {
        state = YamlConfiguration.loadConfiguration(stateFile);
        if (plugin.getConfig().getBoolean("persistence.restore-after-restart", true)) {
            Bukkit.getScheduler().runTask(plugin, this::restoreSaved);
        }
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    void stop() {
        saveState();
        for (Phantom phantom : new ArrayList<>(phantoms.values())) {
            if (phantom.entity != null && phantom.entity.isValid()) {
                phantom.entity.remove();
            }
        }
        phantoms.clear();
    }

    UUID createPhantom(EntityType type, Location location, UUID owner, Set<UUID> observers,
                       long ttlSeconds, String displayName) {
        if (type == null || !type.isAlive() || location == null || location.getWorld() == null) {
            throw new IllegalArgumentException("type and location must describe a live entity in a loaded world");
        }
        if (phantoms.size() >= maxGlobal) {
            throw new IllegalStateException("global phantom limit reached: " + maxGlobal);
        }
        if (owner != null && countOwned(owner) >= maxPerPlayer) {
            throw new IllegalStateException("per-player phantom limit reached: " + maxPerPlayer);
        }

        Set<UUID> safeObservers = observers == null ? new LinkedHashSet<>() : new LinkedHashSet<>(observers);
        if (safeObservers.isEmpty() && owner != null) {
            safeObservers.add(owner);
        }
        long ttl = ttlSeconds > 0 ? ttlSeconds : defaultTtlSeconds;
        long expiresAt = ttl > 0 ? System.currentTimeMillis() + ttl * 1000L : 0;
        Phantom phantom = new Phantom(UUID.randomUUID(), type, location, owner, safeObservers, expiresAt, displayName);
        try {
            phantom.entity = location.getWorld().spawnEntity(location, type);
            phantom.entity.setPersistent(false);
            if (displayName != null && !displayName.isBlank()) {
                phantom.entity.setCustomName(ChatColor.translateAlternateColorCodes('&', displayName));
                phantom.entity.setCustomNameVisible(true);
            }
            if (phantom.entity instanceof Mob mob) {
                mob.setAware(true);
            }
            phantoms.put(phantom.id, phantom);
            hideEntityFromAll(phantom);
            refresh(phantom);
            saveState();
            logDebug("created id=" + phantom.id + " type=" + type + " owner=" + owner
                    + " observers=" + safeObservers.size() + " at=" + formatLocation(location));
            return phantom.id;
        } catch (RuntimeException exception) {
            if (phantom.entity != null && phantom.entity.isValid()) {
                phantom.entity.remove();
            }
            throw exception;
        }
    }

    boolean removePhantom(UUID id, String reason) {
        Phantom phantom = phantoms.remove(id);
        if (phantom == null) {
            return false;
        }
        if (phantom.entity != null && phantom.entity.isValid()) {
            phantom.entity.remove();
        }
        saveState();
        logDebug("removed id=" + id + " reason=" + reason);
        return true;
    }

    boolean movePhantom(UUID id, Location location) {
        Phantom phantom = phantoms.get(id);
        if (phantom == null || location == null || location.getWorld() == null || phantom.entity == null) {
            return false;
        }
        if (!phantom.entity.teleport(location)) {
            return false;
        }
        phantom.location = location.clone();
        refresh(phantom);
        saveState();
        logDebug("moved id=" + id + " to=" + formatLocation(location));
        return true;
    }

    boolean showTo(UUID id, UUID playerId) {
        Phantom phantom = phantoms.get(id);
        Player player = Bukkit.getPlayer(playerId);
        if (phantom == null || player == null) {
            return false;
        }
        phantom.observers.add(playerId);
        refresh(phantom);
        saveState();
        return true;
    }

    boolean hideFrom(UUID id, UUID playerId) {
        Phantom phantom = phantoms.get(id);
        Player player = Bukkit.getPlayer(playerId);
        if (phantom == null) {
            return false;
        }
        phantom.observers.remove(playerId);
        if (player != null && phantom.entity != null) {
            player.hideEntity(plugin, phantom.entity);
        }
        saveState();
        return true;
    }

    boolean showOnlyTo(UUID id, Set<UUID> playerIds) {
        Phantom phantom = phantoms.get(id);
        if (phantom == null) {
            return false;
        }
        phantom.observers.clear();
        if (playerIds != null) {
            phantom.observers.addAll(playerIds);
        }
        refresh(phantom);
        saveState();
        return true;
    }

    boolean hideFromAll(UUID id) {
        Phantom phantom = phantoms.get(id);
        if (phantom == null) {
            return false;
        }
        phantom.observers.clear();
        hideEntityFromAll(phantom);
        saveState();
        return true;
    }

    void refreshPlayer(Player player) {
        for (Phantom phantom : phantoms.values()) {
            refreshForPlayer(phantom, player);
        }
    }

    void playPersonalEffect(UUID playerId, Particle particle, Location location, int count,
                            Sound sound, float volume, float pitch) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || location == null || particle == null) {
            return;
        }
        player.spawnParticle(particle, location, Math.max(0, count));
        if (sound != null) {
            player.playSound(location, sound, volume, pitch);
        }
    }


    int activeCount() {
        return phantoms.size();
    }

    List<Phantom> all() {
        return new ArrayList<>(phantoms.values());
    }

    private void tick() {
        if (automaticCleanup) {
            for (Phantom phantom : new ArrayList<>(phantoms.values())) {
                if (phantom.isExpired()) {
                    removePhantom(phantom.id, "ttl");
                } else if (phantom.entity == null || !phantom.entity.isValid()) {
                    removePhantom(phantom.id, "entity-invalid");
                }
            }
        }
    }

    private void refresh(Phantom phantom) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshForPlayer(phantom, player);
        }
    }

    private void hideEntityFromAll(Phantom phantom) {
        if (phantom.entity == null) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.hideEntity(plugin, phantom.entity);
        }
    }

    private void refreshForPlayer(Phantom phantom, Player player) {
        if (phantom.entity == null || !phantom.entity.isValid()) {
            return;
        }
        boolean visible = phantom.observers.contains(player.getUniqueId())
                && player.getWorld().equals(phantom.entity.getWorld())
                && (loadRadius <= 0 || player.getLocation().distanceSquared(phantom.entity.getLocation()) <= loadRadius * loadRadius);
        if (visible) {
            player.showEntity(plugin, phantom.entity);
        } else {
            player.hideEntity(plugin, phantom.entity);
        }
    }

    private void restoreSaved() {
        ConfigurationSection root = state.getConfigurationSection("phantoms");
        if (root == null) {
            return;
        }
        for (String rawId : root.getKeys(false)) {
            try {
                UUID id = UUID.fromString(rawId);
                String worldName = state.getString("phantoms." + rawId + ".world");
                World world = worldName == null ? null : Bukkit.getWorld(worldName);
                EntityType type = EntityType.valueOf(state.getString("phantoms." + rawId + ".type", "ZOMBIE"));
                if (world == null || !type.isAlive()) {
                    continue;
                }
                long expiresAt = state.getLong("phantoms." + rawId + ".expires-at", 0);
                if (expiresAt > 0 && expiresAt <= System.currentTimeMillis()) {
                    continue;
                }
                Location location = new Location(world,
                        state.getDouble("phantoms." + rawId + ".x"),
                        state.getDouble("phantoms." + rawId + ".y"),
                        state.getDouble("phantoms." + rawId + ".z"),
                        (float) state.getDouble("phantoms." + rawId + ".yaw"),
                        (float) state.getDouble("phantoms." + rawId + ".pitch"));
                UUID owner = parseUuid(state.getString("phantoms." + rawId + ".owner"));
                Set<UUID> observers = new LinkedHashSet<>();
                for (String observer : state.getStringList("phantoms." + rawId + ".observers")) {
                    UUID parsed = parseUuid(observer);
                    if (parsed != null) observers.add(parsed);
                }
                Phantom phantom = new Phantom(id, type, location, owner, observers, expiresAt,
                        state.getString("phantoms." + rawId + ".name"));
                phantom.entity = world.spawnEntity(location, type);
                phantom.entity.setPersistent(false);
                if (phantom.displayName != null && !phantom.displayName.isBlank()) {
                    phantom.entity.setCustomName(ChatColor.translateAlternateColorCodes('&', phantom.displayName));
                    phantom.entity.setCustomNameVisible(true);
                }
                phantoms.put(id, phantom);
                hideEntityFromAll(phantom);
                refresh(phantom);
            } catch (Exception exception) {
                plugin.getLogger().log(Level.WARNING, "Не удалось восстановить фантом " + rawId, exception);
            }
        }
        plugin.getLogger().info("Восстановлено фантомов: " + phantoms.size());
    }

    private void saveState() {
        if (state == null) {
            state = new YamlConfiguration();
        }
        state.set("phantoms", null);
        for (Phantom phantom : phantoms.values()) {
            String path = "phantoms." + phantom.id;
            Location location = phantom.entity != null && phantom.entity.isValid()
                    ? phantom.entity.getLocation() : phantom.location;
            state.set(path + ".type", phantom.type.name());
            state.set(path + ".world", location.getWorld().getName());
            state.set(path + ".x", location.getX());
            state.set(path + ".y", location.getY());
            state.set(path + ".z", location.getZ());
            state.set(path + ".yaw", location.getYaw());
            state.set(path + ".pitch", location.getPitch());
            state.set(path + ".owner", phantom.owner == null ? null : phantom.owner.toString());
            state.set(path + ".observers", phantom.observers.stream().map(UUID::toString).toList());
            state.set(path + ".expires-at", phantom.expiresAt);
            state.set(path + ".name", phantom.displayName);
        }
        try {
            stateFile.getParentFile().mkdirs();
            state.save(stateFile);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Не удалось сохранить phantoms.yml", exception);
        }
    }

    private int countOwned(UUID owner) {
        return (int) phantoms.values().stream().filter(phantom -> phantom.owns(owner)).count();
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) return null;
        try { return UUID.fromString(value); } catch (IllegalArgumentException ignored) { return null; }
    }

    private String formatLocation(Location location) {
        return location.getWorld().getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ();
    }

    private void logDebug(String message) {
        if (debug) plugin.getLogger().info("[debug] " + message);
    }
}
