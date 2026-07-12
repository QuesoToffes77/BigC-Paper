package dev.linqfy.bigCasares.modules.pveboss;

import java.util.Objects;

public record BossTelegraphDefinition(
    BossTelegraphType type,
    double radius,
    String particle,
    String warningSound
) {

    public BossTelegraphDefinition {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(particle, "particle");
        Objects.requireNonNull(warningSound, "warningSound");
        if (!Double.isFinite(radius) || radius < 0.0) {
            throw new IllegalArgumentException("telegraph radius must be finite and non-negative");
        }
    }

    public static BossTelegraphDefinition of(BossTelegraphType type) {
        return new BossTelegraphDefinition(type, 0.0, "", "");
    }
}
