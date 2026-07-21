package dev.linqfy.bigCasares.modules.pveboss;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BossRaidScalingTest {

    @ParameterizedTest
    @CsvSource({"0,60000", "10,60000", "15,80000", "20,100000", "35,100000"})
    void sahurHealthClampsRaidBetweenTenAndTwenty(int players, double expected) {
        BossRaidScaling scaling = new BossRaidScaling(20_000.0, 4_000.0, 10, 20);

        assertEquals(expected, scaling.maximumHealth(players));
    }

    @ParameterizedTest
    @CsvSource({"0,10", "10,10", "15,15", "20,20", "35,20"})
    void reportsTheClampedParticipantSnapshot(int players, int expected) {
        BossRaidScaling scaling = new BossRaidScaling(20_000.0, 4_000.0, 10, 20);

        assertEquals(expected, scaling.clampedParticipants(players));
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0, -1.0})
    void rejectsNonFiniteOrNonPositiveBaseHealth(double baseHealth) {
        assertThrows(IllegalArgumentException.class, () -> new BossRaidScaling(baseHealth, 4_000.0, 10, 20));
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -1.0})
    void rejectsNonFiniteOrNegativeHealthPerPlayer(double healthPerPlayer) {
        assertThrows(IllegalArgumentException.class, () -> new BossRaidScaling(20_000.0, healthPerPlayer, 10, 20));
    }

    @Test
    void permitsZeroHealthPerPlayer() {
        BossRaidScaling scaling = new BossRaidScaling(20_000.0, 0.0, 10, 20);

        assertEquals(20_000.0, scaling.maximumHealth(10));
    }

    @Test
    void rejectsMinimumBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> new BossRaidScaling(20_000.0, 4_000.0, 0, 20));
    }

    @Test
    void rejectsMaximumBelowMinimum() {
        assertThrows(IllegalArgumentException.class, () -> new BossRaidScaling(20_000.0, 4_000.0, 20, 19));
    }

    @Test
    void rejectsNegativeActualParticipantCount() {
        BossRaidScaling scaling = new BossRaidScaling(20_000.0, 4_000.0, 10, 20);

        assertThrows(IllegalArgumentException.class, () -> scaling.clampedParticipants(-1));
        assertThrows(IllegalArgumentException.class, () -> scaling.maximumHealth(-1));
    }

    @Test
    void rejectsAComputedHealthValueThatOverflows() {
        BossRaidScaling scaling = new BossRaidScaling(Double.MAX_VALUE, Double.MAX_VALUE, 1, 2);

        assertThrows(IllegalStateException.class, () -> scaling.maximumHealth(2));
    }
}
