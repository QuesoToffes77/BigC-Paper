package dev.linqfy.bigCasares.modules.glider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GliderActivationPolicyTest {

    @Test
    void gliderRequiresAnEquippedHand() {
        assertFalse(GliderActivationPolicy.canStart(context(false, false, -0.4, true)));
        assertTrue(GliderActivationPolicy.canStart(context(true, false, -0.4, true)));
        assertFalse(GliderActivationPolicy.canStart(context(false, false, -0.4, true)),
            "An ordinary inventory item is represented by no equipped Glider");
    }

    @Test
    void glidingStartsWhileFallingButNotOnGround() {
        assertTrue(GliderActivationPolicy.canStart(context(true, false, -0.2, true)));
        assertFalse(GliderActivationPolicy.canStart(context(true, true, -0.2, true)));
        assertFalse(GliderActivationPolicy.canStart(context(true, false, 0.1, true)));
    }

    @Test
    void normalTotemAndOtherOffhandItemsAreUnaffected() {
        assertFalse(GliderActivationPolicy.canStart(context(false, false, -0.4, true)));
    }

    @Test
    void rejectsWaterVehiclesElytraSpectatorAndCreativeFlight() {
        GliderActivationContext valid = context(true, false, -0.4, true);
        assertFalse(GliderActivationPolicy.canStart(valid.withSwimming(true)));
        assertFalse(GliderActivationPolicy.canStart(valid.withVehicle(true)));
        assertFalse(GliderActivationPolicy.canStart(valid.withElytra(true)));
        assertFalse(GliderActivationPolicy.canStart(valid.withSpectator(true)));
        assertFalse(GliderActivationPolicy.canStart(valid.withCreativeFlying(true)));
    }

    private static GliderActivationContext context(
        boolean offhand,
        boolean ground,
        double velocityY,
        boolean sneaking
    ) {
        return new GliderActivationContext(
            offhand, ground, velocityY, sneaking, false, false, false, false, false, true,
            GliderActivationMode.SNEAK, -0.05
        );
    }
}
