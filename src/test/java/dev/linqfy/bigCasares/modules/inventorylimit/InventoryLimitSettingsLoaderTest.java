package dev.linqfy.bigCasares.modules.inventorylimit;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryLimitSettingsLoaderTest {

    @Test
    void loadsPositiveMaterialLimits() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("inventory-limit.limits.TOTEM_OF_UNDYING", 3);
        config.set("inventory-limit.limits.ENDER_PEARL", 8);

        Map<Material, Integer> limits = new InventoryLimitSettingsLoader().load(config);

        assertEquals(3, limits.get(Material.TOTEM_OF_UNDYING));
        assertEquals(8, limits.get(Material.ENDER_PEARL));
    }

    @Test
    void rejectsInvalidMaterial() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("inventory-limit.limits.NOT_A_REAL_MATERIAL", 3);

        IllegalArgumentException thrown = assertThrows(
            IllegalArgumentException.class,
            () -> new InventoryLimitSettingsLoader().load(config)
        );

        assertEquals("Material invalido: NOT_A_REAL_MATERIAL", thrown.getMessage());
    }

    @Test
    void rejectsNonPositiveLimit() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("inventory-limit.limits.TOTEM_OF_UNDYING", 0);

        IllegalArgumentException thrown = assertThrows(
            IllegalArgumentException.class,
            () -> new InventoryLimitSettingsLoader().load(config)
        );

        assertEquals("El limite debe ser mayor que cero para TOTEM_OF_UNDYING", thrown.getMessage());
    }
}
