package dev.linqfy.bigCasares.modules.endevent;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HunterDropFreezePolicyTest {

    @Test
    void cooldownLastsSixtyFiveSecondsAndIsReadyAtTheBoundary() {
        Instant usedAt = Instant.parse("2026-07-25T18:00:00Z");
        Instant readyAt = HunterDropFreezePolicy.nextUseAt(usedAt);

        assertEquals(usedAt.plusSeconds(65), readyAt);
        assertFalse(HunterDropFreezePolicy.isReady(readyAt, usedAt.plusSeconds(64)));
        assertTrue(HunterDropFreezePolicy.isReady(readyAt, usedAt.plusSeconds(65)));
    }

    @Test
    void reportsRoundedUpCooldownSeconds() {
        Instant readyAt = Instant.parse("2026-07-25T18:01:05Z");

        assertEquals(2L, HunterDropFreezePolicy.remainingSeconds(
            readyAt, Instant.parse("2026-07-25T18:01:03.100Z")
        ));
        assertEquals(0L, HunterDropFreezePolicy.remainingSeconds(readyAt, readyAt));
    }

    @Test
    void includesTargetsOnTheTwentyFiveBlockBoundaryOnly() {
        assertTrue(HunterDropFreezePolicy.isInRangeSquared(625.0));
        assertFalse(HunterDropFreezePolicy.isInRangeSquared(625.0001));
        assertEquals(Duration.ofSeconds(5), HunterDropFreezePolicy.FREEZE_DURATION);
    }
}
