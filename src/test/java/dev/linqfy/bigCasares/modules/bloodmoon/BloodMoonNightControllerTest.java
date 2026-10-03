package dev.linqfy.bigCasares.modules.bloodmoon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BloodMoonNightControllerTest {

    @Test
    void bloodMoonStartsAtNightAndEndsAtDay() {
        BloodMoonNightController controller = new BloodMoonNightController(
            new BloodMoonSchedulingSettings(BloodMoonSchedulingMode.CHANCE, 1.0, 0, 5)
        );

        assertEquals(BloodMoonTransition.STARTED, controller.observe(13_000L, 0.99, false));
        assertTrue(controller.active());
        assertEquals(BloodMoonTransition.STOPPED, controller.observe(23_000L, 0.0, false));
        assertFalse(controller.active());
    }

    @Test
    void bloodMoonDoesNotStartTwiceDuringTheSameNight() {
        BloodMoonNightController controller = new BloodMoonNightController(
            new BloodMoonSchedulingSettings(BloodMoonSchedulingMode.CHANCE, 1.0, 0, 5)
        );

        assertEquals(BloodMoonTransition.STARTED, controller.observe(13_000L, 0.0, false));
        assertEquals(BloodMoonTransition.NONE, controller.observe(18_000L, 0.0, false));
        assertEquals(BloodMoonTransition.NONE, controller.observe(13_500L, 0.0, false));
    }

    @Test
    void minimumNormalNightsAreRespectedAfterAnEvent() {
        BloodMoonNightController controller = new BloodMoonNightController(
            new BloodMoonSchedulingSettings(BloodMoonSchedulingMode.CHANCE, 1.0, 2, 5)
        );

        assertEquals(BloodMoonTransition.STARTED, controller.observe(13_000L, 0.0, false));
        assertEquals(BloodMoonTransition.STOPPED, controller.observe(23_000L, 0.0, false));
        assertEquals(BloodMoonTransition.NONE, controller.observe(37_000L, 0.0, false));
        assertEquals(BloodMoonTransition.NONE, controller.observe(61_000L, 0.0, false));
        assertEquals(BloodMoonTransition.STARTED, controller.observe(85_000L, 0.0, false));
    }

    @Test
    void intervalModeStartsOnlyOnConfiguredNight() {
        BloodMoonNightController controller = new BloodMoonNightController(
            new BloodMoonSchedulingSettings(BloodMoonSchedulingMode.INTERVAL, 0.0, 0, 3)
        );

        assertEquals(BloodMoonTransition.NONE, controller.observe(13_000L, 0.0, false));
        assertEquals(BloodMoonTransition.NONE, controller.observe(37_000L, 0.0, false));
        assertEquals(BloodMoonTransition.STARTED, controller.observe(61_000L, 0.0, false));
    }

    @Test
    void conflictBlocksAutomaticStartAndManualStartRequiresNight() {
        BloodMoonNightController controller = new BloodMoonNightController(
            new BloodMoonSchedulingSettings(BloodMoonSchedulingMode.CHANCE, 1.0, 0, 5)
        );

        assertEquals(BloodMoonTransition.NONE, controller.observe(13_000L, 0.0, true));
        assertFalse(controller.forceStart(6_000L));
        assertTrue(controller.forceStart(14_000L));
    }
}
