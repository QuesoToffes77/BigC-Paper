package dev.linqfy.bigCasares.modules.nexus;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NexusContainerClearanceTest {

    private final NexusPlacementPolicy policy = new NexusPlacementPolicy(NexusPlacementSettings.defaults());
    private final NexusBlockPosition nexus = new NexusBlockPosition(0, 64, 0);

    @Test
    void recognizesAllConfiguredContainerFamilies() {
        assertTrue(NexusContainerMaterials.isContainer("CHEST"));
        assertTrue(NexusContainerMaterials.isContainer("minecraft:trapped_chest"));
        assertTrue(NexusContainerMaterials.isContainer("BLUE_SHULKER_BOX"));
        assertTrue(NexusContainerMaterials.isContainer("CRAFTER"));
        assertTrue(NexusContainerMaterials.isContainer("DECORATED_POT"));
        assertFalse(NexusContainerMaterials.isContainer("STONE"));
    }

    @Test
    void rejectsNexusWhenChestTouchesInclusiveClearanceBoundary() {
        FakeProbe probe = new FakeProbe();
        NexusBlockPosition chest = nexus.offset(4, 3, 4);
        probe.put(chest, "CHEST");

        NexusPlacementResult result = policy.validateNexusPlacement(nexus, probe);

        assertEquals(NexusPlacementRejection.CONTAINER_TOO_CLOSE, result.rejection());
        assertEquals(Optional.of(chest), result.blockingPosition());
    }

    @Test
    void allowsNexusWhenContainerIsOutsideClearanceVolume() {
        FakeProbe probe = new FakeProbe();
        probe.put(nexus.offset(5, 0, 0), "BARREL");

        NexusPlacementResult result = policy.validateNexusPlacement(nexus, probe);

        assertTrue(result.allowed());
    }

    @Test
    void preventsContainerPlacementInsideActiveNexusVolume() {
        NexusPlacementResult result = policy.validateContainerPlacement(
                nexus.offset(4, 0, 0),
                List.of(nexus),
                Optional.empty()
        );

        assertEquals(NexusPlacementRejection.PROTECTED_NEXUS_VOLUME, result.rejection());
    }

    @Test
    void preventsDoubleChestFromCrossingProtectedBoundary() {
        NexusPlacementResult result = policy.validateContainerPlacement(
                nexus.offset(5, 0, 0),
                List.of(nexus),
                Optional.of(nexus.offset(4, 0, 0))
        );

        assertEquals(NexusPlacementRejection.PROTECTED_NEXUS_VOLUME, result.rejection());
        assertEquals(Optional.of(nexus.offset(4, 0, 0)), result.blockingPosition());
    }

    @Test
    void preventsPistonMovingContainerIntoProtectedBoundary() {
        NexusPlacementResult result = policy.validatePistonMove(
                nexus.offset(5, 0, 0),
                nexus.offset(4, 0, 0),
                List.of(nexus)
        );

        assertEquals(NexusPlacementRejection.PROTECTED_NEXUS_VOLUME, result.rejection());
    }

    private static final class FakeProbe implements NexusPlacementProbe {
        private final Map<NexusBlockPosition, String> materials = new HashMap<>();

        private void put(NexusBlockPosition position, String material) {
            materials.put(position, material);
        }

        @Override
        public String materialAt(NexusBlockPosition position) {
            return materials.getOrDefault(position, "AIR");
        }
    }
}
