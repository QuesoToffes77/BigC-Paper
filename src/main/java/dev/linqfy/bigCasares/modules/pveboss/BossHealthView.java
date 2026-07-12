package dev.linqfy.bigCasares.modules.pveboss;

import java.util.Objects;
import java.util.UUID;

public record BossHealthView(UUID bossInstanceId, double currentHealth, double maxHealth) {

    public BossHealthView {
        Objects.requireNonNull(bossInstanceId, "bossInstanceId");
        if (!Double.isFinite(currentHealth) || currentHealth < 0.0) {
            throw new IllegalArgumentException("currentHealth must be finite and non-negative");
        }
        if (!Double.isFinite(maxHealth) || maxHealth <= 0.0) {
            throw new IllegalArgumentException("maxHealth must be finite and positive");
        }
    }

    public double progress() {
        return Math.max(0.0, Math.min(1.0, currentHealth / maxHealth));
    }
}
