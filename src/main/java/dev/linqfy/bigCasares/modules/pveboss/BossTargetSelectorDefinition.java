package dev.linqfy.bigCasares.modules.pveboss;

import java.util.Objects;

public record BossTargetSelectorDefinition(
    BossTargetSelectorType type,
    double radius,
    int maxTargets
) {

    public BossTargetSelectorDefinition {
        Objects.requireNonNull(type, "type");
        if (Double.isNaN(radius) || radius < 0.0) {
            throw new IllegalArgumentException("radius must be non-negative");
        }
        if (maxTargets < 1) {
            throw new IllegalArgumentException("maxTargets must be positive");
        }
    }

    public static BossTargetSelectorDefinition of(BossTargetSelectorType type) {
        int limit = type == BossTargetSelectorType.ALL_IN_RADIUS ? Integer.MAX_VALUE : 1;
        return new BossTargetSelectorDefinition(type, Double.POSITIVE_INFINITY, limit);
    }

    public static BossTargetSelectorDefinition allInRadius(double radius) {
        return new BossTargetSelectorDefinition(BossTargetSelectorType.ALL_IN_RADIUS, radius, Integer.MAX_VALUE);
    }
}
