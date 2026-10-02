package dev.linqfy.bigCasares.modules.grapplinghook;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrappleImpulseTest {

    @Test
    void tierImpulsePowerEscalatesMonotonically() {
        double previous = 0.0;
        for (GrapplingHookTier tier : GrapplingHookTier.values()) {
            double power = GrapplingHookSettings.defaults().tuningFor(tier).impulsePower();
            assertTrue(power > previous,
                tier + " impulse (" + power + ") must be stronger than the previous tier (" + previous + ")");
            previous = power;
        }
    }

    @Test
    void tierImpulseProgressionMatchesTheApprovedValues() {
        assertEquals(2.0, GrapplingHookSettings.defaults().tuningFor(GrapplingHookTier.I).impulsePower(), 1.0e-9);
        assertEquals(2.4, GrapplingHookSettings.defaults().tuningFor(GrapplingHookTier.II).impulsePower(), 1.0e-9);
        assertEquals(2.8, GrapplingHookSettings.defaults().tuningFor(GrapplingHookTier.III).impulsePower(), 1.0e-9);
        assertEquals(3.3, GrapplingHookSettings.defaults().tuningFor(GrapplingHookTier.IV).impulsePower(), 1.0e-9);
        assertEquals(3.8, GrapplingHookSettings.defaults().tuningFor(GrapplingHookTier.V).impulsePower(), 1.0e-9);
        assertEquals(4.2, GrapplingHookSettings.defaults().tuningFor(GrapplingHookTier.VI).impulsePower(), 1.0e-9);
    }

    @Test
    void effectiveImpulseVIBeatsImpulseIByAHealthyMargin() {
        double tierOne = GrapplingHookSettings.defaults().tuningFor(GrapplingHookTier.I).impulsePower();
        double tierSix = GrapplingHookSettings.defaults().tuningFor(GrapplingHookTier.VI).impulsePower();
        assertTrue(tierSix > tierOne * 1.9,
            "tier VI (" + tierSix + ") should be roughly double tier I (" + tierOne + ")");
    }

    @Test
    void biasStaysSmallOnEveryTier() {
        for (GrapplingHookTier tier : GrapplingHookTier.values()) {
            double bias = GrapplingHookSettings.defaults().tuningFor(tier).upwardBias();
            double power = GrapplingHookSettings.defaults().tuningFor(tier).impulsePower();
            assertTrue(bias < power * 0.25,
                tier + " bias (" + bias + ") must stay a small arc assist against power " + power);
        }
    }

    @Test
    void everyTierFlatShotIsPulledMainlyTowardTheWall() {
        for (GrapplingHookTier tier : GrapplingHookTier.values()) {
            var tuning = GrapplingHookSettings.defaults().tuningFor(tier);
            Vector impulse = GrappleShotMath.impulseVelocity(
                new Vector(0, 0, 0), new Vector(10, 0, 0),
                tuning.impulsePower(), tuning.upwardBias(), GrapplingHookSettings.MAX_IMPULSE_SPEED);
            double toward = impulse.clone().normalize().dot(new Vector(1, 0, 0));
            assertTrue(toward > 0.96,
                tier + " flat shot must fly toward the wall, dot=" + toward);
        }
    }

    @Test
    void noTierExceedsTheSafetyCeiling() {
        for (GrapplingHookTier tier : GrapplingHookTier.values()) {
            var tuning = GrapplingHookSettings.defaults().tuningFor(tier);
            Vector up = GrappleShotMath.impulseVelocity(
                new Vector(0, 0, 0), new Vector(0, 10, 0),
                tuning.impulsePower(), tuning.upwardBias(), GrapplingHookSettings.MAX_IMPULSE_SPEED);
            assertTrue(up.length() <= GrapplingHookSettings.MAX_IMPULSE_SPEED + 1.0e-9,
                tier + " upward speed " + up.length() + " must stay under the ceiling");
            Vector flat = GrappleShotMath.impulseVelocity(
                new Vector(0, 0, 0), new Vector(10, 0, 0),
                tuning.impulsePower(), tuning.upwardBias(), GrapplingHookSettings.MAX_IMPULSE_SPEED);
            assertTrue(flat.length() <= GrapplingHookSettings.MAX_IMPULSE_SPEED + 1.0e-9);
        }
    }

    @Test
    void defaultImpulsePowersAreUnderTheCeilingWithoutClamping() {
        for (GrapplingHookTier tier : GrapplingHookTier.values()) {
            var tuning = GrapplingHookSettings.defaults().tuningFor(tier);
            Vector flat = GrappleShotMath.impulseVelocity(
                new Vector(0, 0, 0), new Vector(10, 0, 0),
                tuning.impulsePower(), tuning.upwardBias(), GrapplingHookSettings.MAX_IMPULSE_SPEED);
            double raw = Math.sqrt(tuning.impulsePower() * tuning.impulsePower()
                + tuning.upwardBias() * tuning.upwardBias());
            assertEquals(raw, flat.length(), 1.0e-9, tier + " default impulse should not be clamped");
        }
    }

    @Test
    void misconfiguredPowerIsClampedAtTheCeiling() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new StringReader(
            "grappling-hook:\n"
                + "  tiers:\n"
                + "    vi: { impulse-power: 99.0, upward-bias: 5.0 }\n"));
        GrapplingHookSettings settings = GrapplingHookSettings.load(yaml);
        var tuning = settings.tuningFor(GrapplingHookTier.VI);
        // The config clamps absurd values before they reach the math.
        assertEquals(4.5, tuning.impulsePower(), 1.0e-9);
        assertEquals(1.0, tuning.upwardBias(), 1.0e-9);
        Vector impulse = GrappleShotMath.impulseVelocity(
            new Vector(0, 0, 0), new Vector(10, 0, 0),
            tuning.impulsePower(), tuning.upwardBias(), GrapplingHookSettings.MAX_IMPULSE_SPEED);
        assertTrue(impulse.length() <= GrapplingHookSettings.MAX_IMPULSE_SPEED + 1.0e-9);
        // And the hard math clamp still stops any direct absurd call.
        Vector absurd = GrappleShotMath.impulseVelocity(
            new Vector(0, 0, 0), new Vector(100, 0, 0),
            100.0, 5.0, GrapplingHookSettings.MAX_IMPULSE_SPEED);
        assertEquals(GrapplingHookSettings.MAX_IMPULSE_SPEED, absurd.length(), 1.0e-9);
    }
}
