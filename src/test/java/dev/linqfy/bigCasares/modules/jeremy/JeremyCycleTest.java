package dev.linqfy.bigCasares.modules.jeremy;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JeremyCycleTest {
    private static final JeremyTimingSettings TIMING = new JeremyTimingSettings(
        420_000L, 3_600_000L, 60_000L, 5_000L, 4_000L
    );

    @Test
    void jeremyStartsAfterRestPeriod() {
        JeremyCycle cycle = JeremyCycle.resting(TIMING, 10_000L, null);

        assertFalse(cycle.canStart(9_999L));
        assertTrue(cycle.canStart(10_000L));
        assertTrue(cycle.startHunt(UUID.randomUUID(), 10_000L));
        assertEquals(JeremyPhase.HUNTING, cycle.phase());
    }

    @Test
    void onlyOneJeremyCanExist() {
        JeremyCycle cycle = JeremyCycle.resting(TIMING, 0L, null);

        assertTrue(cycle.startHunt(UUID.randomUUID(), 0L));
        assertFalse(cycle.startHunt(UUID.randomUUID(), 1L));
    }

    @Test
    void huntLastsSevenMinutesAndTimeoutStartsOneHourRest() {
        JeremyCycle cycle = JeremyCycle.resting(TIMING, 0L, null);
        cycle.startHunt(UUID.randomUUID(), 1_000L);

        assertEquals(JeremyTransition.NONE, cycle.advance(420_999L));
        assertEquals(JeremyTransition.HUNT_TIMEOUT, cycle.advance(421_000L));
        cycle.finishHunt(421_000L);

        assertEquals(JeremyPhase.RESTING, cycle.phase());
        assertEquals(4_021_000L, cycle.restUntilMillis());
    }

    @Test
    void noPlayerRetryDoesNotStartTheFullRestAgain() {
        JeremyCycle cycle = JeremyCycle.resting(TIMING, 0L, null);

        cycle.retryWithoutPlayer(5_000L);

        assertEquals(65_000L, cycle.restUntilMillis());
    }

    @Test
    void jeremyCelebratesAfterKillingTargetAndRestStartsAfterCelebration() {
        JeremyCycle cycle = JeremyCycle.resting(TIMING, 0L, null);
        cycle.startHunt(UUID.randomUUID(), 0L);

        assertTrue(cycle.startCelebration(2_000L));
        assertEquals(JeremyPhase.CELEBRATING, cycle.phase());
        assertEquals(JeremyTransition.NONE, cycle.advance(5_999L));
        assertEquals(JeremyTransition.CELEBRATION_FINISHED, cycle.advance(6_000L));

        cycle.finishCelebration(6_000L);
        assertEquals(JeremyPhase.RESTING, cycle.phase());
        assertEquals(3_606_000L, cycle.restUntilMillis());
    }

    @Test
    void targetLogoutAndJeremyDeathEndHunt() {
        JeremyCycle cycle = JeremyCycle.resting(TIMING, 0L, null);
        cycle.startHunt(UUID.randomUUID(), 0L);
        cycle.finishHunt(20_000L);
        assertEquals(3_620_000L, cycle.restUntilMillis());

        cycle.resetCooldown(30_000L);
        cycle.startHunt(UUID.randomUUID(), 30_000L);
        cycle.finishHunt(31_000L);
        assertEquals(3_631_000L, cycle.restUntilMillis());
    }
}
