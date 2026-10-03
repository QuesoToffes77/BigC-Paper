package dev.linqfy.bigCasares.items.catalog;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogItemStackFactoryMaterialMigrationTest {

    @Test
    void migratesOnlyLegacyNitricAcidPotionToGlassBottle() {
        assertTrue(CatalogItemStackFactory.supportsMaterialMigration(
            "nitric_acid", Material.POTION, Material.GLASS_BOTTLE));
        assertFalse(CatalogItemStackFactory.supportsMaterialMigration(
            "nitric_acid", Material.SPLASH_POTION, Material.GLASS_BOTTLE));
        assertFalse(CatalogItemStackFactory.supportsMaterialMigration(
            "other_item", Material.POTION, Material.GLASS_BOTTLE));
        assertFalse(CatalogItemStackFactory.supportsMaterialMigration(
            "nitric_acid", Material.POTION, Material.HONEY_BOTTLE));
    }
}
