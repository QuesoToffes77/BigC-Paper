package dev.linqfy.bigCasares.modules.jeremy;

final class JeremyUltrasoundController {
    private final int chargeTicks;
    private final int cooldownTicks;
    private JeremyUltrasoundPhase phase = JeremyUltrasoundPhase.IDLE;
    private long phaseEndsAtTick;

    JeremyUltrasoundController(int chargeTicks, int cooldownTicks) {
        this.chargeTicks = Math.max(1, chargeTicks);
        this.cooldownTicks = Math.max(1, cooldownTicks);
    }

    boolean beginCharge(long currentTick, boolean stuck) {
        if (!stuck || phase == JeremyUltrasoundPhase.CHARGING
            || phase == JeremyUltrasoundPhase.COOLDOWN && currentTick < phaseEndsAtTick) {
            return false;
        }
        phase = JeremyUltrasoundPhase.CHARGING;
        phaseEndsAtTick = currentTick + chargeTicks;
        return true;
    }

    JeremyUltrasoundTransition advance(long currentTick, boolean stuck, boolean targetValid) {
        if (phase == JeremyUltrasoundPhase.COOLDOWN && currentTick >= phaseEndsAtTick) {
            phase = JeremyUltrasoundPhase.IDLE;
        }
        if (phase != JeremyUltrasoundPhase.CHARGING) {
            return JeremyUltrasoundTransition.NONE;
        }
        if (!stuck || !targetValid) {
            phase = JeremyUltrasoundPhase.IDLE;
            phaseEndsAtTick = currentTick;
            return JeremyUltrasoundTransition.CANCELLED;
        }
        if (currentTick < phaseEndsAtTick) {
            return JeremyUltrasoundTransition.NONE;
        }
        phase = JeremyUltrasoundPhase.COOLDOWN;
        phaseEndsAtTick = currentTick + cooldownTicks;
        return JeremyUltrasoundTransition.FIRE;
    }

    JeremyUltrasoundPhase phase() {
        return phase;
    }

    void reset() {
        phase = JeremyUltrasoundPhase.IDLE;
        phaseEndsAtTick = 0L;
    }
}

enum JeremyUltrasoundPhase {
    IDLE,
    CHARGING,
    COOLDOWN
}

enum JeremyUltrasoundTransition {
    NONE,
    FIRE,
    CANCELLED
}
