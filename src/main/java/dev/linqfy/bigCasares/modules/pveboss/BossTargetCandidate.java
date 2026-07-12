package dev.linqfy.bigCasares.modules.pveboss;

import java.util.Objects;
import java.util.UUID;

public record BossTargetCandidate(
    UUID targetId,
    BossPosition position,
    double health,
    double maxHealth,
    double damageDealt,
    boolean eligible
) {

    public BossTargetCandidate {
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(position, "position");
        if (!Double.isFinite(health) || health < 0.0) {
            throw new IllegalArgumentException("health must be finite and non-negative");
        }
        if (!Double.isFinite(maxHealth) || maxHealth <= 0.0) {
            throw new IllegalArgumentException("maxHealth must be finite and positive");
        }
        if (!Double.isFinite(damageDealt) || damageDealt < 0.0) {
            throw new IllegalArgumentException("damageDealt must be finite and non-negative");
        }
    }

    public double healthRatio() {
        return health / maxHealth;
    }
}
