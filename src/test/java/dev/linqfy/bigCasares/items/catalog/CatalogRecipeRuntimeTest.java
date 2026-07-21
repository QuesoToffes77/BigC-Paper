package dev.linqfy.bigCasares.items.catalog;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CatalogRecipeRuntimeTest {

    @Test
    void replacesOwnedRecipesWithTheCandidateCatalog() {
        FakeGateway gateway = new FakeGateway();
        CatalogRecipeRuntime runtime = new CatalogRecipeRuntime(gateway);

        runtime.replace(catalog("COPPER_INGOT"));
        runtime.replace(catalog("GOLD_INGOT"));

        assertEquals("GOLD_INGOT", gateway.recipes.get("copper_apple_recipe").ingredients().get('C'));
        assertEquals(1, runtime.activeRecipeCount());
    }

    @Test
    void restoresThePreviousRecipesWhenCandidateRegistrationFails() {
        FakeGateway gateway = new FakeGateway();
        CatalogRecipeRuntime runtime = new CatalogRecipeRuntime(gateway);
        runtime.replace(catalog("COPPER_INGOT"));
        gateway.failOnMaterial = "GOLD_INGOT";

        assertThrows(IllegalStateException.class, () -> runtime.replace(catalog("GOLD_INGOT")));

        assertEquals("COPPER_INGOT", gateway.recipes.get("copper_apple_recipe").ingredients().get('C'));
        assertEquals(1, runtime.activeRecipeCount());
    }

    private static CustomItemCatalog catalog(String ingredient) {
        ItemRecipeDefinition recipe = new ItemRecipeDefinition(
            "copper_apple_recipe", 1, List.of("CCC", "CAC", "CCC"),
            Map.of('C', ingredient, 'A', "APPLE")
        );
        CustomItemDefinition item = new CustomItemDefinition(
            "copper_apple", "copper-apple", "APPLE", "bigcasares:copper_apple", OptionalInt.of(1001),
            new ItemDisplayDefinition("item.bigcasares.copper_apple", "Manzana de Cobre", List.of()),
            64, null, null, recipe, null,
            new ItemAppearanceDefinition(
                "java/assets/bigcasares/items/copper_apple.json",
                "bedrock/textures/item/copper_apple.png"
            )
        );
        return new CustomItemCatalog(Map.of(item.id(), item));
    }

    private static final class FakeGateway implements CatalogRecipeGateway {

        private final Map<String, ItemRecipeDefinition> recipes = new LinkedHashMap<>();
        private String failOnMaterial;

        @Override
        public void remove(String recipeKey) {
            recipes.remove(recipeKey);
        }

        @Override
        public boolean register(
            String recipeKey,
            CustomItemDefinition result,
            ItemRecipeDefinition recipe,
            String catalogRevision
        ) {
            if (failOnMaterial != null && recipe.ingredients().containsValue(failOnMaterial)) {
                return false;
            }
            recipes.put(recipeKey, recipe);
            return true;
        }
    }
}
