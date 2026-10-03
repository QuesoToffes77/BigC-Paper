package dev.linqfy.bigCasares.items.catalog;

import dev.linqfy.bigCasares.items.CustomItemRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GunpowderCraftingTest {

    @TempDir
    Path tempDir;

    @Test
    void gunpowderRecipeUsesTheRealCustomNitrateIngredientAndProducesVanillaGunpowder() throws Exception {
        CustomItemCatalog catalog = catalogWith();
        ItemRecipeDefinition recipe = catalog.require("potassium_nitrate").recipeDefinition().orElseThrow();

        assertEquals(RecipeType.SHAPELESS, recipe.type());
        assertEquals("GUNPOWDER", recipe.resultMaterial());
        assertEquals(1, recipe.resultAmount());
        assertEquals(List.of("bigcasares:potassium_nitrate", "SUGAR", "COAL|CHARCOAL"),
            recipe.shapelessIngredients());
    }

    @Test
    void customIngredientIsResolvedAsAnExactChoice() throws Exception {
        // The gateway maps bigcasares:potassium_nitrate to an ExactChoice over
        // the catalog stack: substitution by a vanilla item with a similar name
        // or look is impossible because identity is the catalog PDC id.
        BukkitCatalogRecipeGateway.RecipeChoiceSpec spec =
            BukkitCatalogRecipeGateway.specFor("bigcasares:potassium_nitrate", catalogWith());

        assertTrue(spec.exact());
        assertEquals(List.of("potassium_nitrate"), spec.catalogItemIds());
        assertTrue(spec.materials().isEmpty());
    }

    @Test
    void coalChoiceIsResolvedAsAMaterialChoice() throws Exception {
        BukkitCatalogRecipeGateway.RecipeChoiceSpec spec =
            BukkitCatalogRecipeGateway.specFor("COAL|CHARCOAL", catalogWith());

        assertFalse(spec.exact());
        assertEquals(List.of("COAL", "CHARCOAL"), spec.materials());
        assertTrue(spec.catalogItemIds().isEmpty());
    }

    @Test
    void unknownCustomIngredientReferenceIsRejectedAtResolutionTime() throws Exception {
        assertThrows(IllegalArgumentException.class,
            () -> BukkitCatalogRecipeGateway.specFor("bigcasares:does_not_exist", catalogWith()));
    }

    @Test
    void catalogValidatorRejectsUnknownCustomItemReferences() throws Exception {
        Files.writeString(tempDir.resolve("broken.yml"), """
            id: potassium_nitrate
            mechanic: catalog-material
            material: SUGAR
            item-model: bigcasares:potassium_nitrate
            display:
              translation-key: item.bigcasares.potassium_nitrate
              fallback-name: Nitrato de Potasio
              lore: []
            max-stack-size: 64
            recipe:
              type: shapeless
              key: broken_recipe
              result-amount: 1
              ingredients: [bigcasares:does_not_exist, SUGAR]
            appearance:
              java-item-definition: java/assets/bigcasares/items/potassium_nitrate.json
              bedrock-texture: bedrock/textures/item/potassium_nitrate.png
            """);

        CustomItemCatalog broken = new ItemCatalogLoader().load(tempDir);

        assertThrows(IllegalArgumentException.class,
            () -> new ItemCatalogValidator().validate(broken, Path.of("resourcepack")));
    }

    @Test
    void registryResolvesCatalogOnlyItemsWithoutConfusingThemWithVanilla() throws Exception {
        CustomItemRegistry registry = new CustomItemRegistry();
        registry.installCatalog(catalogWith());

        assertTrue(registry.findById("potassium_nitrate").isPresent());
        assertEquals("potassium_nitrate", registry.findById("potassium_nitrate").orElseThrow().getId());
        assertEquals("catalog-material", catalogWith().require("potassium_nitrate").mechanic());
        assertTrue(registry.findById("nitric_acid").isPresent());
        assertTrue(registry.findById("sugar").isEmpty(), "vanilla ids are not custom items");
        assertTrue(registry.getAllItems().stream().anyMatch(item -> item.getId().equals("potassium_nitrate")));
    }

    @Test
    void nitricAcidIsRegisteredAsItsOwnCustomItem() throws Exception {
        CustomItemCatalog catalog = catalogWith();

        CustomItemDefinition nitricAcid = catalog.require("nitric_acid");

        assertEquals("nitric_acid", nitricAcid.id());
        assertEquals("catalog-material", nitricAcid.mechanic());
        assertEquals("GLASS_BOTTLE", nitricAcid.material());
        assertEquals(64, nitricAcid.maxStackSize());
        assertTrue(nitricAcid.recipeDefinition().isEmpty());
        assertFalse(catalog.find("potassium_nitrate").orElseThrow().id().equals(nitricAcid.id()));
    }

    private CustomItemCatalog catalogWith() throws Exception {
        Files.writeString(tempDir.resolve("potassium_nitrate.yml"), """
            id: potassium_nitrate
            mechanic: catalog-material
            material: SUGAR
            item-model: bigcasares:potassium_nitrate
            display:
              translation-key: item.bigcasares.potassium_nitrate
              fallback-name: Nitrato de Potasio
              lore: []
            max-stack-size: 64
            recipe:
              type: shapeless
              key: potassium_nitrate_gunpowder_recipe
              result-amount: 1
              result-material: GUNPOWDER
              ingredients:
                - bigcasares:potassium_nitrate
                - SUGAR
                - COAL|CHARCOAL
            appearance:
              java-item-definition: java/assets/bigcasares/items/potassium_nitrate.json
              bedrock-texture: bedrock/textures/item/potassium_nitrate.png
            """);
        Files.writeString(tempDir.resolve("nitric_acid.yml"), """
            id: nitric_acid
            mechanic: catalog-material
            material: GLASS_BOTTLE
            item-model: bigcasares:nitric_acid
            display:
              translation-key: item.bigcasares.nitric_acid
              fallback-name: Acido Nitrico
              lore: []
            max-stack-size: 64
            appearance:
              java-item-definition: java/assets/bigcasares/items/nitric_acid.json
              bedrock-texture: bedrock/textures/item/nitric_acid.png
            """);
        return new ItemCatalogLoader().load(tempDir);
    }
}
