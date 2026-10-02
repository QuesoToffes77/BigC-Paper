package dev.linqfy.bigCasares.modules.grapplinghook;

import java.util.Objects;

/**
 * Pure per-shot feedback plan: which sound (with volume/pitch) and how many
 * particles to use for a given tier. Higher tiers sound slightly more
 * powerful via a small scale factor on volume and pitch. When sounds are
 * disabled every sound accessor returns {@code null} and the runtime skips
 * playback entirely.
 */
public final class GrappleFeedbackPlan {

    /** Volume/pitch growth per tier step, kept deliberately small. */
    private static final float TIER_SCALE = 0.03f;

    private final GrapplingHookSettings settings;
    private final GrapplingHookTier tier;

    public GrappleFeedbackPlan(GrapplingHookSettings settings, GrapplingHookTier tier) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.tier = Objects.requireNonNull(tier, "tier");
    }

    public boolean soundsEnabled() {
        return settings.sounds().enabled();
    }

    public boolean feedbackEnabled() {
        return settings.feedback().enabled();
    }

    public int particleCount() {
        return settings.feedback().particleCount();
    }

    public int chainIntervalTicks() {
        return settings.sounds().chainIntervalTicks();
    }

    public SoundTuning fire() {
        return scaled(settings.sounds().fire());
    }

    public SoundTuning chain() {
        return scaled(settings.sounds().chain());
    }

    public SoundTuning attach() {
        return scaled(settings.sounds().attach());
    }

    public SoundTuning impulse() {
        return scaled(settings.sounds().impulse());
    }

    public SoundTuning fail() {
        return scaled(settings.sounds().fail());
    }

    public SoundTuning cooldown() {
        return scaled(settings.sounds().cooldown());
    }

    private SoundTuning scaled(SoundTuning base) {
        if (!settings.sounds().enabled()) {
            return null;
        }
        float scale = 1.0f + tier.ordinal() * TIER_SCALE;
        return new SoundTuning(base.sound(), base.volume() * scale, base.pitch() * scale);
    }
}
