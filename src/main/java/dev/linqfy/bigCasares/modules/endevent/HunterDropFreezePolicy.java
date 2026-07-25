package dev.linqfy.bigCasares.modules.endevent;

import java.time.Duration;
import java.time.Instant;

final class HunterDropFreezePolicy {
    static final Duration FREEZE_DURATION = Duration.ofSeconds(5);
    static final Duration COOLDOWN = Duration.ofSeconds(65);
    static final double RADIUS_SQUARED = 25.0 * 25.0;

    private HunterDropFreezePolicy() {
    }

    static Instant nextUseAt(Instant usedAt) {
        return usedAt.plus(COOLDOWN);
    }

    static boolean isReady(Instant nextUseAt, Instant now) {
        return nextUseAt == null || !now.isBefore(nextUseAt);
    }

    static long remainingSeconds(Instant nextUseAt, Instant now) {
        if (isReady(nextUseAt, now)) {
            return 0L;
        }
        long millis = Duration.between(now, nextUseAt).toMillis();
        return Math.max(0L, (millis + 999L) / 1000L);
    }

    static boolean isInRangeSquared(double distanceSquared) {
        return distanceSquared <= RADIUS_SQUARED;
    }
}
