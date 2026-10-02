package dev.linqfy.bigCasares.modules.airdrop;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Pure bookkeeping for one AirDrop's defending wave. Every spawned entity
 * (zombies and their horses) is tracked by UUID plus its spawn location, and
 * mounted riders keep an explicit zombie-to-horse link so cleanup and death
 * handling can always remove the horse together with its rider.
 */
final class AirdropDefenderWave {

    private final Set<UUID> entities = new LinkedHashSet<>();
    private final Map<UUID, Location> positions = new LinkedHashMap<>();
    private final Map<UUID, UUID> riderToHorse = new LinkedHashMap<>();
    private final Map<UUID, UUID> companions = new LinkedHashMap<>();

    void track(org.bukkit.entity.Entity entity) {
        track(entity, entity.getLocation());
    }

    void track(org.bukkit.entity.Entity entity, Location location) {
        entities.add(entity.getUniqueId());
        positions.put(entity.getUniqueId(), location);
    }

    void trackRider(org.bukkit.entity.Entity rider, org.bukkit.entity.Entity horse) {
        track(rider);
        track(horse);
        riderToHorse.put(rider.getUniqueId(), horse.getUniqueId());
        companions.put(rider.getUniqueId(), horse.getUniqueId());
        companions.put(horse.getUniqueId(), rider.getUniqueId());
    }

    boolean contains(UUID entityId) {
        return entities.contains(entityId);
    }

    /** Returns the horse UUID when the tracked entity is a mounted rider. */
    UUID horseOf(UUID entityId) {
        return riderToHorse.get(entityId);
    }

    UUID companionOf(UUID entityId) {
        return companions.get(entityId);
    }

    void untrack(UUID entityId) {
        entities.remove(entityId);
        positions.remove(entityId);
        riderToHorse.remove(entityId);
        UUID companionId = companions.remove(entityId);
        if (companionId != null) {
            companions.remove(companionId);
            riderToHorse.remove(companionId);
        }
    }

    int size() {
        return entities.size();
    }

    boolean isEmpty() {
        return entities.isEmpty();
    }

    List<UUID> entityIds() {
        return new ArrayList<>(entities);
    }

    Location positionOf(UUID entityId) {
        return positions.get(entityId);
    }
}
