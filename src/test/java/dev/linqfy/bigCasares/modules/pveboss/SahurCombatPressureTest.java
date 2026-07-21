package dev.linqfy.bigCasares.modules.pveboss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

class SahurCombatPressureTest {
    private final SahurCombatPressure pressure =
        new SahurCombatPressure(Duration.ofSeconds(2), 4, Duration.ofMillis(500));

    @Test
    void spinTriggersForFourNearbyPlayersOrFourRecentCriticalHits() {
        assertTrue(pressure.shouldSpin(4, List.of(), t(10_000)));
        assertTrue(pressure.shouldSpin(1, List.of(t(8_100), t(8_500), t(9_000), t(10_000)), t(10_000)));
        assertFalse(pressure.shouldSpin(3, List.of(t(8_100), t(8_500), t(9_000)), t(10_000)));
        assertFalse(pressure.shouldSpin(3, List.of(t(0), t(200), t(500), t(1_900)), t(10_000)));
    }

    @Test
    void comboUsesThirtyFiveThenFifteenPercentContinuationRollsAndCapsAtThree() {
        assertEquals(1, pressure.comboLength(random(0.35)));
        assertEquals(2, pressure.comboLength(random(0.34, 0.15)));
        assertEquals(3, pressure.comboLength(random(0.34, 0.14)));
    }

    @Test
    void hitCooldownIsTrackedIndependentlyPerPlayer() {
        UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID second = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Instant start = Instant.parse("2026-07-18T00:00:00Z");

        assertTrue(pressure.mayHit(first, start));
        assertFalse(pressure.mayHit(first, start.plusMillis(499)));
        assertTrue(pressure.mayHit(second, start.plusMillis(499)));
        assertTrue(pressure.mayHit(first, start.plusMillis(500)));
    }

    private static Instant t(long millis) {
        return Instant.EPOCH.plusMillis(millis);
    }

    private static RandomGenerator random(double... values) {
        return new RandomGenerator() {
            private int index;

            @Override
            public long nextLong() {
                return 0L;
            }

            @Override
            public double nextDouble() {
                if (index >= values.length) {
                    throw new AssertionError("unexpected continuation roll");
                }
                return values[index++];
            }
        };
    }
}
