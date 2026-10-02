package dev.linqfy.bigCasares.modules.jeremy;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JeremyLootPolicyTest {

    @Test
    void playerKillReceivesConfiguredReward() {
        JeremyLootSettings settings = new JeremyLootSettings(true, 75, 4, 8, 4, 12, 0.08, 1);
        JeremyLootRoll roll = new JeremyLootPolicy(settings, fixedRandom(0.01, 0))
            .roll(true).orElseThrow();

        assertEquals(4, roll.emeralds());
        assertEquals(4, roll.goldIngots());
        assertEquals(1, roll.diamonds());
        assertEquals(75, roll.experience());
    }

    @Test
    void timeoutLogoutAndAdministrativeRemovalCannotReward() {
        assertTrue(new JeremyLootPolicy(JeremyLootSettings.defaults(), fixedRandom(0.0, 0))
            .roll(false).isEmpty());
    }

    private static Random fixedRandom(double chance, int value) {
        return new Random() {
            @Override public double nextDouble() { return chance; }
            @Override public int nextInt(int bound) { return Math.min(value, bound - 1); }
        };
    }
}
