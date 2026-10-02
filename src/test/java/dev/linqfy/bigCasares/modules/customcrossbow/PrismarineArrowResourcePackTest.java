package dev.linqfy.bigCasares.modules.customcrossbow;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PrismarineArrowResourcePackTest {

    @Test
    void resourcePackUsesDedicatedPrismarineArrowItemModel() throws IOException {
        Path itemDefinition = Path.of("resourcepack", "java", "assets", "bigcasares", "items", "prismarine_arrow.json");
        Path rawModel = Path.of("resourcepack", "java", "assets", "bigcasares", "models", "item", "prismarine_arrow.json");
        Path texture = Path.of("resourcepack", "java", "assets", "bigcasares", "textures", "item", "prismarine_arrow.png");

        assertTrue(Files.exists(itemDefinition), "Missing items/prismarine_arrow.json");
        assertTrue(Files.exists(rawModel), "Missing models/item/prismarine_arrow.json");
        assertTrue(Files.exists(texture), "Missing textures/item/prismarine_arrow.png");

        String itemDefinitionJson = Files.readString(itemDefinition);
        assertTrue(itemDefinitionJson.contains("\"type\": \"minecraft:model\""));
        assertTrue(itemDefinitionJson.contains("\"model\": \"bigcasares:item/prismarine_arrow\""));
    }

    @Test
    void resourcePackHasLoadedCrossbowModelsForSpecialCharges() throws IOException {
        for (CustomCrossbowChargeType chargeType : CustomCrossbowChargeType.values()) {
            String modelId = CustomCrossbowRules.loadedCrossbowModelId(chargeType);
            Path itemDefinition = Path.of("resourcepack", "java", "assets", "bigcasares", "items", modelId + ".json");
            Path rawModel = Path.of("resourcepack", "java", "assets", "bigcasares", "models", "item", modelId + ".json");
            Path texture = Path.of("resourcepack", "java", "assets", "bigcasares", "textures", "item", modelId + ".png");

            assertTrue(Files.exists(itemDefinition), "Missing items/" + modelId + ".json");
            assertTrue(Files.exists(rawModel), "Missing models/item/" + modelId + ".json");
            assertTrue(Files.exists(texture), "Missing textures/item/" + modelId + ".png");

            String itemDefinitionJson = Files.readString(itemDefinition);
            assertTrue(itemDefinitionJson.contains("\"type\": \"minecraft:model\""));
            assertTrue(itemDefinitionJson.contains("\"model\": \"bigcasares:item/" + modelId + "\""));

            String rawModelJson = Files.readString(rawModel);
            assertTrue(rawModelJson.contains("\"parent\": \"minecraft:item/crossbow_arrow\""),
                    "Loaded crossbow model must inherit the vanilla arrow-loaded crossbow transforms: " + modelId);

            Path generatedPackSourceModel = Path.of(
                    "resourcepack", "java", "assets", "bigcasares", "models", "item", modelId + ".json"
            );
            assertTrue(Files.exists(generatedPackSourceModel),
                    "Missing Java pack models/item/" + modelId + ".json");
            String generatedPackSourceJson = Files.readString(generatedPackSourceModel);
            assertTrue(generatedPackSourceJson.contains("\"parent\": \"minecraft:item/crossbow_arrow\""),
                    "Generated Java pack source must inherit the vanilla arrow-loaded crossbow transforms: " + modelId);
        }
    }
}
