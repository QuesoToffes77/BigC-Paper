package dev.linqfy.bigCasares.modules.glider;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GliderPhysicsTest {

    @Test
    void reducesFallSpeedAndAddsForwardMovement() {
        Vector result = GliderPhysics.step(
            new Vector(0.0, -0.8, 0.0), new Vector(0.0, 0.0, 1.0),
            GliderTierStats.defaults(GliderTier.I)
        );

        assertTrue(result.getY() >= -0.57);
        assertTrue(result.getY() < 0.0);
        assertTrue(result.getZ() > 0.0);
    }

    @Test
    void cameraPitchTradesMomentumForLift() {
        GliderTierStats stats = GliderTierStats.defaults(GliderTier.VI);
        Vector upward = GliderPhysics.step(
            new Vector(0.0, -0.25, 0.6), new Vector(0.0, 0.45, 0.89), stats);
        Vector downward = GliderPhysics.step(
            new Vector(0.0, -0.25, 0.6), new Vector(0.0, -0.45, 0.89), stats);

        assertTrue(upward.getY() > downward.getY());
        assertTrue(horizontal(upward) < horizontal(downward));
    }

    @Test
    void tierSixCannotGainInfiniteAltitude() {
        GliderTierStats stats = GliderTierStats.defaults(GliderTier.VI);
        Vector velocity = new Vector(0.0, -0.5, 0.0);
        Vector look = new Vector(0.0, 0.35, 0.94);
        double accumulatedY = 0.0;

        for (int tick = 0; tick < 400; tick++) {
            velocity = GliderPhysics.step(velocity, look, stats);
            accumulatedY += velocity.getY();
            assertTrue(velocity.getY() <= GliderPhysics.MAX_BOOST_VERTICAL_SPEED);
        }

        assertTrue(accumulatedY < -8.0, "Tier VI must continue losing altitude over time");
    }

    @Test
    void boostIsBounded() {
        Vector boosted = GliderPhysics.boost(
            new Vector(1.3, -0.1, 0.0), new Vector(1.0, 1.0, 0.0),
            GliderTierStats.defaults(GliderTier.VI)
        );

        assertTrue(horizontal(boosted) <= GliderPhysics.MAX_BOOST_HORIZONTAL_SPEED + 0.0001);
        assertTrue(boosted.getY() <= GliderPhysics.MAX_BOOST_VERTICAL_SPEED);
    }

    @Test
    void steeringBlendsInsteadOfSnapping() {
        GliderTierStats stats = GliderTierStats.defaults(GliderTier.I);
        Vector result = GliderPhysics.step(new Vector(0.4, -0.4, 0.0), new Vector(0.0, 0.0, 1.0), stats);

        assertTrue(result.getX() > 0.0);
        assertTrue(result.getZ() > 0.0);
        assertEquals(-0.42, result.getY(), 0.15);
    }

    private static double horizontal(Vector vector) {
        return Math.hypot(vector.getX(), vector.getZ());
    }
}
