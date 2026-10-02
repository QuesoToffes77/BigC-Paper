package dev.linqfy.bigCasares.modules.glider;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GliderBoostCooldownTest {

    @Test
    void boostCooldownWorksAcrossSessions() {
        UUID player = UUID.randomUUID();
        GliderBoostCooldown cooldown = new GliderBoostCooldown();

        assertTrue(cooldown.tryUse(player, 100L, 80));
        assertFalse(cooldown.tryUse(player, 179L, 80));
        assertTrue(cooldown.tryUse(player, 180L, 80));
    }

    @Test
    void tiersOneAndTwoCannotBoost() {
        GliderBoostCooldown cooldown = new GliderBoostCooldown();
        UUID player = UUID.randomUUID();

        assertFalse(cooldown.tryUse(player, 10L, GliderTierStats.defaults(GliderTier.I)));
        assertFalse(cooldown.tryUse(player, 10L, GliderTierStats.defaults(GliderTier.II)));
        assertTrue(cooldown.tryUse(player, 10L, GliderTierStats.defaults(GliderTier.III)));
    }
}
