package dev.linqfy.bigCasares.modules.tombstone;

import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class TombstoneItemLayout {
    static final int STORAGE_SLOTS = 36;
    static final int ARMOR_START = 36;
    static final int OFFHAND_SLOT = 40;
    static final int CURSOR_SLOT = 41;
    static final int SNAPSHOT_SIZE = 42;

    private TombstoneItemLayout() {
    }

    static List<ItemStack> snapshot(
        ItemStack[] storage,
        ItemStack[] armor,
        ItemStack offhand,
        ItemStack cursor
    ) {
        List<ItemStack> items = new ArrayList<>(Collections.nCopies(SNAPSHOT_SIZE, null));
        for (int slot = 0; slot < Math.min(STORAGE_SLOTS, storage.length); slot++) {
            items.set(slot, cloneOrNull(storage[slot]));
        }
        for (int slot = 0; slot < Math.min(4, armor.length); slot++) {
            items.set(ARMOR_START + slot, cloneOrNull(armor[slot]));
        }
        items.set(OFFHAND_SLOT, cloneOrNull(offhand));
        items.set(CURSOR_SLOT, cloneOrNull(cursor));
        return Collections.unmodifiableList(items);
    }

    static boolean hasItems(List<ItemStack> items) {
        return items.stream().anyMatch(TombstoneItemLayout::isPresent);
    }

    static ItemStack cloneOrNull(ItemStack item) {
        return isPresent(item) ? item.clone() : null;
    }

    private static boolean isPresent(ItemStack item) {
        return item != null && item.getType() != Material.AIR && item.getAmount() > 0;
    }
}
