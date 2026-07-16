package dev.linqfy.bigCasares.items;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public interface CustomItem {

    String getId();

    int getCustomModelData();

    NamespacedKey getItemKey();

    ItemStack createItemStack(int amount);

    void onConsume(Player player, ItemStack consumedItem);

    default boolean matches(ItemStack item) {
        return matchesLegacy(item);
    }

    default boolean matchesLegacy(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        var meta = item.getItemMeta();
        var modelData = ModelDataUtil.readCustomModelData(meta);
        if (modelData.isEmpty() || modelData.getAsInt() != getCustomModelData()) {
            return false;
        }

        var pdc = meta.getPersistentDataContainer();
        if (!pdc.has(getItemKey(), PersistentDataType.BYTE)) {
            return false;
        }

        Byte marker = pdc.get(getItemKey(), PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }
}
