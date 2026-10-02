package dev.linqfy.bigCasares.items.catalog;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class CatalogRecipeRuntime {

    private final CatalogRecipeGateway gateway;
    private Map<String, ActiveRecipe> activeRecipes = Map.of();

    public CatalogRecipeRuntime(CatalogRecipeGateway gateway) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    public synchronized void replace(CustomItemCatalog candidate) {
        Objects.requireNonNull(candidate, "candidate");
        Map<String, ActiveRecipe> desired = recipesOf(candidate);
        Map<String, ActiveRecipe> previous = activeRecipes;
        Set<String> allKeys = new LinkedHashSet<>(previous.keySet());
        allKeys.addAll(desired.keySet());
        allKeys.forEach(gateway::remove);
        try {
            for (ActiveRecipe recipe : desired.values()) {
                if (!gateway.register(recipe.key(), recipe.recipe(), recipe.catalog())) {
                    throw new IllegalStateException("could not register catalog recipe: " + recipe.key());
                }
            }
            activeRecipes = Map.copyOf(desired);
        } catch (Throwable failure) {
            desired.keySet().forEach(gateway::remove);
            try {
                for (ActiveRecipe recipe : previous.values()) {
                    if (!gateway.register(recipe.key(), recipe.recipe(), recipe.catalog())) {
                        throw new IllegalStateException("could not restore catalog recipe: " + recipe.key());
                    }
                }
                activeRecipes = previous;
            } catch (Throwable rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
            if (failure instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("could not replace catalog recipes", failure);
        }
    }

    public synchronized void clear() {
        activeRecipes.keySet().forEach(gateway::remove);
        activeRecipes = Map.of();
    }

    public synchronized int activeRecipeCount() {
        return activeRecipes.size();
    }

    private static Map<String, ActiveRecipe> recipesOf(CustomItemCatalog catalog) {
        Map<String, ActiveRecipe> recipes = new LinkedHashMap<>();
        for (CustomItemDefinition definition : catalog.definitions().values()) {
            definition.recipeDefinition().ifPresent(recipe -> {
                ActiveRecipe prior = recipes.put(recipe.key(), new ActiveRecipe(
                    recipe.key(), recipe, catalog
                ));
                if (prior != null) {
                    throw new IllegalArgumentException("duplicate catalog recipe key: " + recipe.key());
                }
            });
        }
        return recipes;
    }

    private record ActiveRecipe(
        String key,
        ItemRecipeDefinition recipe,
        CustomItemCatalog catalog
    ) {
    }
}
