package dev.linqfy.bigCasares.modules.pveboss;

import java.util.Objects;

public record BossAbilityCondition(BossAbilityConditionType type, double value) {

    public BossAbilityCondition {
        Objects.requireNonNull(type, "type");
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("condition value must be finite");
        }
        if ((type == BossAbilityConditionType.HEALTH_BELOW
            || type == BossAbilityConditionType.HEALTH_ABOVE
            || type == BossAbilityConditionType.RANDOM_CHANCE)
            && (value < 0.0 || value > 1.0)) {
            throw new IllegalArgumentException(type + " value must be between 0 and 1");
        }
        if ((type == BossAbilityConditionType.PHASE_EQUALS
            || type == BossAbilityConditionType.PLAYERS_AT_LEAST)
            && (value < 1.0 || value != Math.rint(value))) {
            throw new IllegalArgumentException(type + " value must be a positive integer");
        }
        if (type == BossAbilityConditionType.TARGET_IN_RANGE && value < 0.0) {
            throw new IllegalArgumentException("TARGET_IN_RANGE value must be non-negative");
        }
    }

    public static BossAbilityCondition noValue(BossAbilityConditionType type) {
        if (type != BossAbilityConditionType.NOT_CASTING && type != BossAbilityConditionType.COOLDOWN_READY) {
            throw new IllegalArgumentException(type + " requires a value");
        }
        return new BossAbilityCondition(type, 0.0);
    }
}
