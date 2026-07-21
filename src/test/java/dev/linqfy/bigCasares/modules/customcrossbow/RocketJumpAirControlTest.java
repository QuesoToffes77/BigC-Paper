package dev.linqfy.bigCasares.modules.customcrossbow;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RocketJumpAirControlTest {

    @Test
    void addsStrongForwardAirAccelerationUsingPlayerYaw() throws Exception {
        Object motion = apply(0.0, 0.0, 0.0f, true, false, false, false);

        assertEquals(0.0, component(motion, "x"), 0.0001);
        assertEquals(0.217, component(motion, "z"), 0.0001);
    }

    @Test
    void diagonalAirStrafingIsNormalizedAndCappedAtHighSpeed() throws Exception {
        Object diagonal = apply(0.0, 0.0, 0.0f, true, false, false, true);
        double diagonalSpeed = Math.hypot(component(diagonal, "x"), component(diagonal, "z"));
        assertEquals(0.217, diagonalSpeed, 0.0001);

        Object capped = apply(2.0, 0.0, -90.0f, true, false, false, false);
        assertEquals(2.17, Math.hypot(component(capped, "x"), component(capped, "z")), 0.0001);
    }

    @Test
    void noMovementInputLeavesHorizontalMomentumUntouched() throws Exception {
        Object motion = apply(1.25, -0.75, 45.0f, false, false, false, false);

        assertEquals(1.25, component(motion, "x"), 0.0001);
        assertEquals(-0.75, component(motion, "z"), 0.0001);
    }

    private static Object apply(
        double x,
        double z,
        float yaw,
        boolean forward,
        boolean backward,
        boolean left,
        boolean right
    ) throws Exception {
        Class<?> type = Class.forName(
            "dev.linqfy.bigCasares.modules.customcrossbow.RocketJumpAirControl"
        );
        Method method = type.getMethod("apply", double.class, double.class, float.class,
            boolean.class, boolean.class, boolean.class, boolean.class);
        return method.invoke(null, x, z, yaw, forward, backward, left, right);
    }

    private static double component(Object motion, String name) throws Exception {
        return (double) motion.getClass().getMethod(name).invoke(motion);
    }
}
