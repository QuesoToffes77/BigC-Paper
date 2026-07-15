package dev.linqfy.bigCasares.modules.servercontrol;

import org.bukkit.potion.PotionEffect;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class VanishSessionRegistry {
    private final Map<UUID, VanishSnapshot> snapshots = new HashMap<>();

    public synchronized boolean contains(UUID playerId) {
        return snapshots.containsKey(playerId);
    }

    public synchronized void put(UUID playerId, VanishSnapshot snapshot) {
        snapshots.put(playerId, snapshot);
    }

    public synchronized VanishSnapshot remove(UUID playerId) {
        return snapshots.remove(playerId);
    }

    public synchronized VanishSnapshot snapshot(UUID playerId) {
        return snapshots.get(playerId);
    }

    public synchronized Set<UUID> playerIds() {
        return Set.copyOf(snapshots.keySet());
    }

    public record VanishSnapshot(
        boolean invisible,
        boolean silent,
        boolean collidable,
        boolean canPickupItems,
        boolean visibleByDefault,
        PotionEffect invisibilityEffect
    ) {
    }
}
