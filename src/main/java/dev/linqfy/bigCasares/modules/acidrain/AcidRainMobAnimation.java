package dev.linqfy.bigCasares.modules.acidrain;

/** Stable BetterModel animation keys and their short one-shot exclusion windows. */
enum AcidRainMobAnimation {
    IDLE("idle", false, 0L),
    WALK("walk", false, 0L),
    ATTACK("attack", true, 12L),
    HURT("hurt", true, 7L);

    private static final double WALKING_SPEED_SQUARED = 0.01;

    private final String key;
    private final boolean oneShot;
    private final long lockTicks;

    AcidRainMobAnimation(String key, boolean oneShot, long lockTicks) {
        this.key = key;
        this.oneShot = oneShot;
        this.lockTicks = lockTicks;
    }

    String key() {
        return key;
    }

    boolean oneShot() {
        return oneShot;
    }

    long lockTicks() {
        return lockTicks;
    }

    static AcidRainMobAnimation loopFor(double horizontalVelocitySquared) {
        return horizontalVelocitySquared >= WALKING_SPEED_SQUARED ? WALK : IDLE;
    }
}
