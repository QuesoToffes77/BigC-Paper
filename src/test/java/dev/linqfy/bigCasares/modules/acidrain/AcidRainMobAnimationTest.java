package dev.linqfy.bigCasares.modules.acidrain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcidRainMobAnimationTest {

    @Test
    void movementSelectsWalkAndStationaryMobsUseIdle() {
        assertEquals(AcidRainMobAnimation.IDLE, AcidRainMobAnimation.loopFor(0.0));
        assertEquals(AcidRainMobAnimation.IDLE, AcidRainMobAnimation.loopFor(0.001));
        assertEquals(AcidRainMobAnimation.WALK, AcidRainMobAnimation.loopFor(0.02));
    }

    @Test
    void attackAndHurtAreBoundedOneShots() {
        assertFalse(AcidRainMobAnimation.IDLE.oneShot());
        assertFalse(AcidRainMobAnimation.WALK.oneShot());
        assertTrue(AcidRainMobAnimation.ATTACK.oneShot());
        assertTrue(AcidRainMobAnimation.HURT.oneShot());
        assertTrue(AcidRainMobAnimation.ATTACK.lockTicks() > AcidRainMobAnimation.HURT.lockTicks());
        assertTrue(AcidRainMobAnimation.HURT.lockTicks() > 0L);
    }
}
