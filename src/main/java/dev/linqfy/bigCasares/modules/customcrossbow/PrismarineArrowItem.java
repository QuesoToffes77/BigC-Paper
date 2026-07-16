package dev.linqfy.bigCasares.modules.customcrossbow;

import dev.linqfy.bigCasares.items.CatalogBackedCustomItem;
import dev.linqfy.bigCasares.items.CustomItemRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class PrismarineArrowItem extends CatalogBackedCustomItem {

    public static final String ID = "prismarine_arrow";
    public static final int MODEL_DATA = 1003;

    public PrismarineArrowItem(CustomItemRegistry registry, NamespacedKey legacyItemKey) {
        super(registry, ID, MODEL_DATA, legacyItemKey);
    }

    @Override
    public void onConsume(Player player, ItemStack consumedItem) {
        // Ammunition is consumed by vanilla bow/crossbow handling.
    }
}
