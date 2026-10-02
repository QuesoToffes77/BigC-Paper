package dev.linqfy.bigCasares.modules.copperapple;

import dev.linqfy.bigCasares.items.CatalogBackedCustomItem;
import dev.linqfy.bigCasares.items.CustomItemRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class CopperAppleItem extends CatalogBackedCustomItem {

    public static final String ID = "copper_apple";
    public static final int MODEL_DATA = 1001;
    private final CopperAppleOxidationService oxidationService;

    public CopperAppleItem(
        CustomItemRegistry registry,
        NamespacedKey legacyItemKey,
        CopperAppleOxidationService oxidationService
    ) {
        super(registry, ID, MODEL_DATA, legacyItemKey);
        this.oxidationService = oxidationService;
    }

    @Override
    public ItemStack createItemStack(int amount) {
        ItemStack stack = super.createItemStack(amount);
        oxidationService.initialize(stack);
        return stack;
    }

    @Override
    public void onConsume(Player player, ItemStack consumedItem) {
        oxidationService.consume(player, consumedItem);
    }
}
