package dev.linqfy.bigCasares.modules.tombstone;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TombstoneProximityTest {

    @Test
    void findsTheNearestTombstoneWithinThreeBlocks() {
        UUID worldId = UUID.randomUUID();
        TombstoneRecord farther = record(worldId, 2.5, 64.0, 0.0);
        TombstoneRecord nearest = record(worldId, 1.0, 64.0, 0.0);

        assertEquals(nearest.id(), TombstoneProximity.nearest(
            List.of(farther, nearest), worldId, 0.0, 64.0, 0.0, 3.0
        ).orElseThrow().id());
    }

    @Test
    void nearbyTombstonesAreReturnedNearestFirstForSelection() {
        UUID worldId = UUID.randomUUID();
        TombstoneRecord farther = record(worldId, 2.5, 64.0, 0.0);
        TombstoneRecord nearest = record(worldId, 0.5, 64.0, 0.0);

        assertEquals(List.of(nearest, farther), TombstoneProximity.nearby(
            List.of(farther, nearest), worldId, 0.0, 64.0, 0.0, 3.0
        ));
    }

    @Test
    void includesExactlyThreeBlocksAndIgnoresOtherWorlds() {
        UUID worldId = UUID.randomUUID();
        TombstoneRecord boundary = record(worldId, 3.0, 64.0, 0.0);
        TombstoneRecord outside = record(worldId, 3.01, 64.0, 0.0);
        TombstoneRecord otherWorld = record(UUID.randomUUID(), 0.1, 64.0, 0.0);

        assertEquals(boundary.id(), TombstoneProximity.nearest(
            List.of(outside, otherWorld, boundary), worldId, 0.0, 64.0, 0.0, 3.0
        ).orElseThrow().id());
        assertTrue(TombstoneProximity.nearest(
            List.of(outside, otherWorld), worldId, 0.0, 64.0, 0.0, 3.0
        ).isEmpty());
    }

    private static TombstoneRecord record(UUID worldId, double x, double y, double z) {
        return new TombstoneRecord(
            UUID.randomUUID(), UUID.randomUUID(), "Jugador", worldId,
            x, y, z, 0.0f, Instant.parse("2099-01-01T00:00:00Z"),
            List.of(new ItemStack(Material.STONE))
        );
    }
}
