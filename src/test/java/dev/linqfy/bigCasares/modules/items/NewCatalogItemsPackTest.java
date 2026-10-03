package dev.linqfy.bigCasares.modules.items;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NewCatalogItemsPackTest {

    private static final Path PACK = Path.of("resourcepack");

    @Test
    void newCatalogItemsAreWiredIntoTheJavaAndBedrockPacks() throws Exception {
        assertContains(PACK.resolve("shared/registry.yml"),
            "potassium-nitrate:", "java-model: bigcasares:item/potassium_nitrate",
            "nitric-acid:", "java-model: bigcasares:item/nitric_acid");

        assertContains(PACK.resolve("java/assets/bigcasares/items/potassium_nitrate.json"),
            "bigcasares:item/potassium_nitrate");
        assertContains(PACK.resolve("java/assets/bigcasares/items/nitric_acid.json"),
            "bigcasares:item/nitric_acid");
        assertContains(PACK.resolve("java/assets/bigcasares/models/item/potassium_nitrate.json"),
            "bigcasares:item/potassium_nitrate");
        assertContains(PACK.resolve("java/assets/bigcasares/models/item/nitric_acid.json"),
            "bigcasares:item/nitric_acid");

        assertTrue(Files.exists(PACK.resolve("java/assets/bigcasares/textures/item/potassium_nitrate.png")));
        assertTrue(Files.exists(PACK.resolve("java/assets/bigcasares/textures/item/nitric_acid.png")));
        assertTrue(Files.exists(PACK.resolve("shared/textures/item/potassium_nitrate.png")));
        assertTrue(Files.exists(PACK.resolve("shared/textures/item/nitric_acid.png")));
        assertTrue(Files.exists(PACK.resolve("bedrock/textures/item/potassium_nitrate.png")));
        assertTrue(Files.exists(PACK.resolve("bedrock/textures/item/nitric_acid.png")));

        assertContains(PACK.resolve("bedrock/textures/item_texture.json"),
            "bigcasares.potassium_nitrate", "bigcasares.nitric_acid");
        assertContains(PACK.resolve("java/assets/bigcasares/lang/en_us.json"),
            "item.bigcasares.potassium_nitrate", "item.bigcasares.nitric_acid");
        assertContains(PACK.resolve("java/assets/bigcasares/lang/es_es.json"),
            "item.bigcasares.potassium_nitrate", "item.bigcasares.nitric_acid");
    }

    private static void assertContains(Path path, String... values) throws Exception {
        String content = Files.readString(path);
        for (String value : values) {
            assertTrue(content.contains(value), path + " is missing " + value);
        }
    }
}
