package dev.linqfy.bigCasares.modules.acidrain;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcidRainEnvironmentRateLimitTest {

    private static final Instant START = Instant.parse("2026-08-07T12:00:00Z");

    @Test
    void respectsMaxBlocksPerSecond() {
        AcidRainEnvironmentService service = new AcidRainEnvironmentService(AcidRainSettings.safeDefaults().environment());
        FakeBlockAccess access = new FakeBlockAccess();
        for (int index = 0; index < 120; index++) {
            service.enqueue(candidate(index, 64, 0));
        }

        AcidRainEnvironmentTickResult result = service.processSecond(access, activeStorm(START), START);

        assertEquals(90, result.brokenBlocks());
        assertEquals(90, access.broken.size());
    }

    @Test
    void perSecondBudgetIsSharedAcrossFrequentCycles() {
        AcidRainEnvironmentService service = new AcidRainEnvironmentService(AcidRainSettings.safeDefaults().environment());
        FakeBlockAccess access = new FakeBlockAccess();
        for (int index = 0; index < 300; index++) {
            service.enqueue(candidate(index, 64, 0));
        }

        // CHEMICAL-style 10-tick cadence: two cycles in the same wall-clock second.
        service.processSecond(access, activeStorm(START), START);
        service.processSecond(access, activeStorm(START), START.plusSeconds(0));
        assertEquals(90, access.broken.size());

        // A new second opens a fresh budget, still capped per second.
        service.processSecond(access, activeStorm(START), START.plusSeconds(1));
        assertEquals(180, access.broken.size());
    }

    @Test
    void respectsMaxBlocksPerEvent() {
        AcidRainEnvironmentSettings environment = AcidRainSettings.safeDefaults().environment()
            .withDestruction(AcidRainSettings.safeDefaults().environment().destruction()
                .withMaxBlocksPerEvent(25)
                .withMaxBlocksPerSecond(20));
        AcidRainEnvironmentService service = new AcidRainEnvironmentService(environment);
        FakeBlockAccess access = new FakeBlockAccess();
        for (int index = 0; index < 100; index++) {
            service.enqueue(candidate(index, 64, 0));
        }

        assertEquals(20, service.processSecond(access, activeStorm(START), START).brokenBlocks());
        assertEquals(5, service.processSecond(access, activeStorm(START), START.plusSeconds(1)).brokenBlocks());
        assertEquals(0, service.processSecond(access, activeStorm(START), START.plusSeconds(2)).brokenBlocks());
        assertEquals(25, access.broken.size());
    }

    @Test
    void candidatesOutsideTheRadiusAreNeverBroken() {
        AcidRainEnvironmentService service = new AcidRainEnvironmentService(AcidRainSettings.safeDefaults().environment());
        FakeBlockAccess access = new FakeBlockAccess();
        service.enqueue(new AcidRainErosionCandidate("world", 0, 0, 18, 1000, 64, 1000));

        AcidRainEnvironmentTickResult result = service.processSecond(access, activeStorm(START), START);

        assertEquals(0, result.brokenBlocks());
        assertEquals(1, result.skippedBlocks());
        assertTrue(access.broken.isEmpty());
    }

    @Test
    void warningEndingAndInactiveStormsNeverBreakBlocks() {
        AcidRainEnvironmentService service = new AcidRainEnvironmentService(AcidRainSettings.safeDefaults().environment());
        FakeBlockAccess access = new FakeBlockAccess();
        service.enqueue(candidate(1, 64, 0));

        AcidRainEnvironmentTickResult warning = service.processSecond(access, storm(AcidRainState.WARNING, START), START);
        AcidRainEnvironmentTickResult ending = service.processSecond(access, storm(AcidRainState.ENDING, START), START);
        AcidRainEnvironmentTickResult inactive = service.processSecond(access, storm(AcidRainState.INACTIVE, START), START);

        assertEquals(0, warning.brokenBlocks());
        assertEquals(0, ending.brokenBlocks());
        assertEquals(0, inactive.brokenBlocks());
        assertTrue(access.broken.isEmpty());

        assertEquals(1, service.processSecond(access, activeStorm(START), START).brokenBlocks());
    }

    @Test
    void safetyPolicyIsAppliedAtProcessingTime() {
        AcidRainEnvironmentService service = new AcidRainEnvironmentService(AcidRainSettings.safeDefaults().environment());
        FakeBlockAccess access = new FakeBlockAccess();
        access.material = Material.OAK_LEAVES;
        service.enqueue(candidate(1, 64, 1));

        AcidRainEnvironmentTickResult result = service.processSecond(access, activeStorm(START), START);

        assertEquals(0, result.brokenBlocks());
        assertEquals(1, result.skippedBlocks());
    }

    @Test
    void duplicateCandidatesAreDeduplicated() {
        AcidRainEnvironmentService service = new AcidRainEnvironmentService(AcidRainSettings.safeDefaults().environment());
        FakeBlockAccess access = new FakeBlockAccess();

        assertTrue(service.enqueue(candidate(1, 64, 1)));
        assertTrue(!service.enqueue(candidate(1, 64, 1)));

        assertEquals(1, service.queuedCandidates());
    }

    @Test
    void reloadWhitelistChangesAreRespectedBySubsequentCycles() {
        AcidRainEnvironmentService service = new AcidRainEnvironmentService(AcidRainSettings.safeDefaults().environment());
        FakeBlockAccess access = new FakeBlockAccess();
        service.enqueue(candidate(1, 64, 0));
        assertEquals(1, service.processSecond(access, activeStorm(START), START).brokenBlocks());

        // Simulates /acidrain reload removing STONE from the whitelist: the
        // protection applies immediately to the next cycle.
        AcidRainEnvironmentSettings tightened = AcidRainSettings.safeDefaults().environment()
            .withDestruction(AcidRainSettings.safeDefaults().environment().destruction()
                .withWhitelist(java.util.Set.of(Material.OAK_PLANKS)));
        service.updateSettings(tightened);
        service.enqueue(candidate(2, 64, 0));

        AcidRainEnvironmentTickResult result = service.processSecond(access, activeStorm(START), START.plusSeconds(1));

        assertEquals(0, result.brokenBlocks());
        assertEquals(1, result.skippedBlocks());
    }

    private static AcidRainErosionCandidate candidate(int x, int y, int z) {
        return new AcidRainErosionCandidate("world", x, 0, 18, x, y, z);
    }

    private static AcidRainSnapshot activeStorm(Instant at) {
        return storm(AcidRainState.ACTIVE, at);
    }

    private static AcidRainSnapshot storm(AcidRainState state, Instant at) {
        return new AcidRainSnapshot(
            state,
            AcidRainLevel.ACID,
            at,
            at.plusSeconds(300),
            300,
            300,
            AcidRainSettings.safeDefaults().worlds()
        );
    }

    private static final class FakeBlockAccess implements AcidRainBlockAccess {
        private Material material = Material.STONE;
        private final List<AcidRainErosionCandidate> broken = new ArrayList<>();

        @Override
        public Material materialAt(AcidRainErosionCandidate candidate) {
            return material;
        }

        @Override
        public boolean canBreak(AcidRainErosionCandidate candidate) {
            return true;
        }

        @Override
        public void breakBlock(AcidRainErosionCandidate candidate) {
            broken.add(candidate);
        }
    }
}
