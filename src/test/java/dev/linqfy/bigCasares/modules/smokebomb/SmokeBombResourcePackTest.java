package dev.linqfy.bigCasares.modules.smokebomb;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SmokeBombResourcePackTest {

    @Test
    void resourcePackUsesDedicatedSmokeBombItemModel() throws IOException {
        Path itemDefinition = Path.of("resourcepack", "java", "assets", "bigcasares", "items", "smoke_bomb.json");
        Path rawModel = Path.of("resourcepack", "java", "assets", "bigcasares", "models", "item", "smoke_bomb.json");
        Path texture = Path.of("resourcepack", "java", "assets", "bigcasares", "textures", "item", "smoke_bomb.png");

        assertTrue(Files.exists(itemDefinition), "Missing items/smoke_bomb.json");
        assertTrue(Files.exists(rawModel), "Missing models/item/smoke_bomb.json");
        assertTrue(Files.exists(texture), "Missing textures/item/smoke_bomb.png");

        String itemDefinitionJson = Files.readString(itemDefinition);
        assertTrue(itemDefinitionJson.contains("\"type\": \"minecraft:model\""));
        assertTrue(itemDefinitionJson.contains("\"model\": \"bigcasares:item/smoke_bomb\""));
    }
}
