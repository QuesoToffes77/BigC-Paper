package dev.linqfy.bigCasares.modules.glider;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GliderResourcePackTest {

    @Test
    void allSixTiersResolveTheirModelAndTexture() throws Exception {
        Path root = Path.of("resourcepack", "java", "assets", "bigcasares");
        Path base = root.resolve("models/item/glider_base.json");
        Path deployedBase = root.resolve("models/item/glider_deployed_base.json");
        assertTrue(Files.isRegularFile(base));
        assertTrue(Files.isRegularFile(deployedBase));
        String baseJson = Files.readString(base);
        assertTrue(baseJson.contains("firstperson_lefthand"));
        assertTrue(baseJson.contains("thirdperson_lefthand"));

        String deployedBaseJson = Files.readString(deployedBase);
        assertFalse(deployedBaseJson.contains("minecraft:block/"),
            "Modern cuboid item models cannot mix item and block texture atlases");
        assertTrue(deployedBaseJson.contains("bigcasares:item/glider_frame_copper"));
        assertTrue(deployedBaseJson.contains("bigcasares:item/glider_support_dark_oak"));
        assertTrue(Files.isRegularFile(root.resolve("textures/item/glider_frame_copper.png")));
        assertTrue(Files.isRegularFile(root.resolve("textures/item/glider_support_dark_oak.png")));

        for (int tier = 1; tier <= 6; tier++) {
            String id = "glider_tier_" + tier;
            Path item = root.resolve("items/" + id + ".json");
            Path model = root.resolve("models/item/" + id + ".json");
            Path texture = root.resolve("textures/item/" + id + ".png");
            assertTrue(Files.isRegularFile(item), "Missing item definition " + id);
            assertTrue(Files.isRegularFile(model), "Missing model " + id);
            assertTrue(Files.isRegularFile(texture), "Missing texture " + id);
            String itemJson = Files.readString(item);
            assertTrue(itemJson.contains("minecraft:display_context"));
            assertTrue(itemJson.contains("minecraft:using_item"));
            assertTrue(itemJson.contains("\"when\":\"gui\""));
            assertTrue(itemJson.contains("bigcasares:item/" + id));
            assertTrue(Files.readString(model).contains("bigcasares:item/" + id));

            String deployedId = "glider_deployed_tier_" + tier;
            Path deployedItem = root.resolve("items/" + deployedId + ".json");
            Path deployedModel = root.resolve("models/item/" + deployedId + ".json");
            assertTrue(Files.isRegularFile(deployedItem), "Missing deployed item definition " + deployedId);
            assertTrue(Files.isRegularFile(deployedModel), "Missing deployed model " + deployedId);
            assertTrue(Files.readString(deployedItem).contains("bigcasares:item/" + deployedId));
            assertTrue(Files.readString(deployedModel).contains("bigcasares:item/glider_deployed_base"));
            assertTrue(itemJson.contains("bigcasares:item/" + deployedId));
        }


        assertTrue(deployedBaseJson.contains("\"gui\""),
            "The deployed model needs a bounded GUI transform for the slot icon");
    }
}
