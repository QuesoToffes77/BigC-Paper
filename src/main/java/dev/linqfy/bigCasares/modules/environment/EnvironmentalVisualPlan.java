package dev.linqfy.bigCasares.modules.environment;

import java.util.Objects;

public record EnvironmentalVisualPlan(EnvironmentalVisualProfile profile, int particleBudget) {
    public static final int HARD_MAX_PARTICLES_PER_PLAYER_CYCLE = 36;

    public EnvironmentalVisualPlan {
        profile = Objects.requireNonNull(profile, "profile");
        particleBudget = Math.max(0, Math.min(HARD_MAX_PARTICLES_PER_PLAYER_CYCLE, particleBudget));
    }

    public static EnvironmentalVisualPlan forProfile(
        EnvironmentalVisualProfile profile,
        EnvironmentalVisualQuality quality
    ) {
        Objects.requireNonNull(profile, "profile");
        EnvironmentalVisualQuality resolved = quality == null ? EnvironmentalVisualQuality.MEDIUM : quality;
        int base = switch (profile) {
            case ACID_RAIN -> 18;
            case CHEMICAL_FOG -> 20;
            case TOXIC_SPORES -> 14;
            case BLOOD_MOON -> 10;
        };
        return new EnvironmentalVisualPlan(profile, (int) Math.ceil(base * resolved.densityMultiplier()));
    }
}
