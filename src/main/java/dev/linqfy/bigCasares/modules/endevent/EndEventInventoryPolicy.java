package dev.linqfy.bigCasares.modules.endevent;

import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;

import java.util.ArrayList;
import java.util.List;

public final class EndEventInventoryPolicy {

    public int removeForbidden(Player player) {
        int removed = 0;
        ItemStack[] storage = player.getInventory().getStorageContents();
        for (int slot = 0; slot < storage.length; slot++) {
            Removal removal = clean(storage[slot]);
            storage[slot] = removal.item();
            removed += removal.removed();
        }
        player.getInventory().setStorageContents(storage);
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int slot = 0; slot < armor.length; slot++) {
            Removal removal = clean(armor[slot]);
            armor[slot] = removal.item();
            removed += removal.removed();
        }
        player.getInventory().setArmorContents(armor);
        Removal offhand = clean(player.getInventory().getItemInOffHand());
        player.getInventory().setItemInOffHand(offhand.item());
        removed += offhand.removed();
        Removal cursor = clean(player.getItemOnCursor());
        player.setItemOnCursor(cursor.item());
        return removed + cursor.removed();
    }

    private Removal clean(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return new Removal(item, 0);
        }
        if (EndEventPotionPolicy.isForbidden(item.getType())) {
            return new Removal(null, item.getAmount());
        }
        int removed = 0;
        if (item.getItemMeta() instanceof BundleMeta bundle) {
            List<ItemStack> cleaned = new ArrayList<>();
            for (ItemStack nested : bundle.getItems()) {
                Removal removal = clean(nested);
                removed += removal.removed();
                if (removal.item() != null) {
                    cleaned.add(removal.item());
                }
            }
            bundle.setItems(cleaned);
            item.setItemMeta(bundle);
        } else if (item.getItemMeta() instanceof BlockStateMeta meta
            && meta.getBlockState() instanceof Container container) {
            ItemStack[] contents = container.getInventory().getContents();
            for (int slot = 0; slot < contents.length; slot++) {
                Removal removal = clean(contents[slot]);
                contents[slot] = removal.item();
                removed += removal.removed();
            }
            container.getInventory().setContents(contents);
            meta.setBlockState(container);
            item.setItemMeta(meta);
        }
        return new Removal(item, removed);
    }

    private record Removal(ItemStack item, int removed) {
    }
}
