package dev.linqfy.bigCasares.items.catalog;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class BukkitCatalogRecipeGateway implements CatalogRecipeGateway {

    private final Plugin plugin;
    private final CatalogItemStackFactory stackFactory;

    public BukkitCatalogRecipeGateway(Plugin plugin, CatalogItemStackFactory stackFactory) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.stackFactory = Objects.requireNonNull(stackFactory, "stackFactory");
    }

    @Override
    public void remove(String recipeKey) {
        Bukkit.removeRecipe(key(recipeKey));
    }

    @Override
    public boolean register(
        String recipeKey,
        CustomItemDefinition result,
        ItemRecipeDefinition recipe,
        String catalogRevision
    ) {
        ShapedRecipe shapedRecipe = new ShapedRecipe(
            key(recipeKey), stackFactory.create(result, catalogRevision, recipe.resultAmount())
        );
        shapedRecipe.shape(recipe.shape().toArray(String[]::new));
        for (var ingredient : recipe.ingredients().entrySet()) {
            Material material = Material.matchMaterial(ingredient.getValue());
            if (material == null) {
                throw new IllegalArgumentException("recipe material is not available: " + ingredient.getValue());
            }
            shapedRecipe.setIngredient(ingredient.getKey(), material);
        }
        return Bukkit.addRecipe(shapedRecipe);
    }

    private NamespacedKey key(String recipeKey) {
        return new NamespacedKey(plugin, recipeKey);
    }
}
