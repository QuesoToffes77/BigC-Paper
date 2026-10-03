package dev.linqfy.bigCasares.modules.copperapple;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CopperAppleResourcePackTest {

    @Test
    void resourcePackUsesDedicatedCopperAppleItemModel() throws IOException {
        Path packMeta = Path.of("resourcepack", "pack.mcmeta");

        String packMetaJson = Files.readString(packMeta);
        assertTrue(packMetaJson.contains("\"pack_format\": 88"));

        for (String modelId : List.of(
            "copper_apple",
            "copper_apple_exposed",
            "copper_apple_weathered",
            "copper_apple_oxidized"
        )) {
            assertModelChain(modelId);
        }
    }

    private static void assertModelChain(String modelId) throws IOException {
        Path itemDefinition = Path.of(
            "resourcepack", "java", "assets", "bigcasares", "items", modelId + ".json");
        Path rawModel = Path.of(
            "resourcepack", "java", "assets", "bigcasares", "models", "item", modelId + ".json");
        Path texture = Path.of(
            "resourcepack", "java", "assets", "bigcasares", "textures", "item", modelId + ".png");

        assertTrue(Files.exists(itemDefinition), "Missing item definition for " + modelId);
        assertTrue(Files.exists(rawModel), "Missing model for " + modelId);
        assertTrue(Files.exists(texture), "Missing texture for " + modelId);
        assertTrue(Files.readString(itemDefinition).contains("\"model\": \"bigcasares:item/" + modelId + "\""));
        assertTrue(Files.readString(rawModel).contains("\"layer0\": \"bigcasares:item/" + modelId + "\""));
    }
}
