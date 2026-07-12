package dev.linqfy.bigCasares.modules.nexus;

import java.util.Objects;

public record NexusDamageMultipliers(
        double melee,
        double projectile,
        double explosion,
        double fire,
        double magic,
        double environment,
        double custom
) {

    public NexusDamageMultipliers {
        validate("melee", melee);
        validate("projectile", projectile);
        validate("explosion", explosion);
        validate("fire", fire);
        validate("magic", magic);
        validate("environment", environment);
        validate("custom", custom);
    }

    public static NexusDamageMultipliers defaults() {
        return new NexusDamageMultipliers(1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0);
    }

    public double forKind(NexusDamageKind kind) {
        return switch (Objects.requireNonNull(kind, "kind")) {
            case MELEE -> melee;
            case PROJECTILE -> projectile;
            case EXPLOSION -> explosion;
            case FIRE -> fire;
            case MAGIC -> magic;
            case ENVIRONMENT -> environment;
            case CUSTOM -> custom;
            case UNKNOWN -> 0.0;
        };
    }

    public NexusDamageMultipliers with(NexusDamageKind kind, double multiplier) {
        validate(kind.name().toLowerCase(), multiplier);
        return switch (kind) {
            case MELEE -> new NexusDamageMultipliers(multiplier, projectile, explosion, fire, magic, environment, custom);
            case PROJECTILE -> new NexusDamageMultipliers(melee, multiplier, explosion, fire, magic, environment, custom);
            case EXPLOSION -> new NexusDamageMultipliers(melee, projectile, multiplier, fire, magic, environment, custom);
            case FIRE -> new NexusDamageMultipliers(melee, projectile, explosion, multiplier, magic, environment, custom);
            case MAGIC -> new NexusDamageMultipliers(melee, projectile, explosion, fire, multiplier, environment, custom);
            case ENVIRONMENT -> new NexusDamageMultipliers(melee, projectile, explosion, fire, magic, multiplier, custom);
            case CUSTOM -> new NexusDamageMultipliers(melee, projectile, explosion, fire, magic, environment, multiplier);
            case UNKNOWN -> this;
        };
    }

    private static void validate(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " multiplier must be finite and non-negative");
        }
    }
}
