package dev.linqfy.bigCasares.modules.glider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GliderTierTest {

    @Test
    void tierIdentificationWorks() {
        assertEquals(6, GliderTier.values().length);
        for (int index = 0; index < GliderTier.values().length; index++) {
            GliderTier tier = GliderTier.values()[index];
            assertEquals(index + 1, tier.number());
            assertEquals("glider_tier_" + (index + 1), tier.catalogId());
            assertEquals(tier, GliderTier.fromCatalogId(tier.catalogId()).orElseThrow());
        }
    }

    @Test
    void tierSixHasHighestDefaultStats() {
        GliderTierStats previous = null;
        for (GliderTier tier : GliderTier.values()) {
            GliderTierStats current = GliderTierStats.defaults(tier);
            if (previous != null) {
                assertTrue(current.forwardSpeed() > previous.forwardSpeed());
                assertTrue(current.steering() > previous.steering());
                assertTrue(current.maxFallSpeed() > previous.maxFallSpeed());
                assertTrue(current.fallDamageReduction() > previous.fallDamageReduction());
            }
            previous = current;
        }
        assertEquals(1.0, previous.fallDamageReduction());
    }
}
