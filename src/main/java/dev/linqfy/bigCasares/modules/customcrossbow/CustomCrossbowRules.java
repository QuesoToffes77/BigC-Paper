package dev.linqfy.bigCasares.modules.customcrossbow;

import org.bukkit.Material;

import java.util.Optional;

public final class CustomCrossbowRules {

    public static final double NORMAL_ARROW_DAMAGE = 6.0;
    public static final double ECHO_SHARD_DAMAGE = 7.0;
    public static final double ENDER_PEARL_VELOCITY_MULTIPLIER = 2.25;

    private CustomCrossbowRules() {
    }

    public static Optional<CustomCrossbowChargeType> resolveCharge(Material offhandMaterial) {
        return switch (offhandMaterial) {
            case ECHO_SHARD -> Optional.of(CustomCrossbowChargeType.ECHO_SHARD);
            case FIREWORK_ROCKET -> Optional.of(CustomCrossbowChargeType.FIREWORK_ROCKET);
            case AMETHYST_SHARD -> Optional.of(CustomCrossbowChargeType.AMETHYST_SHARD);
            // case ENDER_PEARL -> Optional.of(CustomCrossbowChargeType.ENDER_PEARL);
            default -> Optional.empty();
        };
    }

    public static CustomCrossbowShotPlan planFor(CustomCrossbowChargeType chargeType) {
        return switch (chargeType) {
            case ECHO_SHARD -> new CustomCrossbowShotPlan(true, false, true, false, false, ECHO_SHARD_DAMAGE, 1.0);
            case FIREWORK_ROCKET -> new CustomCrossbowShotPlan(false, true, false, false, false, 0.0, 1.0);
            case AMETHYST_SHARD -> new CustomCrossbowShotPlan(false, true, false, false, true, 0.0, 1.0);
            case ENDER_PEARL -> new CustomCrossbowShotPlan(true, false, false, true, false, 0.0, ENDER_PEARL_VELOCITY_MULTIPLIER);
        };
    }

    public static double amethystDamage(double normalArrowDamage) {
        return normalArrowDamage * 1.5;
    }

    public static double rocketJumpVelocity(int fireworkPower, int chargedProjectiles) {
        int clampedPower = Math.max(0, fireworkPower);
        int clampedProjectiles = Math.max(1, chargedProjectiles);
        return 0.65 + (clampedPower * 0.25) + ((clampedProjectiles - 1) * 0.30);
    }

    public static double rocketJumpDamage(int fireworkPower, int chargedProjectiles) {
        int clampedPower = Math.max(0, fireworkPower);
        int clampedProjectiles = Math.max(1, chargedProjectiles);
        return 2.0 + clampedPower + ((clampedProjectiles - 1) * 1.0);
    }

    public static int remainingOffhandAmountAfterCharge(int currentAmount) {
        return Math.max(0, currentAmount - 1);
    }

    public static String loadedCrossbowModelId(CustomCrossbowChargeType chargeType) {
        return switch (chargeType) {
            case ECHO_SHARD -> "crossbow_echo_shard";
            case FIREWORK_ROCKET -> "crossbow_firework_rocket";
            case AMETHYST_SHARD -> "crossbow_amethyst_shard";
            case ENDER_PEARL -> "crossbow_ender_pearl";
        };
    }

    public static int loadTicks(CustomCrossbowChargeType chargeType, int quickChargeLevel) {
        int baseTicks = switch (chargeType) {
            case ECHO_SHARD -> 45;
            case ENDER_PEARL -> 35;
            case AMETHYST_SHARD -> 30;
            case FIREWORK_ROCKET -> 25;
        };
        return Math.max(10, baseTicks - Math.max(0, quickChargeLevel) * 5);
    }

    public static int durabilityCost(CustomCrossbowChargeType chargeType) {
        return switch (chargeType) {
            case ECHO_SHARD -> 5;
            case ENDER_PEARL -> 4;
            case AMETHYST_SHARD, FIREWORK_ROCKET -> 2;
        };
    }

    public static int prismarineArrowDurabilityCost() {
        return 2;
    }

    public static int defaultEchoShardCooldownTicks() {
        return 100;
    }
}
