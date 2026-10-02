package dev.linqfy.bigCasares.modules.jeremy;

import java.util.Objects;
import java.util.UUID;

final class JeremyCycle {
    private final JeremyTimingSettings timing;
    private JeremyPhase phase;
    private UUID targetUuid;
    private UUID lastTargetUuid;
    private long phaseEndsAtMillis;

    private JeremyCycle(JeremyTimingSettings timing, JeremySnapshot snapshot) {
        this.timing = Objects.requireNonNull(timing, "timing");
        this.phase = snapshot.phase();
        this.targetUuid = snapshot.targetUuid();
        this.lastTargetUuid = snapshot.lastTargetUuid();
        this.phaseEndsAtMillis = snapshot.phaseEndsAtMillis();
    }

    static JeremyCycle resting(JeremyTimingSettings timing, long restUntilMillis, UUID lastTargetUuid) {
        return new JeremyCycle(timing,
            new JeremySnapshot(JeremyPhase.RESTING, null, lastTargetUuid, restUntilMillis));
    }

    static JeremyCycle restore(JeremyTimingSettings timing, JeremySnapshot snapshot) {
        JeremySnapshot safe = snapshot == null
            ? new JeremySnapshot(JeremyPhase.RESTING, null, null, 0L)
            : snapshot;
        if (safe.phase() != JeremyPhase.RESTING && safe.targetUuid() == null) {
            safe = new JeremySnapshot(JeremyPhase.RESTING, null, safe.lastTargetUuid(), safe.phaseEndsAtMillis());
        }
        return new JeremyCycle(timing, safe);
    }

    boolean canStart(long nowMillis) {
        return phase == JeremyPhase.RESTING && nowMillis >= phaseEndsAtMillis;
    }

    boolean startHunt(UUID selectedTarget, long nowMillis) {
        if (!canStart(nowMillis) || selectedTarget == null) {
            return false;
        }
        phase = JeremyPhase.HUNTING;
        targetUuid = selectedTarget;
        lastTargetUuid = selectedTarget;
        phaseEndsAtMillis = safeAdd(nowMillis, timing.huntMillis());
        return true;
    }

    boolean forceStart(UUID selectedTarget, long nowMillis) {
        if (phase != JeremyPhase.RESTING || selectedTarget == null) {
            return false;
        }
        phaseEndsAtMillis = nowMillis;
        return startHunt(selectedTarget, nowMillis);
    }

    JeremyTransition advance(long nowMillis) {
        if (nowMillis < phaseEndsAtMillis) {
            return JeremyTransition.NONE;
        }
        return switch (phase) {
            case HUNTING -> JeremyTransition.HUNT_TIMEOUT;
            case CELEBRATING -> JeremyTransition.CELEBRATION_FINISHED;
            case RESTING -> JeremyTransition.REST_FINISHED;
        };
    }

    boolean startCelebration(long nowMillis) {
        if (phase != JeremyPhase.HUNTING) {
            return false;
        }
        phase = JeremyPhase.CELEBRATING;
        phaseEndsAtMillis = safeAdd(nowMillis, timing.celebrationMillis());
        return true;
    }

    void finishCelebration(long nowMillis) {
        if (phase == JeremyPhase.CELEBRATING) {
            enterRest(nowMillis);
        }
    }

    void finishHunt(long nowMillis) {
        if (phase == JeremyPhase.HUNTING || phase == JeremyPhase.CELEBRATING) {
            enterRest(nowMillis);
        }
    }

    void retryWithoutPlayer(long nowMillis) {
        if (phase == JeremyPhase.RESTING) {
            phaseEndsAtMillis = safeAdd(nowMillis, timing.noPlayerRetryMillis());
        }
    }

    void resetCooldown(long nowMillis) {
        if (phase == JeremyPhase.RESTING) {
            phaseEndsAtMillis = nowMillis;
        }
    }

    JeremyPhase phase() {
        return phase;
    }

    UUID targetUuid() {
        return targetUuid;
    }

    UUID lastTargetUuid() {
        return lastTargetUuid;
    }

    long phaseEndsAtMillis() {
        return phaseEndsAtMillis;
    }

    long restUntilMillis() {
        return phase == JeremyPhase.RESTING ? phaseEndsAtMillis : 0L;
    }

    JeremySnapshot snapshot() {
        return new JeremySnapshot(phase, targetUuid, lastTargetUuid, phaseEndsAtMillis);
    }

    private void enterRest(long nowMillis) {
        phase = JeremyPhase.RESTING;
        targetUuid = null;
        phaseEndsAtMillis = safeAdd(nowMillis, timing.restMillis());
    }

    private static long safeAdd(long value, long delta) {
        try {
            return Math.addExact(value, Math.max(0L, delta));
        } catch (ArithmeticException ignored) {
            return Long.MAX_VALUE;
        }
    }
}

enum JeremyPhase {
    RESTING,
    HUNTING,
    CELEBRATING
}

enum JeremyTransition {
    NONE,
    REST_FINISHED,
    HUNT_TIMEOUT,
    CELEBRATION_FINISHED
}

record JeremySnapshot(JeremyPhase phase, UUID targetUuid, UUID lastTargetUuid, long phaseEndsAtMillis) {
    JeremySnapshot {
        phase = phase == null ? JeremyPhase.RESTING : phase;
        phaseEndsAtMillis = Math.max(0L, phaseEndsAtMillis);
    }
}
