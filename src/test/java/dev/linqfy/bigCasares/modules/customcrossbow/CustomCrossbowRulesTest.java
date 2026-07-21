package dev.linqfy.bigCasares.modules.customcrossbow;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomCrossbowRulesTest {

    @Test
    void resolvesSpecialChargeFromOffhandMaterial() {
        assertEquals(Optional.of(CustomCrossbowChargeType.ECHO_SHARD), CustomCrossbowRules.resolveCharge(Material.ECHO_SHARD));
        assertEquals(Optional.of(CustomCrossbowChargeType.FIREWORK_ROCKET), CustomCrossbowRules.resolveCharge(Material.FIREWORK_ROCKET));
        assertEquals(Optional.of(CustomCrossbowChargeType.AMETHYST_SHARD), CustomCrossbowRules.resolveCharge(Material.AMETHYST_SHARD));
        // assertEquals(Optional.of(CustomCrossbowChargeType.ENDER_PEARL), CustomCrossbowRules.resolveCharge(Material.ENDER_PEARL));
        assertEquals(Optional.empty(), CustomCrossbowRules.resolveCharge(Material.AIR));
    }

    @Test
    void echoShardShotIsHitscanAndCancelsVanillaProjectile() {
        CustomCrossbowShotPlan plan = CustomCrossbowRules.planFor(CustomCrossbowChargeType.ECHO_SHARD);

        assertTrue(plan.cancelVanillaProjectile());
        assertTrue(plan.hitscan());
        assertFalse(plan.usesVanillaProjectile());
        assertTrue(plan.directDamage() >= 6.0);
        assertTrue(plan.directDamage() <= 8.0);
    }

    @Test
    void fireworkShotKeepsVanillaProjectileAndAddsRocketJump() {
        CustomCrossbowShotPlan plan = CustomCrossbowRules.planFor(CustomCrossbowChargeType.FIREWORK_ROCKET);

        assertFalse(plan.cancelVanillaProjectile());
        assertTrue(plan.usesVanillaProjectile());
        assertEquals(1.45, CustomCrossbowRules.rocketJumpVelocity(2, 2), 0.0001);
    }

    @Test
    void amethystShotUsesArrowButOverwritesEnchantScaling() {
        CustomCrossbowShotPlan plan = CustomCrossbowRules.planFor(CustomCrossbowChargeType.AMETHYST_SHARD);

        assertFalse(plan.cancelVanillaProjectile());
        assertTrue(plan.usesVanillaProjectile());
        assertTrue(plan.overwritesEnchantmentDamage());
        assertEquals(9.0, CustomCrossbowRules.amethystDamage(6.0), 0.0001);
    }

    @Test
    void enderPearlShotCancelsArrowAndSpawnsFastPearl() {
        CustomCrossbowShotPlan plan = CustomCrossbowRules.planFor(CustomCrossbowChargeType.ENDER_PEARL);

        assertTrue(plan.cancelVanillaProjectile());
        assertFalse(plan.usesVanillaProjectile());
        assertTrue(plan.spawnsEnderPearl());
        assertEquals(2.25, plan.velocityMultiplier(), 0.0001);
    }

    @Test
    void consumingChargeItemRemovesExactlyOneUnit() {
        assertEquals(0, CustomCrossbowRules.remainingOffhandAmountAfterCharge(1));
        assertEquals(4, CustomCrossbowRules.remainingOffhandAmountAfterCharge(5));
        assertEquals(0, CustomCrossbowRules.remainingOffhandAmountAfterCharge(0));
    }

    @Test
    void exposesDedicatedLoadedCrossbowModelsByChargeType() {
        assertEquals("crossbow_echo_shard", CustomCrossbowRules.loadedCrossbowModelId(CustomCrossbowChargeType.ECHO_SHARD));
        assertEquals("crossbow_firework_rocket", CustomCrossbowRules.loadedCrossbowModelId(CustomCrossbowChargeType.FIREWORK_ROCKET));
        assertEquals("crossbow_amethyst_shard", CustomCrossbowRules.loadedCrossbowModelId(CustomCrossbowChargeType.AMETHYST_SHARD));
        assertEquals("crossbow_ender_pearl", CustomCrossbowRules.loadedCrossbowModelId(CustomCrossbowChargeType.ENDER_PEARL));
    }

    @Test
    void specialChargeLoadTimesUsePerItemDefaultsAndQuickChargeFloor() {
        assertEquals(45, CustomCrossbowRules.loadTicks(CustomCrossbowChargeType.ECHO_SHARD, 0));
        assertEquals(35, CustomCrossbowRules.loadTicks(CustomCrossbowChargeType.ENDER_PEARL, 0));
        assertEquals(30, CustomCrossbowRules.loadTicks(CustomCrossbowChargeType.AMETHYST_SHARD, 0));
        assertEquals(25, CustomCrossbowRules.loadTicks(CustomCrossbowChargeType.FIREWORK_ROCKET, 0));
        assertEquals(30, CustomCrossbowRules.loadTicks(CustomCrossbowChargeType.ECHO_SHARD, 3));
        assertEquals(10, CustomCrossbowRules.loadTicks(CustomCrossbowChargeType.FIREWORK_ROCKET, 9));
    }

    @Test
    void specialShotsHaveEchoHarshDurabilityCosts() {
        assertEquals(5, CustomCrossbowRules.durabilityCost(CustomCrossbowChargeType.ECHO_SHARD));
        assertEquals(4, CustomCrossbowRules.durabilityCost(CustomCrossbowChargeType.ENDER_PEARL));
        assertEquals(2, CustomCrossbowRules.durabilityCost(CustomCrossbowChargeType.AMETHYST_SHARD));
        assertEquals(2, CustomCrossbowRules.durabilityCost(CustomCrossbowChargeType.FIREWORK_ROCKET));
        assertEquals(2, CustomCrossbowRules.prismarineArrowDurabilityCost());
    }

    @Test
    void echoShardHasDefaultShotCooldown() {
        assertEquals(100, CustomCrossbowRules.defaultEchoShardCooldownTicks());
    }
}
