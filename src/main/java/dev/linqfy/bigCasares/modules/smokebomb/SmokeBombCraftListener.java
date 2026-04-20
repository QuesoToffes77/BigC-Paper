package dev.linqfy.bigCasares.modules.smokebomb;

import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;

public final class SmokeBombCraftListener implements Listener {

    private final NamespacedKey recipeKey;
    private final SmokeBombItem smokeBombItem;

    public SmokeBombCraftListener(NamespacedKey recipeKey, SmokeBombItem smokeBombItem) {
        this.recipeKey = recipeKey;
        this.smokeBombItem = smokeBombItem;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (!(recipe instanceof ShapedRecipe shapedRecipe) || !recipeKey.equals(shapedRecipe.getKey())) {
            return;
        }

        CraftingInventory inventory = event.getInventory();
        if (!SmokeBombRecipeMatcher.matches(inventory.getMatrix())) {
            inventory.setResult(null);
            return;
        }

        inventory.setResult(smokeBombItem.createItemStack(1));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        Recipe recipe = event.getRecipe();
        if (!(recipe instanceof ShapedRecipe shapedRecipe) || !recipeKey.equals(shapedRecipe.getKey())) {
            return;
        }
        if (!SmokeBombRecipeMatcher.matches(event.getInventory().getMatrix())) {
            event.setCancelled(true);
            return;
        }

        event.setCurrentItem(smokeBombItem.createItemStack(1));
    }
}
