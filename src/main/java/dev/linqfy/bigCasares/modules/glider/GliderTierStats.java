package dev.linqfy.bigCasares.modules.glider;

public record GliderTierStats(
    double forwardSpeed,
    double maxFallSpeed,
    double steering,
    double lift,
    boolean boostEnabled,
    double boostPower,
    int boostCooldownTicks,
    double fallDamageReduction
) {
    public GliderTierStats {
        forwardSpeed = clampFinite(forwardSpeed, 0.1, 1.2);
        maxFallSpeed = clampFinite(maxFallSpeed, -1.0, -0.03);
        steering = clampFinite(steering, 0.05, 1.0);
        lift = clampFinite(lift, 0.0, 0.05);
        boostPower = clampFinite(boostPower, 0.0, 1.2);
        boostCooldownTicks = Math.max(1, Math.min(boostCooldownTicks, 1_200));
        fallDamageReduction = clampFinite(fallDamageReduction, 0.0, 1.0);
    }

    public static GliderTierStats defaults(GliderTier tier) {
        return switch (tier) {
            case I -> new GliderTierStats(0.38, -0.42, 0.45, 0.0, false, 0.0, 80, 0.35);
            case II -> new GliderTierStats(0.44, -0.36, 0.55, 0.0, false, 0.0, 80, 0.50);
            case III -> new GliderTierStats(0.50, -0.30, 0.65, 0.01, true, 0.45, 80, 0.65);
            case IV -> new GliderTierStats(0.57, -0.25, 0.75, 0.015, true, 0.55, 65, 0.80);
            case V -> new GliderTierStats(0.64, -0.20, 0.85, 0.02, true, 0.65, 50, 0.90);
            case VI -> new GliderTierStats(0.72, -0.15, 0.95, 0.025, true, 0.78, 35, 1.00);
        };
    }

    public double reduceFallDamage(double damage) {
        return Math.max(0.0, damage * (1.0 - fallDamageReduction));
    }

    public boolean isStrictUpgradeFrom(GliderTierStats previous) {
        return forwardSpeed > previous.forwardSpeed
            && maxFallSpeed > previous.maxFallSpeed
            && steering > previous.steering
            && fallDamageReduction > previous.fallDamageReduction;
    }

    private static double clampFinite(double value, double minimum, double maximum) {
        if (!Double.isFinite(value)) {
            return minimum;
        }
        return Math.max(minimum, Math.min(value, maximum));
    }
}
