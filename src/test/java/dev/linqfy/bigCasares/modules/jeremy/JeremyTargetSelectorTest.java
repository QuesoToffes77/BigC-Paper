package dev.linqfy.bigCasares.modules.jeremy;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JeremyTargetSelectorTest {
    private final JeremyTargetingSettings settings = new JeremyTargetingSettings(
        true, true, false, false, true, Set.of("world"), Set.of("excluded")
    );

    @Test
    void jeremySelectsEligibleRandomPlayer() {
        UUID eligible = UUID.randomUUID();
        JeremyTargetSelector selector = new JeremyTargetSelector(settings, new Random(1));

        assertEquals(eligible, selector.select(List.of(candidate(eligible, JeremyPlayerMode.SURVIVAL, "world")), null).orElseThrow());
    }

    @Test
    void jeremyAvoidsLastTargetWhenPossible() {
        UUID previous = UUID.randomUUID();
        UUID next = UUID.randomUUID();
        JeremyTargetSelector selector = new JeremyTargetSelector(settings, new Random(1));

        UUID selected = selector.select(List.of(
            candidate(previous, JeremyPlayerMode.SURVIVAL, "world"),
            candidate(next, JeremyPlayerMode.ADVENTURE, "world")
        ), previous).orElseThrow();

        assertNotEquals(previous, selected);
        assertEquals(next, selected);
    }

    @Test
    void jeremyNeverTargetsSpectatorOrCreativeByDefault() {
        JeremyTargetSelector selector = new JeremyTargetSelector(settings, new Random(1));

        assertTrue(selector.select(List.of(
            candidate(UUID.randomUUID(), JeremyPlayerMode.SPECTATOR, "world"),
            candidate(UUID.randomUUID(), JeremyPlayerMode.CREATIVE, "world")
        ), null).isEmpty());
    }

    @Test
    void offlineDeadNpcUnloadedAndExcludedPlayersAreRejected() {
        JeremyTargetSelector selector = new JeremyTargetSelector(settings, new Random(1));
        UUID id = UUID.randomUUID();

        assertTrue(selector.select(List.of(
            new JeremyTargetCandidate(id, false, false, false, true, JeremyPlayerMode.SURVIVAL, "world"),
            new JeremyTargetCandidate(id, true, true, false, true, JeremyPlayerMode.SURVIVAL, "world"),
            new JeremyTargetCandidate(id, true, false, true, true, JeremyPlayerMode.SURVIVAL, "world"),
            new JeremyTargetCandidate(id, true, false, false, false, JeremyPlayerMode.SURVIVAL, "world"),
            candidate(id, JeremyPlayerMode.SURVIVAL, "excluded")
        ), null).isEmpty());
    }

    private static JeremyTargetCandidate candidate(UUID id, JeremyPlayerMode mode, String world) {
        return new JeremyTargetCandidate(id, true, false, false, true, mode, world);
    }
}
