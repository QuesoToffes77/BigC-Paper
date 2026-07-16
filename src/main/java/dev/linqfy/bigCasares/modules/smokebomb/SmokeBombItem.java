package dev.linqfy.bigCasares.modules.smokebomb;

import dev.linqfy.bigCasares.items.CatalogBackedCustomItem;
import dev.linqfy.bigCasares.items.CustomItemRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class SmokeBombItem extends CatalogBackedCustomItem {

    public static final String ID = "smoke_bomb";
    public static final int MODEL_DATA = 1002;

    public SmokeBombItem(CustomItemRegistry registry, NamespacedKey legacyItemKey) {
        super(registry, ID, MODEL_DATA, legacyItemKey);
    }

    @Override
    public void onConsume(Player player, ItemStack consumedItem) {
        // Smoke bombs are thrown, not consumed as food.
    }
}
