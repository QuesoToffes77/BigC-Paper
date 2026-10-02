package dev.linqfy.bigCasares.modules.grapplinghook;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrappleMovementTest {

    @Test
    void reelKeepsExistingTangentialSwingMomentum() {
        Vector result = GrappleMovement.controlledVelocity(
            new Vector(0.0, 0.0, 1.25), new Vector(3.0, 0.0, 0.0), 0.0f,
            false, false, false, false, false, 0.22, 2.4, 0.7
        );

        assertEquals(3.0, result.getX(), 1.0e-9);
        assertEquals(1.25, result.getZ(), 1.0e-9);
    }

    @Test
    void movementInputAddsStrongCameraRelativeAirControl() {
        Vector result = GrappleMovement.controlledVelocity(
            new Vector(), new Vector(1.5, 0.0, 0.0), 0.0f,
            true, false, false, false, false, 0.22, 2.4, 0.7
        );

        assertEquals(1.5, result.getX(), 1.0e-9);
        assertEquals(0.22, result.getZ(), 1.0e-9);
    }

    @Test
    void jumpRaisesThePlayerWithoutRemovingTheReelOrSwing() {
        Vector result = GrappleMovement.controlledVelocity(
            new Vector(0.0, -0.4, 0.8), new Vector(1.8, 0.0, 0.0), 90.0f,
            false, false, false, false, true, 0.22, 2.4, 0.7
        );

        assertEquals(1.8, result.getX(), 1.0e-9);
        assertEquals(0.8, result.getZ(), 1.0e-9);
        assertEquals(0.7, result.getY(), 1.0e-9);
    }

    @Test
    void repeatedJumpImpulseHasASafeVerticalCeiling() {
        Vector result = GrappleMovement.controlledVelocity(
            new Vector(0.0, 1.1, 0.0), new Vector(1.0, 0.0, 0.0), 0.0f,
            false, false, false, false, true, 0.22, 2.4, 0.7
        );

        assertEquals(1.4, result.getY(), 1.0e-9);
        assertTrue(result.length() < GrapplingHookSettings.MAX_IMPULSE_SPEED);
    }
}
