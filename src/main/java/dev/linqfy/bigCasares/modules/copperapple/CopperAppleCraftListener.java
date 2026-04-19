package dev.linqfy.bigCasares.modules.copperapple;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;

public final class CopperAppleCraftListener implements Listener {

    private final NamespacedKey recipeKey;
    private final CopperAppleItem copperAppleItem;

    public CopperAppleCraftListener(NamespacedKey recipeKey, CopperAppleItem copperAppleItem) {
        this.recipeKey = recipeKey;
        this.copperAppleItem = copperAppleItem;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (!(recipe instanceof ShapedRecipe shapedRecipe)) {
            return;
        }
        if (!recipeKey.equals(shapedRecipe.getKey())) {
            return;
        }

        CraftingInventory inventory = event.getInventory();
        if (!isExactRecipe(inventory.getMatrix())) {
            inventory.setResult(null);
            return;
        }

        inventory.setResult(copperAppleItem.createItemStack(1));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        Recipe recipe = event.getRecipe();
        if (!(recipe instanceof ShapedRecipe shapedRecipe)) {
            return;
        }
        if (!recipeKey.equals(shapedRecipe.getKey())) {
            return;
        }
        if (!isExactRecipe(event.getInventory().getMatrix())) {
            event.setCancelled(true);
            return;
        }

        event.setCurrentItem(copperAppleItem.createItemStack(1));
    }

    private boolean isExactRecipe(ItemStack[] matrix) {
        if (matrix.length != 9) {
            return false;
        }

        for (int i = 0; i < matrix.length; i++) {
            ItemStack slot = matrix[i];
            if (slot == null) {
                return false;
            }

            if (i == 4) {
                if (slot.getType() != Material.APPLE) {
                    return false;
                }
            } else if (slot.getType() != Material.COPPER_INGOT) {
                return false;
            }
        }

        return true;
    }
}
