package dev.linqfy.bigCasares.items.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ItemCatalogLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsACompleteDefinitionIntoAnImmutableCatalog() throws Exception {
        Files.writeString(tempDir.resolve("copper_apple.yml"), """
            id: copper_apple
            mechanic: copper-apple
            material: APPLE
            item-model: bigcasares:copper_apple
            legacy-custom-model-data: 1001
            display:
              translation-key: item.bigcasares.copper_apple
              fallback-name: Manzana de Cobre
              lore: []
            max-stack-size: 64
            components:
              food:
                nutrition: 4
                saturation: 1.2
                can-always-eat: true
            recipe:
              key: copper_apple_recipe
              result-amount: 1
              shape: [CCC, CAC, CCC]
              ingredients:
                C: COPPER_INGOT
                A: APPLE
            appearance:
              java-item-definition: java/assets/bigcasares/items/copper_apple.json
              bedrock-texture: bedrock/textures/item/copper_apple.png
            """);

        CustomItemCatalog catalog = new ItemCatalogLoader().load(tempDir);
        CustomItemDefinition definition = catalog.require("copper_apple");

        assertEquals(1, catalog.size());
        assertEquals("APPLE", definition.material());
        assertEquals("bigcasares:copper_apple", definition.itemModel());
        assertEquals(1001, definition.legacyCustomModelData().orElseThrow());
        assertEquals(4, definition.foodDefinition().orElseThrow().nutrition());
        assertEquals("CCC", definition.recipeDefinition().orElseThrow().shape().getFirst());
    }

    @Test
    void rejectsDefinitionsMissingRequiredValues() throws Exception {
        Files.writeString(tempDir.resolve("broken.yml"), """
            id: smoke_bomb
            mechanic: smoke-bomb
            material: SNOWBALL
            """);

        assertThrows(IllegalArgumentException.class, () -> new ItemCatalogLoader().load(tempDir));
    }

    @Test
    void rejectsAnEmptyDefinitionDirectory() {
        assertThrows(IllegalArgumentException.class, () -> new ItemCatalogLoader().load(tempDir));
    }

    @Test
    void rejectsDuplicateIdsUnsafeIdsAndIncompleteRecipes() throws Exception {
        Files.writeString(tempDir.resolve("first.yml"), definition("copper_apple", "copper-apple", ""));
        Files.writeString(tempDir.resolve("second.yml"), definition("copper_apple", "copper-apple", ""));

        assertThrows(IllegalArgumentException.class, () -> new ItemCatalogLoader().load(tempDir));

        Files.delete(tempDir.resolve("second.yml"));
        Files.writeString(tempDir.resolve("unsafe.yml"), definition("unsafe id", "copper-apple", ""));
        assertThrows(IllegalArgumentException.class, () -> new ItemCatalogLoader().load(tempDir));

        Files.delete(tempDir.resolve("unsafe.yml"));
        Files.writeString(tempDir.resolve("broken_recipe.yml"), definition("copper_apple", "copper-apple", """
            recipe:
              key: copper_apple_recipe
              result-amount: 1
              shape: ["X"]
              ingredients:
                A: APPLE
            """));
        assertThrows(IllegalArgumentException.class, () -> new ItemCatalogLoader().load(tempDir));
    }

    private static String definition(String id, String mechanic, String extra) {
        return """
            id: %s
            mechanic: %s
            material: APPLE
            item-model: bigcasares:copper_apple
            display:
              translation-key: item.bigcasares.copper_apple
              fallback-name: Manzana de Cobre
              lore: []
            max-stack-size: 64
            appearance:
              java-item-definition: java/assets/bigcasares/items/copper_apple.json
              bedrock-texture: bedrock/textures/item/copper_apple.png
            %s
            """.formatted(id, mechanic, extra);
    }
}
