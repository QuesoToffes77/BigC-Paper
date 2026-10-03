package dev.linqfy.bigCasares.modules.copperapple;

public final class CopperAppleStackingPolicy {

    private CopperAppleStackingPolicy() {
    }

    public static long canonicalCreatedAt(long nowMillis, long windowMillis) {
        if (windowMillis <= 0L) {
            return Math.max(0L, nowMillis);
        }
        long safeNow = Math.max(0L, nowMillis);
        return safeNow - Math.floorMod(safeNow, windowMillis);
    }

    public static long oldestCreatedAt(long first, long second) {
        if (first < 0L) {
            return second;
        }
        if (second < 0L) {
            return first;
        }
        return Math.min(first, second);
    }
}
