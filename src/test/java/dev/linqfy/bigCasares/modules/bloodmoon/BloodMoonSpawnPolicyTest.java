package dev.linqfy.bigCasares.modules.bloodmoon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BloodMoonSpawnPolicyTest {

    @Test
    void spawnDistanceMustStayInsideConfiguredRange() {
        assertFalse(BloodMoonSpawnPolicy.distanceAllowed(23.99, 24, 56));
        assertTrue(BloodMoonSpawnPolicy.distanceAllowed(24.0, 24, 56));
        assertTrue(BloodMoonSpawnPolicy.distanceAllowed(56.0, 24, 56));
        assertFalse(BloodMoonSpawnPolicy.distanceAllowed(56.01, 24, 56));
    }

    @Test
    void extraSpawnNeverExceedsPerPlayerOrWorldLimits() {
        BloodMoonSpawnLedger ledger = new BloodMoonSpawnLedger();

        assertTrue(ledger.tryReserve("player-a", "mob-1", 2, 3));
        assertTrue(ledger.tryReserve("player-a", "mob-2", 2, 3));
        assertFalse(ledger.tryReserve("player-a", "mob-3", 2, 3));
        assertTrue(ledger.tryReserve("player-b", "mob-3", 2, 3));
        assertFalse(ledger.tryReserve("player-b", "mob-4", 2, 3));

        ledger.release("mob-2");
        assertTrue(ledger.tryReserve("player-b", "mob-4", 2, 3));
        assertEquals(3, ledger.worldCount());
    }

    @Test
    void vanillaPressureCeilingAndMultiplierAreRespected() {
        BloodMoonSpawnPressure pressure = new BloodMoonSpawnPressure(4, 9, 139, 70);
        BloodMoonSpawningSettings settings = BloodMoonSpawningSettings.defaults();

        assertTrue(BloodMoonSpawnPolicy.pressureAllows(settings, pressure));
        assertFalse(BloodMoonSpawnPolicy.pressureAllows(settings,
            new BloodMoonSpawnPressure(4, 9, 140, 70)));
        assertEquals(1, BloodMoonSpawnPolicy.extraAttemptsPerPlayer(2.0));
        assertEquals(0, BloodMoonSpawnPolicy.extraAttemptsPerPlayer(1.0));
    }
}
