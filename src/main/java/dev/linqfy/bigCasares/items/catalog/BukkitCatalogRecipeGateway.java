package dev.linqfy.bigCasares.items.catalog;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
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
        ItemRecipeDefinition recipe,
        CustomItemCatalog catalog
    ) {
        Objects.requireNonNull(catalog, "catalog");
        String revision = catalog.revision();
        CustomItemDefinition result = ownerOf(recipeKey, catalog);
        ItemStack resultStack = resultStack(result, recipe, revision);
        if (recipe.type() == RecipeType.SHAPELESS) {
            ShapelessRecipe shapelessRecipe = new ShapelessRecipe(key(recipeKey), resultStack);
            for (String ingredient : recipe.shapelessIngredients()) {
                shapelessRecipe.addIngredient(choiceFor(ingredient, catalog, revision));
            }
            return Bukkit.addRecipe(shapelessRecipe);
        }
        ShapedRecipe shapedRecipe = new ShapedRecipe(key(recipeKey), resultStack);
        shapedRecipe.shape(recipe.shape().toArray(String[]::new));
        for (var ingredient : recipe.ingredients().entrySet()) {
            shapedRecipe.setIngredient(ingredient.getKey(), choiceFor(ingredient.getValue(), catalog, revision));
        }
        return Bukkit.addRecipe(shapedRecipe);
    }

    private ItemStack resultStack(CustomItemDefinition owner, ItemRecipeDefinition recipe, String revision) {
        if (recipe.resultMaterial() != null) {
            // The recipe produces a vanilla item (for example GUNPOWDER); the
            // owning definition only hosts the recipe.
            return new ItemStack(requireMaterial(recipe.resultMaterial()), recipe.resultAmount());
        }
        return stackFactory.create(owner, revision, recipe.resultAmount());
    }

    /**
     * Resolves an ingredient to a real Bukkit {@link RecipeChoice}.
     * Vanilla materials become a {@link RecipeChoice.MaterialChoice} (a
     * {@code COAL|CHARCOAL} choice stays substitutable only within those
     * materials). BigCasares catalog references become an
     * {@link RecipeChoice.ExactChoice} over the catalog item stack, so any
     * vanilla item with a similar name or look is rejected: identity is the
     * persistent-data-based catalog id.
     */
    private RecipeChoice choiceFor(String ingredient, CustomItemCatalog catalog, String revision) {
        RecipeChoiceSpec spec = specFor(ingredient, catalog);
        if (spec.exact()) {
            List<ItemStack> stacks = new ArrayList<>(spec.catalogItemIds().size());
            for (String itemId : spec.catalogItemIds()) {
                stacks.add(stackFactory.create(catalog.require(itemId), revision, 1));
            }
            return new RecipeChoice.ExactChoice(stacks);
        }
        List<Material> materials = new ArrayList<>(spec.materials().size());
        for (String material : spec.materials()) {
            materials.add(requireMaterial(material));
        }
        return new RecipeChoice.MaterialChoice(materials);
    }

    /**
     * Pure description of how one catalog ingredient string maps to a Bukkit
     * choice, kept outside {@link RecipeChoice} so the substitution contract
     * (exact custom item vs. vanilla material choice) is unit-testable.
     */
    static RecipeChoiceSpec specFor(String ingredient, CustomItemCatalog catalog) {
        String[] parts = ingredient.split("\\|");
        boolean custom = parts[0].contains(":");
        if (custom) {
            List<String> ids = new ArrayList<>(parts.length);
            for (String part : parts) {
                ids.add(catalog.require(customItemId(part)).id());
            }
            return new RecipeChoiceSpec(true, ids, List.of());
        }
        return new RecipeChoiceSpec(false, List.of(), List.of(parts));
    }

    record RecipeChoiceSpec(boolean exact, List<String> catalogItemIds, List<String> materials) {
    }

    private static CustomItemDefinition ownerOf(String recipeKey, CustomItemCatalog catalog) {
        return catalog.definitions().values().stream()
            .filter(definition -> definition.recipeDefinition()
                .map(recipe -> recipe.key().equals(recipeKey)).orElse(false))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "no catalog item owns recipe " + recipeKey));
    }

    private static String customItemId(String reference) {
        int colon = reference.indexOf(':');
        if (colon < 0) {
            throw new IllegalArgumentException("invalid custom item reference: " + reference);
        }
        return reference.substring(colon + 1);
    }

    private Material requireMaterial(String value) {
        Material material = Material.matchMaterial(value);
        if (material == null) {
            throw new IllegalArgumentException("recipe material is not available: " + value);
        }
        return material;
    }

    private NamespacedKey key(String recipeKey) {
        return new NamespacedKey(plugin, recipeKey);
    }
}
