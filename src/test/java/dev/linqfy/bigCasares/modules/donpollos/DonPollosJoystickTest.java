package dev.linqfy.bigCasares.modules.donpollos;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonPollosJoystickTest {

    private static final Set<DonPollosDanceMove> UP_LEFT = EnumSet.of(DonPollosDanceMove.FORWARD, DonPollosDanceMove.LEFT);

    @Test
    void onlyAPerpendicularPairIsAJoystickDiagonal() {
        assertTrue(DonPollosJoystick.isDiagonal(UP_LEFT));
        assertTrue(DonPollosJoystick.isDiagonal(EnumSet.of(DonPollosDanceMove.BACKWARD, DonPollosDanceMove.RIGHT)));
        // opuestas, una sola, tres o saltar con otra: no es joystick, sigue siendo error
        assertFalse(DonPollosJoystick.isDiagonal(EnumSet.of(DonPollosDanceMove.FORWARD, DonPollosDanceMove.BACKWARD)));
        assertFalse(DonPollosJoystick.isDiagonal(EnumSet.of(DonPollosDanceMove.LEFT, DonPollosDanceMove.RIGHT)));
        assertFalse(DonPollosJoystick.isDiagonal(EnumSet.of(DonPollosDanceMove.FORWARD)));
        assertFalse(DonPollosJoystick.isDiagonal(EnumSet.of(DonPollosDanceMove.FORWARD, DonPollosDanceMove.LEFT,
            DonPollosDanceMove.RIGHT)));
        assertFalse(DonPollosJoystick.isDiagonal(EnumSet.of(DonPollosDanceMove.JUMP, DonPollosDanceMove.LEFT)));
        assertFalse(DonPollosJoystick.isDiagonal(EnumSet.allOf(DonPollosDanceMove.class)));
    }

    @Test
    void worldMovementIsSeenFromWhereThePlayerLooks() {
        // mirando al sur (yaw 0): ir a +Z es adelante, ir a +X (este) es a la izquierda
        assertLocal(1, 0, DonPollosJoystick.local(0, 1, 0));
        assertLocal(0, 1, DonPollosJoystick.local(1, 0, 0));
        // mirando al oeste (yaw 90): ir a -X es adelante, ir a +Z (sur) es a la izquierda
        assertLocal(1, 0, DonPollosJoystick.local(-1, 0, 90));
        assertLocal(0, 1, DonPollosJoystick.local(0, 1, 90));
    }

    @Test
    void theDirectionTheStickMostlyPointedWins() {
        assertEquals(DonPollosDanceMove.LEFT, DonPollosJoystick.dominant(UP_LEFT, 0.05, 0.2));
        assertEquals(DonPollosDanceMove.FORWARD, DonPollosJoystick.dominant(UP_LEFT, 0.2, 0.05));
    }

    @Test
    void withoutMovingThereIsNoWayToKnow() {
        assertNull(DonPollosJoystick.dominant(UP_LEFT, 0.001, 0.001));
    }

    private static void assertLocal(double forward, double left, double[] local) {
        assertEquals(forward, local[0], 1e-9);
        assertEquals(left, local[1], 1e-9);
    }
}
