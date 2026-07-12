package dev.linqfy.bigCasares.modules.nexus;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NexusVisualRecoveryTest {

    private final NexusVisualRecoveryPlanner planner = new NexusVisualRecoveryPlanner(2);

    @Test
    void plansMissingVisualReconstructionAfterRestart() {
        NexusVisualRequest desired = request(NexusId.random());

        NexusVisualRecoveryPlan plan = planner.plan(List.of(desired), List.of());

        assertEquals(Set.of(desired), plan.toSpawn());
        assertTrue(plan.toRemove().isEmpty());
    }

    @Test
    void replacesIncompleteCompositeVisual() {
        NexusVisualRequest desired = request(NexusId.random());
        NexusVisualHandle incomplete = new NexusVisualHandle(
                desired.nexusId(),
                UUID.randomUUID(),
                Set.of(UUID.randomUUID())
        );

        NexusVisualRecoveryPlan plan = planner.plan(List.of(desired), List.of(incomplete));

        assertEquals(Set.of(desired), plan.toSpawn());
        assertEquals(Set.of(desired.nexusId()), plan.toRemove());
    }

    @Test
    void leavesCompleteVisualUntouchedAndRemovesOrphans() {
        NexusVisualRequest desired = request(NexusId.random());
        NexusVisualHandle complete = new NexusVisualHandle(
                desired.nexusId(),
                UUID.randomUUID(),
                Set.of(UUID.randomUUID(), UUID.randomUUID())
        );
        NexusId orphanId = NexusId.random();
        NexusVisualHandle orphan = new NexusVisualHandle(
                orphanId,
                UUID.randomUUID(),
                Set.of(UUID.randomUUID(), UUID.randomUUID())
        );

        NexusVisualRecoveryPlan plan = planner.plan(List.of(desired), List.of(complete, orphan));

        assertTrue(plan.toSpawn().isEmpty());
        assertEquals(Set.of(orphanId), plan.toRemove());
    }

    private static NexusVisualRequest request(NexusId id) {
        return new NexusVisualRequest(
                id,
                new NexusPosition(UUID.randomUUID(), 10.5, 65.0, -4.5, 0.0f),
                "GDS",
                500.0,
                500.0,
                "bigcasares:nexus"
        );
    }
}
