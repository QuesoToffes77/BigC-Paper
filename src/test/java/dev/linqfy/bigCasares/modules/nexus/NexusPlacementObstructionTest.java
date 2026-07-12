package dev.linqfy.bigCasares.modules.nexus;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NexusPlacementObstructionTest {

    private final NexusPlacementPolicy policy = new NexusPlacementPolicy(NexusPlacementSettings.defaults());
    private final NexusBlockPosition origin = new NexusBlockPosition(0, 64, 0);

    @Test
    void rejectsObstructedAnchorHitbox() {
        FakeProbe probe = new FakeProbe();
        probe.put(origin.offset(1, 1, 1), "STONE");

        NexusPlacementResult result = policy.validateNexusPlacement(origin, probe);

        assertEquals(NexusPlacementRejection.HITBOX_OBSTRUCTED, result.rejection());
    }

    @Test
    void requiresAtLeastTwoOpenFaces() {
        FakeProbe probe = new FakeProbe();
        blockFace(probe, 0, -1);
        blockFace(probe, 1, 0);
        blockFace(probe, 0, 1);

        NexusPlacementResult result = policy.validateNexusPlacement(origin, probe);

        assertEquals(NexusPlacementRejection.INSUFFICIENT_OPEN_FACES, result.rejection());
    }

    @Test
    void rejectsOneBlockWideStraightDoorway() {
        FakeProbe probe = new FakeProbe();
        blockFace(probe, 1, 0);
        blockFace(probe, -1, 0);

        NexusPlacementResult result = policy.validateNexusPlacement(origin, probe);

        assertEquals(NexusPlacementRejection.DOORWAY, result.rejection());
    }

    @Test
    void allowsTwoAdjacentOpenFaces() {
        FakeProbe probe = new FakeProbe();
        blockFace(probe, -1, 0);
        blockFace(probe, 0, 1);

        NexusPlacementResult result = policy.validateNexusPlacement(origin, probe);

        assertTrue(result.allowed());
    }

    private void blockFace(FakeProbe probe, int x, int z) {
        probe.put(origin.offset(x * 2, 0, z * 2), "STONE");
        probe.put(origin.offset(x * 2, 1, z * 2), "STONE");
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
