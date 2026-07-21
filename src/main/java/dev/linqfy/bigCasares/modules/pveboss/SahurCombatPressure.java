package dev.linqfy.bigCasares.modules.pveboss;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.random.RandomGenerator;

public final class SahurCombatPressure {
    private static final double SECOND_HIT_CHANCE = 0.35;
    private static final double THIRD_HIT_CHANCE = 0.15;

    private final Duration criticalHitWindow;
    private final int spinThreshold;
    private final Duration hitCooldown;
    private final Map<UUID, Instant> lastHits = new HashMap<>();

    public SahurCombatPressure(Duration criticalHitWindow, int spinThreshold, Duration hitCooldown) {
        this.criticalHitWindow = requirePositive(criticalHitWindow, "criticalHitWindow");
        if (spinThreshold < 1) {
            throw new IllegalArgumentException("spinThreshold must be positive");
        }
        this.spinThreshold = spinThreshold;
        this.hitCooldown = requirePositive(hitCooldown, "hitCooldown");
    }

    public boolean shouldSpin(int nearbyPlayers, List<Instant> criticalHits, Instant now) {
        Objects.requireNonNull(criticalHits, "criticalHits");
        Objects.requireNonNull(now, "now");
        if (nearbyPlayers >= spinThreshold) {
            return true;
        }
        Instant cutoff = now.minus(criticalHitWindow);
        long recentHits = criticalHits.stream()
            .map(hit -> Objects.requireNonNull(hit, "critical hit timestamp"))
            .filter(hit -> !hit.isBefore(cutoff) && !hit.isAfter(now))
            .count();
        return recentHits >= spinThreshold;
    }

    public int comboLength(RandomGenerator random) {
        Objects.requireNonNull(random, "random");
        if (random.nextDouble() >= SECOND_HIT_CHANCE) {
            return 1;
        }
        return random.nextDouble() < THIRD_HIT_CHANCE ? 3 : 2;
    }

    public synchronized boolean mayHit(UUID playerId, Instant now) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(now, "now");
        Instant previous = lastHits.get(playerId);
        if (previous != null && Duration.between(previous, now).compareTo(hitCooldown) < 0) {
            return false;
        }
        lastHits.put(playerId, now);
        return true;
    }

    private static Duration requirePositive(Duration duration, String name) {
        Objects.requireNonNull(duration, name);
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return duration;
    }
}
