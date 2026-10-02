package dev.linqfy.bigCasares.modules.copperapple;

public record CopperAppleOxidationPolicy(
    long exposedAfterMillis,
    long weatheredAfterMillis,
    long oxidizedAfterMillis
) {
    public CopperAppleOxidationPolicy {
        if (exposedAfterMillis < 0L
            || weatheredAfterMillis < exposedAfterMillis
            || oxidizedAfterMillis < weatheredAfterMillis) {
            throw new IllegalArgumentException("Copper Apple oxidation thresholds must be non-negative and ordered");
        }
    }

    public static CopperAppleOxidationPolicy fromSeconds(
        long exposedAfterSeconds,
        long weatheredAfterSeconds,
        long oxidizedAfterSeconds
    ) {
        return new CopperAppleOxidationPolicy(
            Math.multiplyExact(exposedAfterSeconds, 1_000L),
            Math.multiplyExact(weatheredAfterSeconds, 1_000L),
            Math.multiplyExact(oxidizedAfterSeconds, 1_000L)
        );
    }

    public CopperAppleOxidationStage stageAt(long createdAtMillis, long nowMillis) {
        if (nowMillis <= createdAtMillis) {
            return CopperAppleOxidationStage.FRESH;
        }
        long ageMillis = nowMillis - createdAtMillis;
        if (ageMillis >= oxidizedAfterMillis) {
            return CopperAppleOxidationStage.OXIDIZED;
        }
        if (ageMillis >= weatheredAfterMillis) {
            return CopperAppleOxidationStage.WEATHERED;
        }
        if (ageMillis >= exposedAfterMillis) {
            return CopperAppleOxidationStage.EXPOSED;
        }
        return CopperAppleOxidationStage.FRESH;
    }

    public long millisUntilNextStage(long createdAtMillis, long nowMillis) {
        long ageMillis = Math.max(0L, nowMillis - createdAtMillis);
        if (ageMillis < exposedAfterMillis) {
            return exposedAfterMillis - ageMillis;
        }
        if (ageMillis < weatheredAfterMillis) {
            return weatheredAfterMillis - ageMillis;
        }
        if (ageMillis < oxidizedAfterMillis) {
            return oxidizedAfterMillis - ageMillis;
        }
        return 0L;
    }
}
