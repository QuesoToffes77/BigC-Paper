package dev.linqfy.bigCasares.modules.specialitems;

import dev.linqfy.bigCasares.items.CatalogBackedCustomItem;
import dev.linqfy.bigCasares.items.CustomItemRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class NukeShotItem extends CatalogBackedCustomItem {

    public static final String ID = "nuke_shot";
    public static final int MODEL_DATA = 1008;

    public NukeShotItem(CustomItemRegistry registry, NamespacedKey legacyItemKey) {
        super(registry, ID, MODEL_DATA, legacyItemKey);
    }

    @Override
    public void onConsume(Player player, ItemStack consumedItem) {
    }
}
