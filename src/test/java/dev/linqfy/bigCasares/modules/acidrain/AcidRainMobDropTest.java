package dev.linqfy.bigCasares.modules.acidrain;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure tests for the toxic mob {@code nitric_acid} drop roll and its settings
 * record: disabled settings yield nothing, chance gates the roll, and the
 * amount stays inside the configured bounds.
 */
class AcidRainMobDropTest {

    @Test
    void disabledDropSettingsNeverDrop() {
        assertEquals(0, AcidRainMobDrops.rollAmount(AcidRainMobDropSettings.disabled(), new Random(1)));
        assertEquals(0, AcidRainMobDrops.rollAmount(
            new AcidRainMobDropSettings(false, 100, 1, 2), new Random(1)));
    }

    @Test
    void zeroChanceNeverDropsAndFullChanceAlwaysRollsAtLeastMinimum() {
        Random random = new Random(42);
        AcidRainMobDropSettings never = new AcidRainMobDropSettings(true, 0, 1, 3);
        for (int i = 0; i < 50; i++) {
            assertEquals(0, AcidRainMobDrops.rollAmount(never, random));
        }

        AcidRainMobDropSettings always = new AcidRainMobDropSettings(true, 100, 1, 3);
        for (int i = 0; i < 50; i++) {
            int amount = AcidRainMobDrops.rollAmount(always, random);
            assertTrue(amount >= 1 && amount <= 3, "amount out of bounds: " + amount);
        }
    }

    @Test
    void fixedMinMaxAmountIsConstant() {
        AcidRainMobDropSettings fixed = new AcidRainMobDropSettings(true, 100, 2, 2);
        Random random = new Random(7);
        for (int i = 0; i < 20; i++) {
            assertEquals(2, AcidRainMobDrops.rollAmount(fixed, random));
        }
    }

    @Test
    void recordNormalizesChanceAndAmountBounds() {
        AcidRainMobDropSettings clamped = new AcidRainMobDropSettings(true, 999, -4, 5);
        assertEquals(100, clamped.chancePercent());
        assertEquals(0, clamped.minAmount());
        assertEquals(5, clamped.maxAmount());

        AcidRainMobDropSettings reversed = new AcidRainMobDropSettings(true, 50, 4, 1);
        assertTrue(reversed.maxAmount() >= reversed.minAmount());

        AcidRainMobDropSettings defaults = AcidRainMobDropSettings.defaults();
        assertTrue(defaults.enabled());
        assertEquals(100, defaults.chancePercent());
        assertEquals(1, defaults.minAmount());
        assertEquals(2, defaults.maxAmount());

        assertFalse(AcidRainMobDropSettings.disabled().enabled());
    }
}
