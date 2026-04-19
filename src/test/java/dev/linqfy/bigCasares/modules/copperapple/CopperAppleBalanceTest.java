package dev.linqfy.bigCasares.modules.copperapple;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CopperAppleBalanceTest {

    @Test
    void keepsCopperAppleAsWeakGoldenAppleProfile() {
        assertEquals(4, CopperAppleBalance.NUTRITION);
        assertEquals(2.4f, CopperAppleBalance.SATURATION);
        assertEquals(600, CopperAppleBalance.ABSORPTION_TICKS);
        assertEquals(60, CopperAppleBalance.REGENERATION_TICKS);
        assertEquals(0, CopperAppleBalance.ABSORPTION_AMPLIFIER);
        assertEquals(0, CopperAppleBalance.REGENERATION_AMPLIFIER);
    }

    @Test
    void keepsAbsorptionLongerThanRegeneration() {
        assertTrue(CopperAppleBalance.ABSORPTION_TICKS > CopperAppleBalance.REGENERATION_TICKS);
    }
}
