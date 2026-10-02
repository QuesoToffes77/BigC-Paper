package dev.linqfy.bigCasares.modules.acidrain;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

final class AcidRainExposureCache {
    private static final long CACHE_TICKS = 20L;
    private final Map<UUID, CachedExposure> cache = new LinkedHashMap<>();

    boolean isExposed(Player player, long currentTick, AcidRainSettings settings) {
        Location location = player.getLocation();
        UUID playerId = player.getUniqueId();
        CachedExposure cached = cache.get(playerId);
        if (cached != null && cached.matches(location) && currentTick < cached.expiresAtTick()) {
            return cached.exposed();
        }
        boolean exposed = computeExposure(location, settings);
        cache.put(playerId, new CachedExposure(
            location.getWorld().getName(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ(),
            exposed,
            currentTick + CACHE_TICKS
        ));
        return exposed;
    }

    void invalidate(UUID playerId) {
        cache.remove(playerId);
    }

    void clear() {
        cache.clear();
    }

    private boolean computeExposure(Location location, AcidRainSettings settings) {
        World world = location.getWorld();
        if (world == null || !settings.worlds().isAffected(world.getName())) {
            return false;
        }
        int highestY = world.getHighestBlockYAt(location);
        return location.getBlockY() >= highestY;
    }

    private record CachedExposure(
        String worldName,
        int x,
        int y,
        int z,
        boolean exposed,
        long expiresAtTick
    ) {
        boolean matches(Location location) {
            World world = location.getWorld();
            return world != null
                && world.getName().equals(worldName)
                && location.getBlockX() == x
                && location.getBlockY() == y
                && location.getBlockZ() == z;
        }
    }
}
