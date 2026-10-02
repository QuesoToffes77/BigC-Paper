package dev.linqfy.bigCasares.modules.grapplinghook;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrapplingHookSettingsTest {

    @Test
    void emptyConfigUsesDefaults() {
        GrapplingHookSettings settings = GrapplingHookSettings.load(new YamlConfiguration());

        assertTrue(settings.enabled());
        assertTrue(settings.canUse());
        assertEquals(GrapplingHookActivationMode.RIGHT_CLICK, settings.activation().mode());
        assertTrue(settings.activation().retractOnActivation());
        assertEquals(1.0, settings.chain().spacing(), 1.0e-9);
        assertTrue(settings.chain().enabled());
        assertTrue(settings.targeting().blocks());
        assertTrue(settings.targeting().entities());
        assertFalse(settings.targeting().players());
        assertEquals(1.6, settings.hookSpeed(), 1.0e-9);
        assertEquals(4, settings.attachTicks());
        assertEquals(10, settings.failCooldownTicks());
        assertEquals(0.85, settings.movement().reelPowerMultiplier(), 1.0e-9);
        assertEquals(3.0, settings.movement().maxReelSpeed(), 1.0e-9);
        assertEquals(2, settings.movement().airJumps());
        assertEquals(2.0, settings.movement().retractSpeedMultiplier(), 1.0e-9);
        assertEquals(50.0, settings.tuningFor(GrapplingHookTier.I).range(), 1.0e-9);
        assertEquals(5.0, settings.tuningFor(GrapplingHookTier.I).cooldownSeconds(), 1.0e-9);
        assertEquals(200.0, settings.tuningFor(GrapplingHookTier.VI).range(), 1.0e-9);
        assertEquals(1.25, settings.tuningFor(GrapplingHookTier.VI).cooldownSeconds(), 1.0e-9);
        // Impulse is per tier: basic-but-useful tier I up to maximum tier VI.
        assertEquals(2.0, settings.tuningFor(GrapplingHookTier.I).impulsePower(), 1.0e-9);
        assertEquals(0.25, settings.tuningFor(GrapplingHookTier.I).upwardBias(), 1.0e-9);
        assertEquals(4.2, settings.tuningFor(GrapplingHookTier.VI).impulsePower(), 1.0e-9);
        assertEquals(0.45, settings.tuningFor(GrapplingHookTier.VI).upwardBias(), 1.0e-9);
    }

    @Test
    void missingSectionFallsBackToDefaults() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("other-section", 1);

        GrapplingHookSettings settings = GrapplingHookSettings.load(yaml);

        assertEquals(GrapplingHookSettings.defaults(), settings);
    }

    @Test
    void loadsPerTierOverrides() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("grappling-hook.tiers.i.range", 14.0);
        yaml.set("grappling-hook.tiers.i.cooldown-seconds", 4.75);
        yaml.set("grappling-hook.tiers.i.impulse-power", 2.2);
        yaml.set("grappling-hook.tiers.i.upward-bias", 0.1);
        yaml.set("grappling-hook.tiers.vi.range", 40.0);
        yaml.set("grappling-hook.tiers.vi.cooldown-seconds", 1.0);
        yaml.set("grappling-hook.attach-ticks", 6);
        yaml.set("grappling-hook.enabled", false);

        GrapplingHookSettings settings = GrapplingHookSettings.load(yaml);

        assertEquals(14.0, settings.tuningFor(GrapplingHookTier.I).range(), 1.0e-9);
        assertEquals(4.75, settings.tuningFor(GrapplingHookTier.I).cooldownSeconds(), 1.0e-9);
        assertEquals(2.2, settings.tuningFor(GrapplingHookTier.I).impulsePower(), 1.0e-9);
        assertEquals(0.1, settings.tuningFor(GrapplingHookTier.I).upwardBias(), 1.0e-9);
        assertEquals(40.0, settings.tuningFor(GrapplingHookTier.VI).range(), 1.0e-9);
        assertEquals(1.0, settings.tuningFor(GrapplingHookTier.VI).cooldownSeconds(), 1.0e-9);
        // Unconfigured tiers keep their own defaults.
        assertEquals(4.2, settings.tuningFor(GrapplingHookTier.VI).impulsePower(), 1.0e-9);
        assertEquals(6, settings.attachTicks());
        assertFalse(settings.enabled());
        assertFalse(settings.canUse());
    }

    @Test
    void loadsActivationModeAndRetractFlag() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("grappling-hook.activation.mode", "SWAP_HANDS");
        yaml.set("grappling-hook.activation.retract-on-activation", false);

        GrapplingHookSettings settings = GrapplingHookSettings.load(yaml);

        assertEquals(GrapplingHookActivationMode.SWAP_HANDS, settings.activation().mode());
        assertFalse(settings.activation().retractOnActivation());
    }

    @Test
    void everyActivationModeParses() {
        for (GrapplingHookActivationMode mode : GrapplingHookActivationMode.values()) {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("grappling-hook.activation.mode", mode.name());
            assertEquals(mode, GrapplingHookSettings.load(yaml).activation().mode(), mode.name());
        }
        // Case-insensitive and trimmed.
        YamlConfiguration lower = new YamlConfiguration();
        lower.set("grappling-hook.activation.mode", "  left_click ");
        assertEquals(GrapplingHookActivationMode.LEFT_CLICK, GrapplingHookSettings.load(lower).activation().mode());
    }

    @Test
    void unknownActivationModeFallsBackWithWarning() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("grappling-hook.activation.mode", "FOO");
        List<String> warnings = new ArrayList<>();

        GrapplingHookSettings settings = GrapplingHookSettings.load(yaml, warnings::add);

        assertEquals(GrapplingHookActivationMode.RIGHT_CLICK, settings.activation().mode());
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("FOO"));
        assertTrue(warnings.get(0).toLowerCase().contains("right_click"));
    }

    @Test
    void loadsTargetingSection() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("grappling-hook.targeting.blocks", false);
        yaml.set("grappling-hook.targeting.entities", true);
        yaml.set("grappling-hook.targeting.players", true);

        GrapplingHookSettings settings = GrapplingHookSettings.load(yaml);

        assertFalse(settings.targeting().blocks());
        assertTrue(settings.targeting().entities());
        assertTrue(settings.targeting().players());
    }

    @Test
    void legacyEntitiesKeysStillWork() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("grappling-hook.entities.enabled", true);
        yaml.set("grappling-hook.entities.allow-players", true);

        GrapplingHookSettings settings = GrapplingHookSettings.load(yaml);

        assertTrue(settings.targeting().entities());
        assertTrue(settings.targeting().players());
    }

    @Test
    void loadsChainSectionAndLegacySpacing() {
        YamlConfiguration newStyle = new YamlConfiguration();
        newStyle.set("grappling-hook.chain.enabled", false);
        newStyle.set("grappling-hook.chain.spacing", 0.75);
        GrapplingHookSettings settings = GrapplingHookSettings.load(newStyle);
        assertFalse(settings.chain().enabled());
        assertEquals(0.75, settings.chain().spacing(), 1.0e-9);

        YamlConfiguration legacy = new YamlConfiguration();
        legacy.set("grappling-hook.chain-spacing", 2.0);
        assertEquals(2.0, GrapplingHookSettings.load(legacy).chain().spacing(), 1.0e-9);
    }

    @Test
    void loadsAndClampsMovementControl() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("grappling-hook.movement.reel-power-multiplier", 1.1);
        yaml.set("grappling-hook.movement.max-reel-speed", 3.4);
        yaml.set("grappling-hook.movement.air-control-acceleration", 0.3);
        yaml.set("grappling-hook.movement.max-horizontal-speed", 2.8);
        yaml.set("grappling-hook.movement.air-jumps", 20);
        yaml.set("grappling-hook.movement.jump-velocity", 0.8);
        yaml.set("grappling-hook.movement.retract-speed-multiplier", 20.0);

        GrapplingHookMovement movement = GrapplingHookSettings.load(yaml).movement();

        assertEquals(1.1, movement.reelPowerMultiplier(), 1.0e-9);
        assertEquals(3.4, movement.maxReelSpeed(), 1.0e-9);
        assertEquals(0.3, movement.airControlAcceleration(), 1.0e-9);
        assertEquals(2.8, movement.maxHorizontalSpeed(), 1.0e-9);
        assertEquals(4, movement.airJumps());
        assertEquals(0.8, movement.jumpVelocity(), 1.0e-9);
        assertEquals(5.0, movement.retractSpeedMultiplier(), 1.0e-9);
    }

    @Test
    void absurdValuesAreClamped() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("grappling-hook.tiers.i.range", 99999.0);
        yaml.set("grappling-hook.tiers.i.cooldown-seconds", -5.0);
        yaml.set("grappling-hook.tiers.i.impulse-power", 999.0);
        yaml.set("grappling-hook.tiers.i.upward-bias", 50.0);
        yaml.set("grappling-hook.chain.spacing", 0.01);
        yaml.set("grappling-hook.hook-speed", 99.0);
        yaml.set("grappling-hook.attach-ticks", 500);

        GrapplingHookSettings settings = GrapplingHookSettings.load(yaml);

        assertEquals(256.0, settings.tuningFor(GrapplingHookTier.I).range(), 1.0e-9);
        assertEquals(0.25, settings.tuningFor(GrapplingHookTier.I).cooldownSeconds(), 1.0e-9);
        assertEquals(4.5, settings.tuningFor(GrapplingHookTier.I).impulsePower(), 1.0e-9);
        assertEquals(1.0, settings.tuningFor(GrapplingHookTier.I).upwardBias(), 1.0e-9);
        assertEquals(0.25, settings.chain().spacing(), 1.0e-9);
        assertEquals(8.0, settings.hookSpeed(), 1.0e-9);
        assertEquals(200, settings.attachTicks());
    }

    @Test
    void nonFiniteValuesAreSanitized() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("grappling-hook.tiers.ii.range", "NaN");
        yaml.set("grappling-hook.tiers.ii.impulse-power", "Infinity");

        GrapplingHookSettings settings = GrapplingHookSettings.load(yaml);

        // Non-finite numbers never reach the math: the loader falls back to
        // the configured defaults for range and impulse power.
        assertEquals(GrapplingHookTier.II.defaultRange(), settings.tuningFor(GrapplingHookTier.II).range(), 1.0e-9);
        assertEquals(GrapplingHookSettings.defaults().tuningFor(GrapplingHookTier.II).impulsePower(),
            settings.tuningFor(GrapplingHookTier.II).impulsePower(), 1.0e-9);
    }

    @Test
    void defaultsRecordMatchesLoadDefaults() {
        assertEquals(GrapplingHookSettings.defaults().tiers(), GrapplingHookSettings.load(new YamlConfiguration()).tiers());
    }

    @Test
    void soundsDefaultsAreEnabledWithChainThrottle() {
        GrapplingHookSettings settings = GrapplingHookSettings.load(new YamlConfiguration());

        assertTrue(settings.sounds().enabled());
        assertEquals(6, settings.sounds().chainIntervalTicks());
        assertEquals("BLOCK_PISTON_EXTEND", settings.sounds().fire().sound());
        assertEquals("BLOCK_CHAIN_STEP", settings.sounds().chain().sound());
        assertEquals("BLOCK_CHAIN_HIT", settings.sounds().attach().sound());
        assertEquals("BLOCK_CHAIN_PLACE", settings.sounds().impulse().sound());
        assertEquals("BLOCK_CHAIN_BREAK", settings.sounds().fail().sound());
        assertEquals("UI_BUTTON_CLICK", settings.sounds().cooldown().sound());
    }

    @Test
    void loadsCustomSoundsFeedbackAndTargeting() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("grappling-hook.sounds.enabled", false);
        yaml.set("grappling-hook.sounds.chain-interval-ticks", 12);
        yaml.set("grappling-hook.sounds.attach.sound", "BLOCK_ANVIL_LAND");
        yaml.set("grappling-hook.sounds.attach.volume", 1.2);
        yaml.set("grappling-hook.sounds.attach.pitch", 0.8);
        yaml.set("grappling-hook.feedback.enabled", false);
        yaml.set("grappling-hook.feedback.particle-count", 12);
        yaml.set("grappling-hook.targeting.entities", true);
        yaml.set("grappling-hook.targeting.players", true);

        GrapplingHookSettings settings = GrapplingHookSettings.load(yaml);

        assertFalse(settings.sounds().enabled());
        assertEquals(12, settings.sounds().chainIntervalTicks());
        assertEquals("BLOCK_ANVIL_LAND", settings.sounds().attach().sound());
        assertEquals(1.2f, settings.sounds().attach().volume(), 1.0e-6f);
        assertEquals(0.8f, settings.sounds().attach().pitch(), 1.0e-6f);
        assertFalse(settings.feedback().enabled());
        assertEquals(12, settings.feedback().particleCount());
        assertTrue(settings.targeting().entities());
        assertTrue(settings.targeting().players());
    }

    @Test
    void blankSoundNameFallsBackToDefault() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("grappling-hook.sounds.fire.sound", "   ");

        GrapplingHookSettings settings = GrapplingHookSettings.load(yaml);

        assertEquals("BLOCK_PISTON_EXTEND", settings.sounds().fire().sound());
    }

    @Test
    void unknownSoundNameIsKeptAndResolvedAtPlayTime() {
        // Validity cannot be checked at load time without a live server (the
        // Sound enum itself needs one). Unknown names are kept in settings and
        // skipped gracefully when played, so a typo never crashes the module.
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("grappling-hook.sounds.fire.sound", "NOT_A_REAL_SOUND");

        GrapplingHookSettings settings = GrapplingHookSettings.load(yaml);

        assertEquals("NOT_A_REAL_SOUND", settings.sounds().fire().sound());
    }

    @Test
    void soundVolumeAndChainIntervalAreClamped() {
        // Partial override: only volume/pitch set, the default sound name stays.
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("grappling-hook.sounds.attach.volume", 99.0);
        yaml.set("grappling-hook.sounds.attach.pitch", 0.01);
        yaml.set("grappling-hook.sounds.chain-interval-ticks", 1);
        yaml.set("grappling-hook.feedback.particle-count", 999);

        GrapplingHookSettings settings = GrapplingHookSettings.load(yaml);

        assertEquals("BLOCK_CHAIN_HIT", settings.sounds().attach().sound());
        assertEquals(2.0f, settings.sounds().attach().volume(), 1.0e-6f);
        assertEquals(0.25f, settings.sounds().attach().pitch(), 1.0e-6f);
        assertEquals(2, settings.sounds().chainIntervalTicks());
        assertEquals(32, settings.feedback().particleCount());
    }
}
