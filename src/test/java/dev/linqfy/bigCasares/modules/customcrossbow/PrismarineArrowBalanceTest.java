package dev.linqfy.bigCasares.modules.customcrossbow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrismarineArrowBalanceTest {

    @Test
    void underwaterPrismarineArrowGetsFastAndStronger() {
        assertTrue(PrismarineArrowBalance.velocityMultiplier(true) > 3.0);
        assertEquals(7.8, PrismarineArrowBalance.damage(6.0, true), 0.0001);
    }

    @Test
    void dryPrismarineArrowIsSlightlyWeakerThanNormal() {
        assertEquals(0.85, PrismarineArrowBalance.velocityMultiplier(false), 0.0001);
        assertEquals(5.1, PrismarineArrowBalance.damage(6.0, false), 0.0001);
    }
}
