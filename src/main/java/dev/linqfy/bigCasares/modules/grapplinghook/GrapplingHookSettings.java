package dev.linqfy.bigCasares.modules.grapplinghook;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Grappling Hook tuning. Every numeric value is normalized defensively in the
 * record so no calculation can propagate negatives or non-finite numbers.
 *
 * The activation input is configurable ({@link GrapplingHookActivationMode}),
 * the pull impulse lives per tier ({@link TierTuning#impulsePower()} plus a
 * small {@link TierTuning#upwardBias()}), and {@link #MAX_IMPULSE_SPEED} is
 * the hard ceiling applied to the final velocity vector so no configuration
 * can produce an absurd speed.
 */
public record GrapplingHookSettings(
    boolean enabled,
    GrapplingHookActivation activation,
    double hookSpeed,
    int attachTicks,
    int failCooldownTicks,
    GrapplingHookChain chain,
    GrapplingHookMovement movement,
    GrapplingHookTargeting targeting,
    GrapplingHookSounds sounds,
    GrapplingHookFeedback feedback,
    Map<GrapplingHookTier, TierTuning> tiers
) {

    private static final double MAX_RANGE = 256.0;
    private static final double MIN_COOLDOWN_SECONDS = 0.25;
    private static final double MAX_COOLDOWN_SECONDS = 120.0;
    private static final double MAX_SPACING = 4.0;
    private static final double MAX_HOOK_SPEED = 8.0;
    private static final int MAX_ATTACH_TICKS = 200;
    private static final int MAX_FAIL_COOLDOWN_TICKS = 200;
    private static final double MIN_IMPULSE_POWER = 0.25;
    private static final double MAX_TIER_IMPULSE_POWER = 4.5;
    private static final double MAX_UPWARD_BIAS = 1.0;

    /**
     * Hard ceiling for the final pull velocity, in blocks/tick (≈ 90
     * blocks/second). Applied last in {@link GrappleShotMath#impulseVelocity}
     * so even absurd configuration values can never launch the player at a
     * dangerous speed.
     */
    public static final double MAX_IMPULSE_SPEED = 4.5;

    public GrapplingHookSettings {
        activation = activation == null ? GrapplingHookActivation.defaults() : activation;
        hookSpeed = clampFinite(hookSpeed, 0.1, MAX_HOOK_SPEED);
        attachTicks = clampInt(attachTicks, 0, MAX_ATTACH_TICKS);
        failCooldownTicks = clampInt(failCooldownTicks, 0, MAX_FAIL_COOLDOWN_TICKS);
        chain = chain == null ? GrapplingHookChain.defaults() : chain;
        movement = movement == null ? GrapplingHookMovement.defaults() : movement;
        targeting = targeting == null ? GrapplingHookTargeting.defaults() : targeting;
        sounds = sounds == null ? GrapplingHookSounds.defaults() : sounds;
        feedback = feedback == null ? GrapplingHookFeedback.defaults() : feedback;
        if (tiers == null || tiers.isEmpty()) {
            tiers = defaultsTiers();
        } else {
            EnumMap<GrapplingHookTier, TierTuning> normalized = new EnumMap<>(GrapplingHookTier.class);
            for (GrapplingHookTier tier : GrapplingHookTier.values()) {
                TierTuning tuning = tiers.get(tier);
                normalized.put(tier, tuning == null ? TierTuning.forTier(tier) : tuning);
            }
            tiers = Map.copyOf(normalized);
        }
    }

    public static GrapplingHookSettings defaults() {
        return new GrapplingHookSettings(true, GrapplingHookActivation.defaults(), 1.6, 4, 10,
            GrapplingHookChain.defaults(),
            GrapplingHookMovement.defaults(),
            GrapplingHookTargeting.defaults(),
            GrapplingHookSounds.defaults(),
            GrapplingHookFeedback.defaults(),
            defaultsTiers());
    }

    /** Loads without reporting warnings (used by tests and silent callers). */
    public static GrapplingHookSettings load(FileConfiguration config) {
        return load(config, ignored -> {
        });
    }

    /**
     * Loads the {@code grappling-hook} section. Invalid configuration values
     * are sanitized to safe defaults; {@code warnings} receives a clear
     * message for anything that was replaced (e.g. an unknown activation mode
     * falling back to {@code RIGHT_CLICK}).
     */
    public static GrapplingHookSettings load(FileConfiguration config, Consumer<String> warnings) {
        Consumer<String> sink = warnings == null ? ignored -> {
        } : warnings;
        ConfigurationSection root = config == null ? null : config.getConfigurationSection("grappling-hook");
        if (root == null) {
            return defaults();
        }
        EnumMap<GrapplingHookTier, TierTuning> tiers = new EnumMap<>(GrapplingHookTier.class);
        for (GrapplingHookTier tier : GrapplingHookTier.values()) {
            String path = "tiers." + tier.name().toLowerCase(Locale.ROOT);
            TierTuning fallback = TierTuning.forTier(tier);
            tiers.put(tier, new TierTuning(
                root.getDouble(path + ".range", fallback.range()),
                root.getDouble(path + ".cooldown-seconds", fallback.cooldownSeconds()),
                root.getDouble(path + ".impulse-power", fallback.impulsePower()),
                root.getDouble(path + ".upward-bias", fallback.upwardBias())
            ));
        }
        return new GrapplingHookSettings(
            root.getBoolean("enabled", true),
            GrapplingHookActivation.load(root, sink),
            root.getDouble("hook-speed", 1.6),
            root.getInt("attach-ticks", 4),
            root.getInt("fail-cooldown-ticks", 10),
            GrapplingHookChain.load(root),
            GrapplingHookMovement.load(root),
            GrapplingHookTargeting.load(root),
            GrapplingHookSounds.load(root),
            GrapplingHookFeedback.load(root),
            tiers
        );
    }

    public TierTuning tuningFor(GrapplingHookTier tier) {
        return tiers.getOrDefault(tier, TierTuning.forTier(tier));
    }

    public boolean canUse() {
        return enabled && hookSpeed > 0.0 && (!chain.enabled() || chain.spacing() > 0.0);
    }

    private static Map<GrapplingHookTier, TierTuning> defaultsTiers() {
        EnumMap<GrapplingHookTier, TierTuning> values = new EnumMap<>(GrapplingHookTier.class);
        for (GrapplingHookTier tier : GrapplingHookTier.values()) {
            values.put(tier, TierTuning.forTier(tier));
        }
        return Map.copyOf(values);
    }

    static double clampFinite(double value, double minimum, double maximum) {
        if (!Double.isFinite(value)) {
            return minimum;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }

    static int clampInt(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    static float clampFloat(float value, float minimum, float maximum) {
        if (!Float.isFinite(value)) {
            return minimum;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }

    /**
     * Per-tier tuning. Range is clamped to {@code [1, 256]} blocks, cooldown
     * to {@code [0.25, 120]} seconds, impulse power to {@code [0.25, 4.0]}
     * blocks/tick and the upward bias to {@code [0, 1]} blocks/tick, so a
     * misconfigured file can never produce a zero-range hook, a negative
     * cooldown or an unbounded pull. The final velocity is additionally
     * clamped to {@link #MAX_IMPULSE_SPEED} by the math.
     */
    public record TierTuning(double range, double cooldownSeconds, double impulsePower, double upwardBias) {
        public TierTuning {
            range = clampFinite(range, 1.0, MAX_RANGE);
            cooldownSeconds = clampFinite(cooldownSeconds, MIN_COOLDOWN_SECONDS, MAX_COOLDOWN_SECONDS);
            impulsePower = clampFinite(impulsePower, MIN_IMPULSE_POWER, MAX_TIER_IMPULSE_POWER);
            upwardBias = clampFinite(upwardBias, 0.0, MAX_UPWARD_BIAS);
        }

        /**
         * Per-tier defaults. Impulse power (blocks/tick) escalates clearly
         * from a basic-but-useful tier I pull to a maximum tier VI pull while
         * staying under the hard speed ceiling; the upward bias stays a small
         * arc assist that never dominates the direction toward the target.
         */
        static TierTuning forTier(GrapplingHookTier tier) {
            return switch (tier) {
                case I -> new TierTuning(tier.defaultRange(), tier.defaultCooldownSeconds(), 2.0, 0.25);
                case II -> new TierTuning(tier.defaultRange(), tier.defaultCooldownSeconds(), 2.4, 0.28);
                case III -> new TierTuning(tier.defaultRange(), tier.defaultCooldownSeconds(), 2.8, 0.31);
                case IV -> new TierTuning(tier.defaultRange(), tier.defaultCooldownSeconds(), 3.3, 0.35);
                case V -> new TierTuning(tier.defaultRange(), tier.defaultCooldownSeconds(), 3.8, 0.40);
                case VI -> new TierTuning(tier.defaultRange(), tier.defaultCooldownSeconds(), 4.2, 0.45);
            };
        }
    }
}

/**
 * How the hook activates and what re-activating during an active session does.
 * Unknown modes fall back to {@code RIGHT_CLICK} with a warning.
 */
record GrapplingHookActivation(GrapplingHookActivationMode mode, boolean retractOnActivation) {

    GrapplingHookActivation {
        mode = mode == null ? GrapplingHookActivationMode.RIGHT_CLICK : mode;
    }

    static GrapplingHookActivation defaults() {
        return new GrapplingHookActivation(GrapplingHookActivationMode.RIGHT_CLICK, true);
    }

    static GrapplingHookActivation load(ConfigurationSection root, Consumer<String> warnings) {
        if (root.getConfigurationSection("activation") == null) {
            return defaults();
        }
        return new GrapplingHookActivation(
            GrapplingHookActivationMode.fromConfig(root.getString("activation.mode"), warnings),
            root.getBoolean("activation.retract-on-activation", true)
        );
    }
}

/**
 * Chain visuals. When disabled the hook still flies and pulls; only the
 * {@code IRON_CHAIN} displays are skipped.
 */
record GrapplingHookChain(boolean enabled, double spacing) {

    GrapplingHookChain {
        spacing = GrapplingHookSettings.clampFinite(spacing, 0.25, 4.0);
    }

    static GrapplingHookChain defaults() {
        return new GrapplingHookChain(true, 1.0);
    }

    static GrapplingHookChain load(ConfigurationSection root) {
        if (root.getConfigurationSection("chain") == null) {
            // Backwards compatible with the old top-level chain-spacing key.
            return new GrapplingHookChain(true, root.getDouble("chain-spacing", 1.0));
        }
        return new GrapplingHookChain(
            root.getBoolean("chain.enabled", true),
            root.getDouble("chain.spacing", root.getDouble("chain-spacing", 1.0))
        );
    }
}

/** Pull speed, air steering and jump assistance while a block hook is attached. */
record GrapplingHookMovement(
    double reelPowerMultiplier,
    double maxReelSpeed,
    double airControlAcceleration,
    double maxHorizontalSpeed,
    int airJumps,
    double jumpVelocity,
    double retractSpeedMultiplier
) {

    GrapplingHookMovement {
        reelPowerMultiplier = GrapplingHookSettings.clampFinite(reelPowerMultiplier, 0.1, 2.0);
        maxReelSpeed = GrapplingHookSettings.clampFinite(maxReelSpeed, 0.25, 4.0);
        airControlAcceleration = GrapplingHookSettings.clampFinite(airControlAcceleration, 0.0, 0.5);
        maxHorizontalSpeed = GrapplingHookSettings.clampFinite(maxHorizontalSpeed, 0.25, 4.0);
        airJumps = GrapplingHookSettings.clampInt(airJumps, 0, 4);
        jumpVelocity = GrapplingHookSettings.clampFinite(jumpVelocity, 0.1, 1.0);
        retractSpeedMultiplier = GrapplingHookSettings.clampFinite(retractSpeedMultiplier, 0.25, 5.0);
    }

    static GrapplingHookMovement defaults() {
        return new GrapplingHookMovement(0.85, 3.0, 0.22, 2.4, 2, 0.7, 2.0);
    }

    static GrapplingHookMovement load(ConfigurationSection root) {
        if (root.getConfigurationSection("movement") == null) {
            return defaults();
        }
        GrapplingHookMovement fallback = defaults();
        return new GrapplingHookMovement(
            root.getDouble("movement.reel-power-multiplier", fallback.reelPowerMultiplier()),
            root.getDouble("movement.max-reel-speed", fallback.maxReelSpeed()),
            root.getDouble("movement.air-control-acceleration", fallback.airControlAcceleration()),
            root.getDouble("movement.max-horizontal-speed", fallback.maxHorizontalSpeed()),
            root.getInt("movement.air-jumps", fallback.airJumps()),
            root.getDouble("movement.jump-velocity", fallback.jumpVelocity()),
            root.getDouble("movement.retract-speed-multiplier", fallback.retractSpeedMultiplier())
        );
    }
}

/**
 * What the ray may attach to. Blocks and entities are both ray tested along
 * the look direction and the nearest valid target wins, so an entity behind a
 * wall can never be grabbed.
 */
record GrapplingHookTargeting(boolean blocks, boolean entities, boolean players) {

    static GrapplingHookTargeting defaults() {
        return new GrapplingHookTargeting(true, true, false);
    }

    static GrapplingHookTargeting load(ConfigurationSection root) {
        if (root.getConfigurationSection("targeting") != null) {
            return new GrapplingHookTargeting(
                root.getBoolean("targeting.blocks", true),
                root.getBoolean("targeting.entities", true),
                root.getBoolean("targeting.players", false)
            );
        }
        // Backwards compatible with the old entities.enabled / allow-players keys.
        return new GrapplingHookTargeting(
            true,
            root.getBoolean("entities.enabled", true),
            root.getBoolean("entities.allow-players", false)
        );
    }
}

/**
 * One configurable sound event. The name is kept as-is (blank falls back to
 * the default tuning at load time); validity against {@link org.bukkit.Sound}
 * is checked lazily when the sound is played, so a typo never crashes the
 * module and the settings stay testable without a live server.
 */
record SoundTuning(String sound, float volume, float pitch) {

    static final SoundTuning FIRE = new SoundTuning("BLOCK_PISTON_EXTEND", 0.6f, 1.3f);
    static final SoundTuning CHAIN = new SoundTuning("BLOCK_CHAIN_STEP", 0.3f, 1.2f);
    static final SoundTuning ATTACH = new SoundTuning("BLOCK_CHAIN_HIT", 0.8f, 1.0f);
    static final SoundTuning IMPULSE = new SoundTuning("BLOCK_CHAIN_PLACE", 0.6f, 1.1f);
    static final SoundTuning FAIL = new SoundTuning("BLOCK_CHAIN_BREAK", 0.4f, 0.9f);
    static final SoundTuning COOLDOWN = new SoundTuning("UI_BUTTON_CLICK", 0.4f, 0.8f);

    SoundTuning {
        sound = sound == null || sound.isBlank() ? "" : sound.trim().toUpperCase(Locale.ROOT);
        volume = GrapplingHookSettings.clampFloat(volume, 0.0f, 2.0f);
        pitch = GrapplingHookSettings.clampFloat(pitch, 0.25f, 2.0f);
    }

    /**
     * Merges any configured value under {@code path} over the fallback, so a
     * user can override just the volume of one event without repeating the
     * sound name. A blank or missing name keeps the fallback tuning.
     */
    static SoundTuning of(ConfigurationSection root, String path, SoundTuning fallback) {
        if (root == null || !root.isSet(path)) {
            return fallback;
        }
        String name = root.getString(path + ".sound", fallback.sound());
        if (name == null || name.isBlank()) {
            return fallback;
        }
        return new SoundTuning(
            name,
            (float) root.getDouble(path + ".volume", fallback.volume()),
            (float) root.getDouble(path + ".pitch", fallback.pitch())
        );
    }
}

/**
 * Sound feedback for every Grappling Hook event. {@code chainIntervalTicks}
 * throttles the subtle chain-extending tick so it is never played every tick.
 */
record GrapplingHookSounds(
    boolean enabled,
    int chainIntervalTicks,
    SoundTuning fire,
    SoundTuning chain,
    SoundTuning attach,
    SoundTuning impulse,
    SoundTuning fail,
    SoundTuning cooldown
) {
    GrapplingHookSounds {
        chainIntervalTicks = GrapplingHookSettings.clampInt(chainIntervalTicks, 2, 100);
        fire = fire == null ? SoundTuning.FIRE : fire;
        chain = chain == null ? SoundTuning.CHAIN : chain;
        attach = attach == null ? SoundTuning.ATTACH : attach;
        impulse = impulse == null ? SoundTuning.IMPULSE : impulse;
        fail = fail == null ? SoundTuning.FAIL : fail;
        cooldown = cooldown == null ? SoundTuning.COOLDOWN : cooldown;
    }

    static GrapplingHookSounds defaults() {
        return new GrapplingHookSounds(true, 6, SoundTuning.FIRE, SoundTuning.CHAIN, SoundTuning.ATTACH,
            SoundTuning.IMPULSE, SoundTuning.FAIL, SoundTuning.COOLDOWN);
    }

    static GrapplingHookSounds load(ConfigurationSection root) {
        ConfigurationSection section = root.getConfigurationSection("sounds");
        if (section == null) {
            return defaults();
        }
        return new GrapplingHookSounds(
            root.getBoolean("sounds.enabled", true),
            root.getInt("sounds.chain-interval-ticks", 6),
            SoundTuning.of(root, "sounds.fire", SoundTuning.FIRE),
            SoundTuning.of(root, "sounds.chain", SoundTuning.CHAIN),
            SoundTuning.of(root, "sounds.attach", SoundTuning.ATTACH),
            SoundTuning.of(root, "sounds.impulse", SoundTuning.IMPULSE),
            SoundTuning.of(root, "sounds.fail", SoundTuning.FAIL),
            SoundTuning.of(root, "sounds.cooldown", SoundTuning.COOLDOWN)
        );
    }
}

/**
 * Visual feedback tuning. Particle counts stay small on purpose: the chain is
 * the main animation and bursts are only one-shot accents at fire/attach/pull.
 */
record GrapplingHookFeedback(boolean enabled, int particleCount) {
    GrapplingHookFeedback {
        particleCount = GrapplingHookSettings.clampInt(particleCount, 1, 32);
    }

    static GrapplingHookFeedback defaults() {
        return new GrapplingHookFeedback(true, 6);
    }

    static GrapplingHookFeedback load(ConfigurationSection root) {
        if (root.getConfigurationSection("feedback") == null) {
            return defaults();
        }
        return new GrapplingHookFeedback(
            root.getBoolean("feedback.enabled", true),
            root.getInt("feedback.particle-count", 6)
        );
    }
}
