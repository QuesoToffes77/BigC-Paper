package dev.linqfy.bigCasares.modules.customcrossbow;

public final class PrismarineArrowBalance {

    private static final double WATER_DAMAGE_MULTIPLIER = 1.3;
    private static final double DRY_DAMAGE_MULTIPLIER = 0.85;
    private static final double WATER_VELOCITY_MULTIPLIER = 3.25;
    private static final double DRY_VELOCITY_MULTIPLIER = 0.85;

    private PrismarineArrowBalance() {
    }

    public static double damage(double baseDamage, boolean underwater) {
        return baseDamage * (underwater ? WATER_DAMAGE_MULTIPLIER : DRY_DAMAGE_MULTIPLIER);
    }

    public static double velocityMultiplier(boolean underwater) {
        return underwater ? WATER_VELOCITY_MULTIPLIER : DRY_VELOCITY_MULTIPLIER;
    }
}
