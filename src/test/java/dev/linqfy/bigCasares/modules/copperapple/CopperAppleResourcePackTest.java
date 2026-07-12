package dev.linqfy.bigCasares.modules.copperapple;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CopperAppleResourcePackTest {

    @Test
    void resourcePackUsesDedicatedCopperAppleItemModel() throws IOException {
        Path itemDefinition = Path.of("resourcepack", "assets", "bigcasares", "items", "copper_apple.json");
        Path rawModel = Path.of("resourcepack", "assets", "bigcasares", "models", "item", "copper_apple.json");
        Path texture = Path.of("resourcepack", "assets", "bigcasares", "textures", "item", "copper_apple.png");
        Path packMeta = Path.of("resourcepack", "pack.mcmeta");

        assertTrue(Files.exists(itemDefinition), "Missing items/copper_apple.json");
        assertTrue(Files.exists(rawModel), "Missing models/item/copper_apple.json");
        assertTrue(Files.exists(texture), "Missing textures/item/copper_apple.png");

        String itemDefinitionJson = Files.readString(itemDefinition);
        String packMetaJson = Files.readString(packMeta);

        assertTrue(itemDefinitionJson.contains("\"type\": \"minecraft:model\""));
        assertTrue(itemDefinitionJson.contains("\"model\": \"bigcasares:item/copper_apple\""));
        assertTrue(packMetaJson.contains("\"pack_format\": 88"));
    }
}
