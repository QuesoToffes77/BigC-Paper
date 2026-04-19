package com.copperapple.listeners;

import com.copperapple.CopperApplePlugin;
import com.copperapple.items.CopperAppleItem;
import com.copperapple.items.CustomItem;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;

import java.util.Optional;

public class CraftListener implements Listener {

    private final CopperApplePlugin plugin;

    public CraftListener(CopperApplePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {

        Recipe recipe = event.getRecipe();
        if (!(recipe instanceof ShapedRecipe shapedRecipe)) return;

        if (!shapedRecipe.getKey().equals(plugin.getRecipeKey())) return;

        CraftingInventory inv = event.getInventory();

        // Validación EXACTA de receta
        if (!isExactRecipe(inv.getMatrix())) {
            inv.setResult(null);
            return;
        }

        Optional<CustomItem> customItemOpt =
                plugin.getItemRegistry().findByModelData(CopperAppleItem.MODEL_DATA);

        if (customItemOpt.isEmpty()) {
            inv.setResult(null);
            return;
        }

        inv.setResult(customItemOpt.get().buildItemStack());
    }

    // Anti duplicación SHIFT-click
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {

        Recipe recipe = event.getRecipe();
        if (!(recipe instanceof ShapedRecipe shapedRecipe)) return;

        if (!shapedRecipe.getKey().equals(plugin.getRecipeKey())) return;

        if (!isExactRecipe(event.getInventory().getMatrix())) {
            event.setCancelled(true);
            return;
        }

        Optional<CustomItem> customItemOpt =
                plugin.getItemRegistry().findByModelData(CopperAppleItem.MODEL_DATA);

        customItemOpt.ifPresent(item -> {
            event.getCurrentItem().setItemMeta(item.buildItemStack().getItemMeta());
        });
    }

    private boolean isExactRecipe(ItemStack[] matrix) {
        if (matrix.length != 9) return false;

        for (int i = 0; i < 9; i++) {
            if (i == 4) {
                if (matrix[i] == null || matrix[i].getType() != Material.APPLE) return false;
            } else {
                if (matrix[i] == null || matrix[i].getType() != Material.COPPER_INGOT) return false;
            }
        }
        return true;
    }
}