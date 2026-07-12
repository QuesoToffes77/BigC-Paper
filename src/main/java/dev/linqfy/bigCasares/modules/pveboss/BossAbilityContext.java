package dev.linqfy.bigCasares.modules.pveboss;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record BossAbilityContext(
    BossPosition bossPosition,
    double health,
    double maxHealth,
    int phase,
    List<BossTargetCandidate> targets,
    UUID currentTargetId
) {

    public BossAbilityContext {
        Objects.requireNonNull(bossPosition, "bossPosition");
        Objects.requireNonNull(targets, "targets");
        if (!Double.isFinite(health) || health < 0.0) {
            throw new IllegalArgumentException("health must be finite and non-negative");
        }
        if (!Double.isFinite(maxHealth) || maxHealth <= 0.0) {
            throw new IllegalArgumentException("maxHealth must be finite and positive");
        }
        if (phase < 1) {
            throw new IllegalArgumentException("phase must be positive");
        }
        targets = List.copyOf(targets);
    }

    public double healthRatio() {
        return health / maxHealth;
    }

    public int eligiblePlayerCount() {
        return (int) targets.stream().filter(BossTargetCandidate::eligible).count();
    }

    public Optional<UUID> currentTarget() {
        return Optional.ofNullable(currentTargetId);
    }
}
