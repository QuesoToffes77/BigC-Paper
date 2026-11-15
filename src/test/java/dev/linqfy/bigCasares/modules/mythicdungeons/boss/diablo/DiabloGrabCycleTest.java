package dev.linqfy.bigCasares.modules.mythicdungeons.boss.diablo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiabloGrabCycleTest {

    private static final DiabloGrabCycle.Settings SETTINGS = new DiabloGrabCycle.Settings(10, 5, 100, 20, 8);

    @Test
    void enrageReachGrabThenStunWhenNobodyIsHeld() {
        DiabloGrabCycle cycle = new DiabloGrabCycle(SETTINGS);
        assertEquals(DiabloGrabCycle.Event.NONE, cycle.tick(0, true), "idle before the phase starts");
        cycle.start(0);
        assertEquals(DiabloGrabCycle.Event.NONE, cycle.tick(9, true));
        assertEquals(DiabloGrabCycle.Event.REACH, cycle.tick(10, true));
        assertEquals(DiabloGrabCycle.Event.NONE, cycle.tick(14, true));
        assertEquals(DiabloGrabCycle.Event.GRAB, cycle.tick(15, true));
        assertEquals(1, cycle.grabs());
        assertFalse(cycle.vulnerable());
        assertEquals(DiabloGrabCycle.Event.NONE, cycle.tick(16, false), "someone is still held");
        assertEquals(DiabloGrabCycle.Event.STUN, cycle.tick(40, true));
        assertTrue(cycle.vulnerable());
        assertEquals(20, cycle.stunTicksLeft(40));
        assertEquals(DiabloGrabCycle.Event.NONE, cycle.tick(59, true));
        assertEquals(DiabloGrabCycle.Event.STAND_UP, cycle.tick(60, true));
        assertFalse(cycle.vulnerable());
        assertEquals(DiabloGrabCycle.Event.REACH, cycle.tick(68, true), "repeats until he dies");
        assertEquals(DiabloGrabCycle.Event.GRAB, cycle.tick(73, false));
        assertEquals(2, cycle.grabs());
    }

    @Test
    void holdingTooLongSlamsWithoutStunning() {
        DiabloGrabCycle cycle = new DiabloGrabCycle(SETTINGS);
        cycle.start(0);
        cycle.tick(10, false);
        cycle.tick(15, false);
        assertEquals(DiabloGrabCycle.Event.NONE, cycle.tick(114, false));
        assertEquals(DiabloGrabCycle.Event.SLAM, cycle.tick(115, false));
        assertEquals(DiabloGrabCycle.Stage.RECOVERING, cycle.stage());
        assertFalse(cycle.vulnerable());
        assertEquals(DiabloGrabCycle.Event.REACH, cycle.tick(123, true));
    }

    @Test
    void rejectsInvalidTimings() {
        assertThrows(IllegalArgumentException.class, () -> new DiabloGrabCycle.Settings(0, 0, 10, 10, 0));
        assertThrows(IllegalArgumentException.class, () -> new DiabloGrabCycle.Settings(0, 1, 10, 0, 0));
    }
}
