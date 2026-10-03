package dev.linqfy.bigCasares.modules.airdrop;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure bookkeeping tests for the defender wave: entities are tracked by UUID
 * with their spawn position, and riders keep their zombie-to-horse link so
 * the horse is always removable together with its rider.
 */
class AirdropDefenderWaveTest {

    private final UUID zombieId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID horseId = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void tracksEntitiesWithTheirPosition() {
        AirdropDefenderWave wave = new AirdropDefenderWave();
        Location location = new Location(null, 100, 64, 200);

        wave.track(entity(zombieId), location);

        assertTrue(wave.contains(zombieId));
        assertEquals(1, wave.size());
        assertEquals(location, wave.positionOf(zombieId));
    }

    @Test
    void untrackRemovesEntityCompletely() {
        AirdropDefenderWave wave = new AirdropDefenderWave();
        wave.track(entity(zombieId), new Location(null, 1, 2, 3));

        wave.untrack(zombieId);

        assertFalse(wave.contains(zombieId));
        assertNull(wave.positionOf(zombieId));
        assertTrue(wave.isEmpty());
    }

    @Test
    void riderLinksItsHorseForJointRemoval() {
        AirdropDefenderWave wave = new AirdropDefenderWave();

        wave.trackRider(entity(zombieId), entity(horseId));

        assertTrue(wave.contains(zombieId));
        assertTrue(wave.contains(horseId));
        assertEquals(horseId, wave.horseOf(zombieId));
        assertEquals(horseId, wave.companionOf(zombieId));
        assertEquals(zombieId, wave.companionOf(horseId));
        assertNull(wave.horseOf(horseId));
        assertEquals(2, wave.size());
    }

    @Test
    void untrackingTheRiderDropsTheHorseLink() {
        AirdropDefenderWave wave = new AirdropDefenderWave();
        wave.trackRider(entity(zombieId), entity(horseId));

        wave.untrack(zombieId);

        assertNull(wave.horseOf(zombieId));
        assertNull(wave.companionOf(zombieId));
        assertNull(wave.companionOf(horseId));
        assertFalse(wave.contains(zombieId));
    }

    @Test
    void entityIdsAreStableSnapshots() {
        AirdropDefenderWave wave = new AirdropDefenderWave();
        wave.track(entity(zombieId), new Location(null, 0, 0, 0));
        wave.track(entity(horseId), new Location(null, 0, 0, 0));

        var ids = wave.entityIds();
        assertEquals(2, ids.size());
        assertTrue(ids.contains(zombieId));
        assertTrue(ids.contains(horseId));

        wave.untrack(zombieId);
        assertEquals(2, ids.size(), "entityIds must be a snapshot, not a live view");
    }

    private static Entity entity(UUID uniqueId) {
        return (Entity) Proxy.newProxyInstance(
            Entity.class.getClassLoader(),
            new Class<?>[]{Entity.class},
            (proxy, method, arguments) -> switch (method.getName()) {
                case "getUniqueId" -> uniqueId;
                case "getLocation" -> new Location(null, 0, 0, 0);
                case "toString" -> "test-entity:" + uniqueId;
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == arguments[0];
                default -> throw new UnsupportedOperationException(method.getName());
            }
        );
    }
}
