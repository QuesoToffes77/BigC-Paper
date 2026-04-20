package dev.linqfy.bigCasares.modules.inventorylimit;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryLimitServiceTest {

    @Test
    void computesExactOverflow() {
        InventoryLimitService service = new InventoryLimitService(Map.of(Material.TOTEM_OF_UNDYING, 3));
        ItemStack[] contents = {
            new ItemStack(Material.TOTEM_OF_UNDYING, 2),
            new ItemStack(Material.TOTEM_OF_UNDYING, 3)
        };

        assertEquals(2, service.overflow(contents, Material.TOTEM_OF_UNDYING));
    }

    @Test
    void trimsExactOverflow() {
        InventoryLimitService service = new InventoryLimitService(Map.of(Material.TOTEM_OF_UNDYING, 3));
        ItemStack[] contents = {
            new ItemStack(Material.TOTEM_OF_UNDYING, 2),
            new ItemStack(Material.TOTEM_OF_UNDYING, 3),
            new ItemStack(Material.DIRT, 64)
        };

        int removed = service.trimOverflow(contents, Material.TOTEM_OF_UNDYING);

        assertEquals(2, removed);
        assertEquals(3, service.count(contents, Material.TOTEM_OF_UNDYING));
        assertEquals(Material.DIRT, contents[2].getType());
    }

    @Test
    void canAcceptOnlyWithinRemainingHeadroom() {
        InventoryLimitService service = new InventoryLimitService(Map.of(Material.TOTEM_OF_UNDYING, 3));
        ItemStack[] contents = {
            new ItemStack(Material.TOTEM_OF_UNDYING, 2)
        };

        assertTrue(service.canAccept(contents, Material.TOTEM_OF_UNDYING, 1));
        assertFalse(service.canAccept(contents, Material.TOTEM_OF_UNDYING, 2));
    }
}
