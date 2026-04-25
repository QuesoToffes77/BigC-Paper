package dev.linqfy.bigCasares.modules.customcrossbow;

import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ShapedRecipe;

public final class PrismarineArrowCraftListener implements Listener {

    private final NamespacedKey recipeKey;
    private final PrismarineArrowItem prismarineArrowItem;

    public PrismarineArrowCraftListener(NamespacedKey recipeKey, PrismarineArrowItem prismarineArrowItem) {
        this.recipeKey = recipeKey;
        this.prismarineArrowItem = prismarineArrowItem;
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getRecipe() instanceof ShapedRecipe recipe) || !recipe.getKey().equals(recipeKey)) {
            return;
        }
        event.getInventory().setResult(prismarineArrowItem.createItemStack(4));
    }
}
