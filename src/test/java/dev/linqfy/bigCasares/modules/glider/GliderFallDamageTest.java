package dev.linqfy.bigCasares.modules.glider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GliderFallDamageTest {

    @Test
    void fallDamageReductionMatchesTier() {
        double damage = 20.0;
        assertEquals(13.0, GliderTierStats.defaults(GliderTier.I).reduceFallDamage(damage), 0.0001);
        assertEquals(10.0, GliderTierStats.defaults(GliderTier.II).reduceFallDamage(damage), 0.0001);
        assertEquals(7.0, GliderTierStats.defaults(GliderTier.III).reduceFallDamage(damage), 0.0001);
        assertEquals(4.0, GliderTierStats.defaults(GliderTier.IV).reduceFallDamage(damage), 0.0001);
        assertEquals(2.0, GliderTierStats.defaults(GliderTier.V).reduceFallDamage(damage), 0.0001);
        assertEquals(0.0, GliderTierStats.defaults(GliderTier.VI).reduceFallDamage(damage), 0.0001);
    }
}
