package dev.linqfy.bigCasares.modules.tombstone;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TombstoneItemLayoutTest {

    @Test
    void preservesStorageGapsThenArmorOffhandAndCursorSlots() {
        ItemStack[] storage = new ItemStack[36];
        storage[0] = new ItemStack(Material.STONE);
        storage[8] = new ItemStack(Material.DIRT);
        ItemStack[] armor = new ItemStack[4];
        armor[2] = new ItemStack(Material.IRON_CHESTPLATE);
        ItemStack offhand = new ItemStack(Material.SHIELD);
        ItemStack cursor = new ItemStack(Material.APPLE);

        List<ItemStack> items = TombstoneItemLayout.snapshot(storage, armor, offhand, cursor);

        assertEquals(42, items.size());
        assertEquals(Material.STONE, items.get(0).getType());
        assertNull(items.get(1));
        assertEquals(Material.DIRT, items.get(8).getType());
        assertEquals(Material.IRON_CHESTPLATE, items.get(38).getType());
        assertEquals(Material.SHIELD, items.get(40).getType());
        assertEquals(Material.APPLE, items.get(41).getType());
    }
}
