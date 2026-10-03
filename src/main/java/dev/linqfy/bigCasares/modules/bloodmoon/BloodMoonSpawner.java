package dev.linqfy.bigCasares.modules.bloodmoon;

import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.GameRules;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

final class BloodMoonSpawner {
    private final Plugin plugin;
    private final BloodMoonSpawningSettings settings;
    private final Random random;
    private final NamespacedKey extraKey;
    private final NamespacedKey ownerKey;
    private final Map<String, BloodMoonSpawnLedger> ledgers = new LinkedHashMap<>();
    private final List<EntityType> types;

    BloodMoonSpawner(Plugin plugin, BloodMoonSpawningSettings settings) {
        this(plugin, settings, new Random());
    }

    BloodMoonSpawner(Plugin plugin, BloodMoonSpawningSettings settings, Random random) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.random = Objects.requireNonNull(random, "random");
        this.extraKey = new NamespacedKey(plugin, "blood_moon_extra");
        this.ownerKey = new NamespacedKey(plugin, "blood_moon_owner");
        this.types = resolveTypes(settings.types());
    }

    int spawnCycle(World world) {
        if (!canAttempt(world) || types.isEmpty()) {
            return 0;
        }
        BloodMoonSpawnLedger ledger = ledger(world);
        prune(world, ledger);
        int loadedHostiles = world.getEntitiesByClass(Monster.class).size();
        int monsterLimit = Math.max(0, world.getMonsterSpawnLimit());
        List<Player> players = new ArrayList<>(world.getPlayers());
        Collections.shuffle(players, random);
        int spawned = 0;
        for (Player player : players) {
            if (!eligible(player)) {
                continue;
            }
            int attempts = BloodMoonSpawnPolicy.extraAttemptsPerPlayer(settings.multiplier());
            for (int attempt = 0; attempt < attempts; attempt++) {
                BloodMoonSpawnPressure pressure = new BloodMoonSpawnPressure(
                    ledger.playerCount(player.getUniqueId().toString()), ledger.worldCount(),
                    loadedHostiles + spawned, monsterLimit);
                if (!BloodMoonSpawnPolicy.pressureAllows(settings, pressure)) {
                    break;
                }
                Location candidate = candidate(player);
                if (candidate == null) {
                    continue;
                }
                EntityType type = types.get(random.nextInt(types.size()));
                Entity entity;
                try {
                    entity = world.spawnEntity(candidate, type, CreatureSpawnEvent.SpawnReason.CUSTOM, spawnedEntity -> {
                        spawnedEntity.getPersistentDataContainer().set(extraKey, PersistentDataType.BYTE, (byte) 1);
                        spawnedEntity.getPersistentDataContainer().set(
                            ownerKey, PersistentDataType.STRING, player.getUniqueId().toString());
                    });
                } catch (RuntimeException failure) {
                    if (plugin.getLogger().isLoggable(java.util.logging.Level.FINE)) {
                        plugin.getLogger().fine("[BloodMoon] Extra spawn rejected: " + failure.getMessage());
                    }
                    continue;
                }
                if (!(entity instanceof LivingEntity living) || !living.isValid()) {
                    continue;
                }
                if (!ledger.tryReserve(player.getUniqueId().toString(), living.getUniqueId().toString(),
                    settings.maxExtraHostilesPerPlayer(), settings.maxExtraHostilesPerWorld())) {
                    living.remove();
                    continue;
                }
                spawned++;
            }
        }
        return spawned;
    }

    void trackLoaded(LivingEntity entity) {
        if (!isExtra(entity)) {
            return;
        }
        String owner = entity.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (owner == null || owner.isBlank()) {
            return;
        }
        ledger(entity.getWorld()).tryReserve(owner, entity.getUniqueId().toString(),
            settings.maxExtraHostilesPerPlayer(), settings.maxExtraHostilesPerWorld());
    }

    void release(Entity entity) {
        if (entity != null) {
            BloodMoonSpawnLedger ledger = ledgers.get(entity.getWorld().getName());
            if (ledger != null) {
                ledger.release(entity.getUniqueId().toString());
            }
        }
    }

    void clearWorld(String worldName) {
        BloodMoonSpawnLedger ledger = ledgers.remove(worldName);
        if (ledger != null) {
            ledger.clear();
        }
    }

    void clear() {
        ledgers.values().forEach(BloodMoonSpawnLedger::clear);
        ledgers.clear();
    }

    private boolean canAttempt(World world) {
        if (world == null || world.getDifficulty() == Difficulty.PEACEFUL
            || settings.multiplier() <= 1.0
            || settings.maxExtraHostilesPerPlayer() <= 0
            || settings.maxExtraHostilesPerWorld() <= 0) {
            return false;
        }
        return Boolean.TRUE.equals(world.getGameRuleValue(GameRules.SPAWN_MOBS))
            && Boolean.TRUE.equals(world.getGameRuleValue(GameRules.SPAWN_MONSTERS));
    }

    private boolean eligible(Player player) {
        return player != null && player.isOnline() && !player.isDead()
            && player.getGameMode() != GameMode.SPECTATOR;
    }

    private Location candidate(Player player) {
        World world = player.getWorld();
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = settings.minDistanceFromPlayer()
            + random.nextDouble() * (settings.maxDistanceFromPlayer() - settings.minDistanceFromPlayer());
        int x = player.getLocation().getBlockX() + (int) Math.round(Math.cos(angle) * distance);
        int z = player.getLocation().getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return null;
        }
        int y = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES) + 1;
        if (y <= world.getMinHeight() || y + 1 >= world.getMaxHeight()) {
            return null;
        }
        Location result = new Location(world, x + 0.5, y, z + 0.5);
        if (!BloodMoonSpawnPolicy.distanceAllowed(
            result.distance(player.getLocation()), settings.minDistanceFromPlayer(), settings.maxDistanceFromPlayer())) {
            return null;
        }
        Block below = world.getBlockAt(x, y - 1, z);
        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        if (!below.getType().isSolid() || below.isLiquid() || !feet.isPassable() || feet.isLiquid()
            || !head.isPassable() || head.isLiquid() || feet.getLightFromBlocks() > 7) {
            return null;
        }
        return result;
    }

    private BloodMoonSpawnLedger ledger(World world) {
        return ledgers.computeIfAbsent(world.getName(), ignored -> new BloodMoonSpawnLedger());
    }

    private void prune(World world, BloodMoonSpawnLedger ledger) {
        for (String rawId : ledger.mobIds()) {
            try {
                Entity entity = world.getEntity(UUID.fromString(rawId));
                if (entity == null || !entity.isValid() || entity.isDead()) {
                    ledger.release(rawId);
                }
            } catch (IllegalArgumentException ignored) {
                ledger.release(rawId);
            }
        }
    }

    private boolean isExtra(Entity entity) {
        return entity.getPersistentDataContainer().has(extraKey, PersistentDataType.BYTE);
    }

    private static List<EntityType> resolveTypes(List<String> configured) {
        List<EntityType> resolved = new ArrayList<>();
        for (String raw : configured) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                EntityType type = EntityType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
                Class<? extends Entity> entityClass = type.getEntityClass();
                if (type.isSpawnable() && entityClass != null
                    && LivingEntity.class.isAssignableFrom(entityClass)
                    && Enemy.class.isAssignableFrom(entityClass)) {
                    resolved.add(type);
                }
            } catch (IllegalArgumentException ignored) {
                // Invalid configured types are fail-safe: no spawn for that entry.
            }
        }
        return List.copyOf(resolved);
    }
}
