package dev.linqfy.bigCasares.modules.acidrain;

import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Pure domain logic for Acid Rain mob spawning. Owns the logical scheduler
 * lifecycle (start/stop, idempotent, interval accumulation) and the spawn gate
 * evaluated against the real {@link AcidRainSnapshot}:
 *
 * <pre>
 * event active AND phase == ACTIVE
 * </pre>
 *
 * <p>No server dependency: the runtime feeds the snapshot, the live active-mob
 * count and the cadence ticks, and performs the actual spawning.
 */
public final class AcidRainMobSpawnService {

    private final Random random;
    private AcidRainMobSettings settings;
    private boolean running;
    private long elapsedTicks;

    public AcidRainMobSpawnService(AcidRainMobSettings settings) {
        this(settings, new Random());
    }

    AcidRainMobSpawnService(AcidRainMobSettings settings, Random random) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.random = Objects.requireNonNull(random, "random");
    }

    /** Starts the logical scheduler. Idempotent: a second start is a no-op. */
    public synchronized boolean start() {
        if (running) {
            return false;
        }
        running = true;
        elapsedTicks = 0;
        return true;
    }

    /** Stops the logical scheduler. Idempotent: a second stop is a no-op. */
    public synchronized boolean stop() {
        if (!running) {
            return false;
        }
        running = false;
        elapsedTicks = 0;
        return true;
    }

    public synchronized boolean isRunning() {
        return running;
    }

    public synchronized void updateSettings(AcidRainMobSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    /**
     * Evaluates one cadence tick. Returns how many spawn attempts the runtime
     * may perform this cycle (0 means none), and why.
     */
    public synchronized AcidRainMobCycleDecision cycleDecision(
        AcidRainSnapshot storm,
        int activeMobCount,
        long cadenceTicks
    ) {
        if (!running) {
            return AcidRainMobCycleDecision.blocked(AcidRainMobSkipReason.STOPPED);
        }
        if (!settings.canSpawn()) {
            return AcidRainMobCycleDecision.blocked(AcidRainMobSkipReason.DISABLED);
        }
        if (storm == null || storm.state() != AcidRainState.ACTIVE) {
            return AcidRainMobCycleDecision.blocked(AcidRainMobSkipReason.STATE);
        }
        int safeActive = Math.max(0, activeMobCount);
        if (safeActive >= settings.maxActive()) {
            return AcidRainMobCycleDecision.blocked(AcidRainMobSkipReason.CAPACITY);
        }
        elapsedTicks += Math.max(1L, cadenceTicks);
        int interval = Math.max(1, settings.spawnIntervalTicks());
        if (elapsedTicks < interval) {
            return AcidRainMobCycleDecision.blocked(AcidRainMobSkipReason.INTERVAL);
        }
        elapsedTicks = elapsedTicks % interval;
        int attempts = Math.max(0, Math.min(settings.attemptsPerCycle(), settings.maxActive() - safeActive));
        return AcidRainMobCycleDecision.allowed(attempts);
    }

    /**
     * Picks a mob type weighted by {@link AcidRainMobType#weight()} among the
     * enabled types. Returns {@code null} only when no type is enabled.
     */
    public synchronized AcidRainMobType pickType() {
        List<AcidRainMobType> enabled = settings.enabledTypes();
        if (enabled.isEmpty()) {
            return null;
        }
        int totalWeight = enabled.stream().mapToInt(AcidRainMobType::weight).sum();
        if (totalWeight <= 0) {
            return enabled.get(random.nextInt(enabled.size()));
        }
        int roll = random.nextInt(totalWeight);
        for (AcidRainMobType type : enabled) {
            roll -= type.weight();
            if (roll < 0) {
                return type;
            }
        }
        return enabled.get(enabled.size() - 1);
    }
}

record AcidRainMobCycleDecision(boolean spawnAllowed, int attempts, AcidRainMobSkipReason reason) {

    static AcidRainMobCycleDecision blocked(AcidRainMobSkipReason reason) {
        return new AcidRainMobCycleDecision(false, 0, reason);
    }

    static AcidRainMobCycleDecision allowed(int attempts) {
        return new AcidRainMobCycleDecision(true, Math.max(0, attempts), AcidRainMobSkipReason.NONE);
    }
}

enum AcidRainMobSkipReason {
    NONE,
    STOPPED,
    DISABLED,
    STATE,
    LEVEL,
    CAPACITY,
    INTERVAL
}
