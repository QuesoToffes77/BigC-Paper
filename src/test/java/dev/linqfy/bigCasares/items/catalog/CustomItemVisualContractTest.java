package dev.linqfy.bigCasares.items.catalog;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomItemVisualContractTest {

    @Test
    void nitricAcidHasAStableStackableCatalogDefinitionAndResolvableModelChain() throws IOException {
        Path definition = Path.of("src", "main", "resources", "content", "items", "nitric_acid.yml");
        Path itemDefinition = Path.of("resourcepack", "java", "assets", "bigcasares", "items", "nitric_acid.json");
        Path model = Path.of("resourcepack", "java", "assets", "bigcasares", "models", "item", "nitric_acid.json");
        Path texture = Path.of("resourcepack", "java", "assets", "bigcasares", "textures", "item", "nitric_acid.png");

        assertTrue(Files.readString(definition).contains("material: GLASS_BOTTLE"));
        assertTrue(Files.readString(definition).contains("max-stack-size: 64"));
        assertTrue(Files.readString(itemDefinition).contains("bigcasares:item/nitric_acid"));
        assertTrue(Files.readString(model).contains("bigcasares:item/nitric_acid"));
        assertTrue(Files.isRegularFile(texture), "Nitric Acid texture must exist in the namespace");
    }
}
