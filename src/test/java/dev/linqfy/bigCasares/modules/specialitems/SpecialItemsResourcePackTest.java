package dev.linqfy.bigCasares.modules.specialitems;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecialItemsResourcePackTest {

    private static final Path PACK = Path.of("resourcepack");

    @Test
    void reusesVanillaMinecraftModelsAndTextures() throws Exception {
        assertContains(Path.of("src/main/resources/content/items/tracker_compass.yml"),
            "item-model: minecraft:compass");
        assertContains(Path.of("src/main/resources/content/items/nuke_shot.yml"),
            "item-model: minecraft:fishing_rod");
        assertContains(PACK.resolve("java/assets/bigcasares/models/item/nuke_shot.json"),
            "minecraft:block/tnt");
        assertContains(PACK.resolve("java/assets/bigcasares/models/item/tracker_compass.json"),
            "minecraft:item/compass");
        assertContains(PACK.resolve("assets/bigcasares/models/item/nuke_shot.json"),
            "minecraft:block/tnt");
        assertContains(PACK.resolve("assets/bigcasares/models/item/tracker_compass.json"),
            "minecraft:item/compass");
        assertContains(PACK.resolve("shared/registry.yml"),
            "java-model: bigcasares:item/nuke_shot", "texture: minecraft:blocks/tnt_side",
            "java-model: bigcasares:item/tracker_compass", "texture: minecraft:items/compass_item");
        assertContains(PACK.resolve("bedrock/textures/item_texture.json"),
            "textures/blocks/tnt_side", "textures/items/compass_item");
        assertContains(PACK.resolve("java/assets/bigcasares/lang/en_us.json"),
            "item.bigcasares.tracker_compass", "item.bigcasares.nuke_shot");
        assertContains(PACK.resolve("java/assets/bigcasares/lang/es_es.json"),
            "item.bigcasares.tracker_compass", "item.bigcasares.nuke_shot");
        assertContains(PACK.resolve("shared/registry.yml"), "tracker-compass:", "nuke-shot:");
    }

    private static void assertContains(Path path, String... values) throws Exception {
        String content = Files.readString(path);
        for (String value : values) {
            assertTrue(content.contains(value), path + " is missing " + value);
        }
    }
}
