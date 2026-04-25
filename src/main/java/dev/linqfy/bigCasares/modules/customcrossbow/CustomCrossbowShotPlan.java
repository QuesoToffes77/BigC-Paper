package dev.linqfy.bigCasares.modules.customcrossbow;

public record CustomCrossbowShotPlan(
    boolean cancelVanillaProjectile,
    boolean usesVanillaProjectile,
    boolean hitscan,
    boolean spawnsEnderPearl,
    boolean overwritesEnchantmentDamage,
    double directDamage,
    double velocityMultiplier
) {
}
