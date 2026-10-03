package dev.linqfy.bigCasares.modules.grapplinghook;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrappleFeedbackPlanTest {

    @Test
    void defaultsProvideAllSixSounds() {
        GrappleFeedbackPlan plan = new GrappleFeedbackPlan(GrapplingHookSettings.defaults(), GrapplingHookTier.I);

        assertTrue(plan.soundsEnabled());
        assertNotNull(plan.fire());
        assertNotNull(plan.chain());
        assertNotNull(plan.attach());
        assertNotNull(plan.impulse());
        assertNotNull(plan.fail());
        assertNotNull(plan.cooldown());
        assertEquals(6, plan.chainIntervalTicks());
        assertEquals(6, plan.particleCount());
    }

    @Test
    void higherTiersAreSlightlyMorePowerful() {
        GrappleFeedbackPlan tierOne = new GrappleFeedbackPlan(GrapplingHookSettings.defaults(), GrapplingHookTier.I);
        GrappleFeedbackPlan tierSix = new GrappleFeedbackPlan(GrapplingHookSettings.defaults(), GrapplingHookTier.VI);

        assertTrue(tierSix.fire().volume() > tierOne.fire().volume());
        assertTrue(tierSix.fire().pitch() > tierOne.fire().pitch());
        assertTrue(tierSix.attach().volume() > tierOne.attach().volume());
        assertTrue(tierSix.impulse().volume() > tierOne.impulse().volume());
        // Same sound, only scaled.
        assertEquals(tierOne.fire().sound(), tierSix.fire().sound());
        assertEquals("BLOCK_PISTON_EXTEND", tierSix.fire().sound());
        assertEquals("BLOCK_CHAIN_STEP", tierSix.chain().sound());
        assertEquals("BLOCK_CHAIN_HIT", tierSix.attach().sound());
        assertEquals("BLOCK_CHAIN_PLACE", tierSix.impulse().sound());
        assertEquals("BLOCK_CHAIN_BREAK", tierSix.fail().sound());
        assertEquals("UI_BUTTON_CLICK", tierSix.cooldown().sound());
    }

    @Test
    void scalingStaysWithinClamps() {
        GrappleFeedbackPlan plan = new GrappleFeedbackPlan(GrapplingHookSettings.defaults(), GrapplingHookTier.VI);
        assertTrue(plan.fire().volume() <= 2.0f);
        assertTrue(plan.fire().pitch() <= 2.0f);
    }

    @Test
    void disabledSoundsYieldNullEvents() {
        GrapplingHookSettings settings = new GrapplingHookSettings(
            true, GrapplingHookActivation.defaults(), 1.6, 4, 10,
            GrapplingHookChain.defaults(),
            GrapplingHookMovement.defaults(),
            GrapplingHookTargeting.defaults(),
            new GrapplingHookSounds(false, 6, SoundTuning.FIRE, SoundTuning.CHAIN, SoundTuning.ATTACH,
                SoundTuning.IMPULSE, SoundTuning.FAIL, SoundTuning.COOLDOWN),
            GrapplingHookFeedback.defaults(),
            GrapplingHookSettings.defaults().tiers()
        );
        GrappleFeedbackPlan plan = new GrappleFeedbackPlan(settings, GrapplingHookTier.III);

        assertNull(plan.fire());
        assertNull(plan.chain());
        assertNull(plan.attach());
        assertNull(plan.impulse());
        assertNull(plan.fail());
        assertNull(plan.cooldown());
        assertTrue(plan.feedbackEnabled());
    }

    @Test
    void disabledFeedbackStillScalesSounds() {
        GrapplingHookSettings settings = new GrapplingHookSettings(
            true, GrapplingHookActivation.defaults(), 1.6, 4, 10,
            GrapplingHookChain.defaults(),
            GrapplingHookMovement.defaults(),
            GrapplingHookTargeting.defaults(),
            GrapplingHookSounds.defaults(),
            new GrapplingHookFeedback(false, 6),
            GrapplingHookSettings.defaults().tiers()
        );
        GrappleFeedbackPlan plan = new GrappleFeedbackPlan(settings, GrapplingHookTier.II);

        assertNotNull(plan.fire());
        assertTrue(!plan.feedbackEnabled());
        assertEquals(6, plan.particleCount());
    }
}
