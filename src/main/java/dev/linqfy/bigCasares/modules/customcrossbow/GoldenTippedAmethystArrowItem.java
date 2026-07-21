package dev.linqfy.bigCasares.modules.customcrossbow;

import dev.linqfy.bigCasares.items.CatalogBackedCustomItem;
import dev.linqfy.bigCasares.items.CustomItemRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class GoldenTippedAmethystArrowItem extends CatalogBackedCustomItem {

    public static final String ID = "golden_tipped_amethyst_arrow";
    public static final int MODEL_DATA = 1006;

    public GoldenTippedAmethystArrowItem(CustomItemRegistry registry, NamespacedKey legacyItemKey) {
        super(registry, ID, MODEL_DATA, legacyItemKey);
    }

    @Override
    public void onConsume(Player player, ItemStack consumedItem) {
        // Vanilla crossbow loading owns ammunition consumption.
    }
}
