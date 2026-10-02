package dev.linqfy.bigCasares.modules.grapplinghook;

import dev.linqfy.bigCasares.items.catalog.CustomItemCatalog;
import dev.linqfy.bigCasares.items.catalog.CustomItemDefinition;
import dev.linqfy.bigCasares.items.catalog.ItemCatalogLoader;
import dev.linqfy.bigCasares.items.catalog.ItemCatalogValidator;
import dev.linqfy.bigCasares.items.catalog.ItemRecipeDefinition;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrapplingHookCatalogTest {

    private static CustomItemCatalog catalog() {
        return new ItemCatalogLoader().load(Path.of("src/main/resources/content/items"));
    }

    @Test
    void fullCatalogPassesTheProductionValidator() {
        // Mirrors ItemCatalogModule.loadCandidate: the whole catalog (including
        // every hook tier) must pass mechanic/model/material/recipe/appearance
        // validation against the authored resource pack. Guards against, for
        // example, duplicate item-models silently breaking server startup.
        CustomItemCatalog catalog = catalog();
        new ItemCatalogValidator().validate(catalog, Path.of("resourcepack"));
    }

    @Test
    void sixGrapplingHooksAreDeclaredWithTheGrapplingMechanic() {
        CustomItemCatalog catalog = catalog();
        for (GrapplingHookTier tier : GrapplingHookTier.values()) {
            CustomItemDefinition definition = catalog.require(tier.catalogId());
            assertEquals("grappling-hook", definition.mechanic());
            assertEquals(tier.modelData(), definition.legacyCustomModelData().getAsInt());
            assertNotNull(definition.recipeDefinition(), tier.catalogId() + " must have a recipe");
        }
    }

    @Test
    void recipesAreProgressiveAndResolveEveryIngredient() {
        CustomItemCatalog catalog = catalog();
        for (GrapplingHookTier tier : GrapplingHookTier.values()) {
            CustomItemDefinition definition = catalog.require(tier.catalogId());
            ItemRecipeDefinition recipe = definition.recipeDefinition().orElseThrow();
            assertEquals(tier.catalogId() + "_recipe", recipe.key());
            assertEquals(1, recipe.resultAmount());
            for (Map.Entry<Character, String> ingredient : recipe.ingredients().entrySet()) {
                assertIngredientResolves(catalog, ingredient.getValue(), tier.catalogId());
            }
            for (String ingredient : recipe.shapelessIngredients()) {
                assertIngredientResolves(catalog, ingredient, tier.catalogId());
            }
        }
    }

    @Test
    void eachUpgradeConsumesThePreviousTier() {
        CustomItemCatalog catalog = catalog();
        GrapplingHookTier[] tiers = GrapplingHookTier.values();
        for (int index = 1; index < tiers.length; index++) {
            GrapplingHookTier previous = tiers[index - 1];
            CustomItemDefinition definition = catalog.require(tiers[index].catalogId());
            ItemRecipeDefinition recipe = definition.recipeDefinition().orElseThrow();
            assertTrue(recipe.ingredients().containsValue("bigcasares:" + previous.catalogId()),
                tiers[index] + " recipe must consume " + previous.catalogId());
        }
    }

    @Test
    void eachUpgradePlacesThePreviousTierAtTheBottomRowEndOfTheGrid() {
        // The authoritative grids put the consumed hook at the end of the
        // bottom row of the 3x3 crafting table, so the upgrade can never be
        // crafted without it and the progression I -> II -> ... -> VI is
        // enforced position-exactly (shaped recipes do not mirror).
        CustomItemCatalog catalog = catalog();
        GrapplingHookTier[] tiers = GrapplingHookTier.values();
        for (int index = 1; index < tiers.length; index++) {
            GrapplingHookTier previous = tiers[index - 1];
            ItemRecipeDefinition recipe = catalog.require(tiers[index].catalogId())
                .recipeDefinition().orElseThrow();
            assertEquals(3, recipe.shape().size(), tiers[index] + " must be a full 3x3 recipe");
            String bottomRow = recipe.shape().get(2);
            assertEquals(3, bottomRow.length(), tiers[index] + " bottom row must be three columns");
            char corner = bottomRow.charAt(2);
            assertEquals("bigcasares:" + previous.catalogId(), recipe.ingredients().get(corner),
                tiers[index] + " must consume " + previous.catalogId() + " at the bottom corner");
        }
    }

    @Test
    void recipesMatchTheAuthoritativeProgressionGrids() {
        CustomItemCatalog catalog = catalog();

        assertRecipe(catalog, "grappling_hook_1",
            java.util.List.of("ILI", "IFI", " I "),
            java.util.Map.of('I', "IRON_INGOT", 'L', "LEAD", 'F', "FISHING_ROD"));
        assertRecipe(catalog, "grappling_hook_2",
            java.util.List.of("DED", "EHE", "DCG"),
            java.util.Map.of('D', "DIAMOND", 'E', "EMERALD", 'H', "IRON_BLOCK",
                'C', "IRON_CHAIN", 'G', "bigcasares:grappling_hook_1"));
        assertRecipe(catalog, "grappling_hook_3",
            java.util.List.of("EDE", "DGD", "ECH"),
            java.util.Map.of('E', "EMERALD", 'D', "DIAMOND", 'G', "GOLD_BLOCK",
                'C', "IRON_CHAIN", 'H', "bigcasares:grappling_hook_2"));
        assertRecipe(catalog, "grappling_hook_4",
            java.util.List.of("EPE", "GEG", "DCH"),
            java.util.Map.of('E', "EMERALD", 'P', "DEEPSLATE_DIAMOND_ORE", 'G', "GOLD_INGOT",
                'D', "DIAMOND", 'C', "IRON_CHAIN", 'H', "bigcasares:grappling_hook_3"));
        assertRecipe(catalog, "grappling_hook_5",
            java.util.List.of("PEP", "EGE", "PCH"),
            java.util.Map.of('P', "DEEPSLATE_DIAMOND_ORE", 'E', "EMERALD", 'G', "GOLD_BLOCK",
                'C', "IRON_CHAIN", 'H', "bigcasares:grappling_hook_4"));
        assertRecipe(catalog, "grappling_hook_6",
            java.util.List.of("EPE", "PEP", "EDH"),
            java.util.Map.of('E', "EMERALD", 'P', "DEEPSLATE_DIAMOND_ORE",
                'D', "DIAMOND", 'H', "bigcasares:grappling_hook_5"));
    }

    @Test
    void tierTwoThroughSixUseExactlyTheSpecifiedMaterials() {
        CustomItemCatalog catalog = catalog();

        assertMaterialCounts(catalog, "grappling_hook_2",
            java.util.Map.of("DIAMOND", 3, "EMERALD", 3, "IRON_BLOCK", 1, "IRON_CHAIN", 1,
                "bigcasares:grappling_hook_1", 1));
        // The grid places 3 emeralds (top-left, top-right, bottom-left) and
        // 3 diamonds (top-center, middle-left, middle-right); the grid is
        // authoritative over the prose material list.
        assertMaterialCounts(catalog, "grappling_hook_3",
            java.util.Map.of("EMERALD", 3, "DIAMOND", 3, "GOLD_BLOCK", 1, "IRON_CHAIN", 1,
                "bigcasares:grappling_hook_2", 1));
        assertMaterialCounts(catalog, "grappling_hook_4",
            java.util.Map.of("EMERALD", 3, "DEEPSLATE_DIAMOND_ORE", 1, "GOLD_INGOT", 2,
                "DIAMOND", 1, "IRON_CHAIN", 1, "bigcasares:grappling_hook_3", 1));
        assertMaterialCounts(catalog, "grappling_hook_5",
            java.util.Map.of("DEEPSLATE_DIAMOND_ORE", 3, "EMERALD", 3, "GOLD_BLOCK", 1,
                "IRON_CHAIN", 1, "bigcasares:grappling_hook_4", 1));
        assertMaterialCounts(catalog, "grappling_hook_6",
            java.util.Map.of("EMERALD", 4, "DEEPSLATE_DIAMOND_ORE", 3, "DIAMOND", 1,
                "bigcasares:grappling_hook_5", 1));
    }

    private static void assertRecipe(
        CustomItemCatalog catalog,
        String itemId,
        java.util.List<String> shape,
        java.util.Map<Character, String> ingredients
    ) {
        ItemRecipeDefinition recipe = catalog.require(itemId).recipeDefinition().orElseThrow();
        assertEquals(shape, recipe.shape(), itemId + " shape");
        assertEquals(ingredients, recipe.ingredients(), itemId + " ingredients");
    }

    private static void assertMaterialCounts(
        CustomItemCatalog catalog,
        String itemId,
        java.util.Map<String, Integer> expected
    ) {
        ItemRecipeDefinition recipe = catalog.require(itemId).recipeDefinition().orElseThrow();
        java.util.Map<String, Integer> actual = new java.util.HashMap<>();
        for (String row : recipe.shape()) {
            for (int index = 0; index < row.length(); index++) {
                char key = row.charAt(index);
                if (key == ' ') {
                    continue;
                }
                String material = recipe.ingredients().get(key);
                actual.merge(material, 1, Integer::sum);
            }
        }
        assertEquals(expected, actual, itemId + " material counts");
    }

    private static void assertIngredientResolves(CustomItemCatalog catalog, String ingredient, String ownerId) {
        if (!ingredient.contains(":")) {
            return; // vanilla material
        }
        String referenced = ingredient.substring(ingredient.indexOf(':') + 1);
        assertTrue(catalog.find(referenced).isPresent(),
            ownerId + " recipe references unknown catalog item " + ingredient);
    }
}
