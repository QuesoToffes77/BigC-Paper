package dev.linqfy.bigCasares.modules.copperapple;

public record CopperAppleEffectProfile(
    int instantHealthAmplifier,
    int absorptionTicks,
    int absorptionAmplifier,
    int regenerationTicks,
    int regenerationAmplifier,
    int speedTicks,
    int speedAmplifier,
    int strengthTicks,
    int strengthAmplifier,
    int resistanceTicks,
    int resistanceAmplifier,
    int lightningChancePercent
) {
    public CopperAppleEffectProfile {
        if (instantHealthAmplifier < 0
            || absorptionTicks < 0
            || absorptionAmplifier < 0
            || regenerationTicks < 0
            || regenerationAmplifier < 0
            || speedTicks < 0
            || speedAmplifier < 0
            || strengthTicks < 0
            || strengthAmplifier < 0
            || resistanceTicks < 0
            || resistanceAmplifier < 0) {
            throw new IllegalArgumentException("Copper Apple effects cannot be negative");
        }
        if (lightningChancePercent < 0 || lightningChancePercent > 100) {
            throw new IllegalArgumentException("Lightning chance must be between 0 and 100");
        }
    }
}
