package dev.linqfy.bigCasares.modules.jeremy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JeremyUltrasoundTest {

    @Test
    void stuckJeremyCanUseUltrasoundAndCooldownIsRespected() {
        JeremyUltrasoundController controller = new JeremyUltrasoundController(30, 100);

        assertTrue(controller.beginCharge(0, true));
        assertEquals(JeremyUltrasoundTransition.NONE, controller.advance(29, true, true));
        assertEquals(JeremyUltrasoundTransition.FIRE, controller.advance(30, true, true));
        assertFalse(controller.beginCharge(99, true));
        assertTrue(controller.beginCharge(130, true));
    }

    @Test
    void progressCancelsCharge() {
        JeremyUltrasoundController controller = new JeremyUltrasoundController(30, 100);
        controller.beginCharge(0, true);

        assertEquals(JeremyUltrasoundTransition.CANCELLED, controller.advance(10, false, true));
        assertEquals(JeremyUltrasoundPhase.IDLE, controller.phase());
    }

    @Test
    void ultrasoundRespectsItsOwnRangeAndHuntConditions() {
        assertTrue(JeremyUltrasoundPolicy.canCharge(true, true, true, true, 31.9, 32.0, true));
        assertFalse(JeremyUltrasoundPolicy.canCharge(true, true, true, true, 32.1, 32.0, true));
        assertFalse(JeremyUltrasoundPolicy.canCharge(false, true, true, true, 5.0, 32.0, true));
    }

    @Test
    void ultrasoundUsesJeremyToTargetDirection() {
        JeremyVector direction = JeremyUltrasoundPolicy.direction(
            new JeremyVector(1, 2, 3), new JeremyVector(4, 6, 3));

        assertEquals(0.6, direction.x(), 0.0001);
        assertEquals(0.8, direction.y(), 0.0001);
        assertEquals(0.0, direction.z(), 0.0001);
    }

    @Test
    void ultrasoundCannotPenetrateMoreThanConfiguredBlocks() {
        JeremyWallPenetrationSettings walls = new JeremyWallPenetrationSettings(true, 2);

        assertTrue(JeremyUltrasoundPolicy.canReach(false, 2, walls));
        assertFalse(JeremyUltrasoundPolicy.canReach(false, 3, walls));
        assertTrue(JeremyUltrasoundPolicy.canReach(true, 100, walls));
    }
}
