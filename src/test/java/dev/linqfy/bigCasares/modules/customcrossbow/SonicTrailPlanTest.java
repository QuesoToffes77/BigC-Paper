package dev.linqfy.bigCasares.modules.customcrossbow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SonicTrailPlanTest {

    @Test
    void sonicTrailIsBoundedAcrossShortAnimation() {
        SonicTrailPlan plan = SonicTrailPlan.create(32.0);

        assertEquals(5, plan.ticks());
        assertTrue(plan.pointsPerTick() <= 8);
        assertTrue(plan.totalParticlePoints() <= 40);
    }
}
