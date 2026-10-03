package dev.linqfy.bigCasares.modules.copperapple;

public final class CopperAppleBalance {

    public static final int NUTRITION = 4;
    public static final float SATURATION = 2.4f;

    private CopperAppleBalance() {
    }

    public static CopperAppleEffectProfile profileFor(CopperAppleOxidationStage stage) {
        return switch (stage) {
            case FRESH -> new CopperAppleEffectProfile(
                0, 20 * 30, 0, 20 * 8, 0, 0, 0, 0, 0, 0, 0, 0
            );
            case EXPOSED -> new CopperAppleEffectProfile(
                0, 20 * 45, 0, 20 * 12, 0, 20 * 20, 0, 0, 0, 0, 0, 15
            );
            case WEATHERED -> new CopperAppleEffectProfile(
                1, 20 * 60, 1, 20 * 12, 1, 20 * 45, 0, 20 * 30, 0, 20 * 10, 0, 45
            );
            case OXIDIZED -> new CopperAppleEffectProfile(
                1, 20 * 90, 1, 20 * 16, 1, 20 * 90, 0, 20 * 60, 0, 20 * 20, 0, 100
            );
        };
    }
}
