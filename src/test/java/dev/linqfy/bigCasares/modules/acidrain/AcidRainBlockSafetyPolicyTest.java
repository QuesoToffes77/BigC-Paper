package dev.linqfy.bigCasares.modules.acidrain;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcidRainBlockSafetyPolicyTest {

    @Test
    void defaultWhitelistAllowsExplicitStoneLogsAndPlanks() {
        AcidRainBlockSafetyPolicy policy = AcidRainBlockSafetyPolicy.fromSettings(AcidRainSettings.safeDefaults().environment());

        assertTrue(policy.canErode(Material.STONE));
        assertTrue(policy.canErode(Material.COBBLESTONE));
        assertTrue(policy.canErode(Material.DEEPSLATE));
        assertTrue(policy.canErode(Material.OAK_LOG));
        assertTrue(policy.canErode(Material.OAK_PLANKS));
    }

    @Test
    void leavesAreNeverErodedEvenWhenWhitelisted() {
        AcidRainEnvironmentSettings environment = AcidRainSettings.safeDefaults().environment()
            .withDestruction(AcidRainSettings.safeDefaults().environment().destruction()
                .withWhitelist(Set.of(Material.OAK_LEAVES, Material.STONE)));
        AcidRainBlockSafetyPolicy policy = AcidRainBlockSafetyPolicy.fromSettings(environment);

        assertFalse(policy.canErode(Material.OAK_LEAVES));
        assertFalse(policy.canErode(Material.SPRUCE_LEAVES));
        assertFalse(policy.canErode(Material.BIRCH_LEAVES));
        assertTrue(policy.canErode(Material.STONE));
    }

    @Test
    void oresContainersCropsDirtAndSpecialBlocksStayProtectedByDefault() {
        AcidRainBlockSafetyPolicy policy = AcidRainBlockSafetyPolicy.fromSettings(AcidRainSettings.safeDefaults().environment());

        assertFalse(policy.canErode(Material.DIAMOND_ORE));
        assertFalse(policy.canErode(Material.DEEPSLATE_DIAMOND_ORE));
        assertFalse(policy.canErode(Material.CHEST));
        assertFalse(policy.canErode(Material.BARREL));
        assertFalse(policy.canErode(Material.SHULKER_BOX));
        assertFalse(policy.canErode(Material.DIRT));
        assertFalse(policy.canErode(Material.GRASS_BLOCK));
        assertFalse(policy.canErode(Material.WHEAT));
        assertFalse(policy.canErode(Material.CARROTS));
        assertFalse(policy.canErode(Material.POTATOES));
        assertFalse(policy.canErode(Material.BEDROCK));
        assertFalse(policy.canErode(Material.SPAWNER));
        assertFalse(policy.canErode(Material.NETHER_PORTAL));
    }

    @Test
    void emptyWhitelistDoesNotErodeAnything() {
        AcidRainEnvironmentSettings environment = AcidRainSettings.safeDefaults().environment()
            .withDestruction(AcidRainSettings.safeDefaults().environment().destruction()
                .withWhitelist(Set.of()));
        AcidRainBlockSafetyPolicy policy = AcidRainBlockSafetyPolicy.fromSettings(environment);

        assertFalse(policy.canErode(Material.STONE));
        assertFalse(policy.canErode(Material.OAK_LOG));
    }
}
