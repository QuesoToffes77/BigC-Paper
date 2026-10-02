package dev.linqfy.bigCasares.modules.glider;

import dev.linqfy.bigCasares.items.catalog.CustomItemCatalog;
import dev.linqfy.bigCasares.items.catalog.ItemCatalogLoader;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GliderRecipeProgressionTest {

    @Test
    void everyUpgradeRequiresExactlyThePreviousTier() throws Exception {
        CustomItemCatalog catalog = catalog();
        for (int number = 2; number <= 6; number++) {
            GliderTier target = GliderTier.fromNumber(number).orElseThrow();
            GliderTier previous = GliderTier.fromNumber(number - 1).orElseThrow();
            var recipe = catalog.require(target.catalogId()).recipeDefinition().orElseThrow();
            long references = recipe.ingredients().values().stream()
                .filter(("bigcasares:" + previous.catalogId())::equals)
                .count();
            assertEquals(1L, references, "The previous tier is consumed exactly once");
            assertEquals(1, recipe.resultAmount());
        }
    }

    @Test
    void fakeItemWithSameMaterialIsRejected() {
        assertFalse(GliderRecipePolicy.acceptsUpgradeIngredient(GliderTier.III, null));
        assertFalse(GliderRecipePolicy.acceptsUpgradeIngredient(GliderTier.III, "phantom_membrane"));
        assertFalse(GliderRecipePolicy.acceptsUpgradeIngredient(GliderTier.III, "glider_tier_1"));
        assertTrue(GliderRecipePolicy.acceptsUpgradeIngredient(GliderTier.III, "glider_tier_2"));
    }

    @Test
    void tierOneHasNoCustomPredecessor() {
        assertTrue(GliderRecipePolicy.previousTier(GliderTier.I).isEmpty());
    }

    @Test
    void recipesMatchTheDocumentedSixTierProgression() throws Exception {
        CustomItemCatalog catalog = catalog();
        assertRecipe(catalog, GliderTier.I, List.of("PFP", "SIS", "FSF"),
            Map.of('P', "PHANTOM_MEMBRANE", 'F', "FEATHER", 'S', "STRING", 'I', "IRON_INGOT"));
        assertRecipe(catalog, GliderTier.II, List.of("PCP", "CGC", "PCP"),
            Map.of('P', "PHANTOM_MEMBRANE", 'C', "COPPER_INGOT", 'G', "bigcasares:glider_tier_1"));
        assertRecipe(catalog, GliderTier.III, List.of("PDP", "DGD", "PDP"),
            Map.of('P', "PHANTOM_MEMBRANE", 'D', "DIAMOND", 'G', "bigcasares:glider_tier_2"));
        assertRecipe(catalog, GliderTier.IV, List.of("ENE", "NGN", "ENE"),
            Map.of('E', "ECHO_SHARD", 'N', "NETHERITE_SCRAP", 'G', "bigcasares:glider_tier_3"));
        assertRecipe(catalog, GliderTier.V, List.of("ENE", "YGY", "ENE"),
            Map.of('E', "ECHO_SHARD", 'N', "NETHERITE_INGOT", 'Y', "ENDER_EYE",
                'G', "bigcasares:glider_tier_4"));
        assertRecipe(catalog, GliderTier.VI, List.of("NSN", "EGE", "NSN"),
            Map.of('N', "NETHERITE_INGOT", 'S', "NETHER_STAR", 'E', "ECHO_SHARD",
                'G', "bigcasares:glider_tier_5"));
    }

    private static void assertRecipe(
        CustomItemCatalog catalog,
        GliderTier tier,
        List<String> shape,
        Map<Character, String> ingredients
    ) {
        var recipe = catalog.require(tier.catalogId()).recipeDefinition().orElseThrow();
        assertEquals(shape, recipe.shape());
        assertEquals(ingredients, recipe.ingredients());
    }

    private static CustomItemCatalog catalog() throws Exception {
        return new ItemCatalogLoader().load(Path.of("src", "main", "resources", "content", "items"));
    }
}
