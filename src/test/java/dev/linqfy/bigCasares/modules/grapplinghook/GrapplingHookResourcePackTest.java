package dev.linqfy.bigCasares.modules.grapplinghook;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the Java appearance wiring for the Ballesta Ark items: every tier
 * model resolves corrected 3D geometry, uses bounded Minecraft UV coordinates,
 * and has a distinct tier material while the legacy crossbow sprite stays valid.
 */
class GrapplingHookResourcePackTest {

    @Test
    void everyTierModelUsesTheExportedThreeDimensionalGeometry() throws IOException {
        for (int tier = 1; tier <= 6; tier++) {
            Path model = Path.of("resourcepack", "java", "assets", "bigcasares",
                "models", "item", "grappling_hook_" + tier + ".json");
            assertTrue(Files.exists(model), "Missing models/item/grappling_hook_" + tier + ".json");
            String modelJson = Files.readString(model);
            assertTrue(modelJson.contains("\"parent\": \"minecraft:item/generated\""),
                "grappling_hook_" + tier + " must use the vanilla item transform model");
            assertTrue(modelJson.contains("\"0\": \"bigcasares:item/grappling_hook_3d_" + tier + "\""),
                "grappling_hook_" + tier + " must reference its tier 3D material");
            assertTrue(modelJson.contains("\"elements\""),
                "grappling_hook_" + tier + " must contain exported cube geometry");
            assertTrue(modelJson.contains("\"firstperson_righthand\""),
                "grappling_hook_" + tier + " must define first-person transforms");
            assertTrue(modelJson.contains("\"thirdperson_righthand\""),
                "grappling_hook_" + tier + " must define third-person transforms");
        }
    }

    @Test
    void everyTierHasAnRgbaThreeDimensionalMaterial() throws IOException {
        for (int tier = 1; tier <= 6; tier++) {
            Path texture = Path.of("resourcepack", "java", "assets", "bigcasares",
                "textures", "item", "grappling_hook_3d_" + tier + ".png");
            assertTrue(Files.exists(texture), "Missing 3D material for tier " + tier);
            byte[] png = Files.readAllBytes(texture);
            assertTrue(png.length > 8, "tier " + tier + " texture is not a PNG");
            assertTrue(png[25] == 6, "tier " + tier + " texture must have an alpha channel");
        }
    }

    @Test
    void allExportedFaceUvsFitMinecraftCoordinateSpace() throws IOException {
        for (int tier = 1; tier <= 6; tier++) {
            int currentTier = tier;
            Path model = Path.of("resourcepack", "java", "assets", "bigcasares",
                "models", "item", "grappling_hook_" + tier + ".json");
            JsonObject root = JsonParser.parseString(Files.readString(model)).getAsJsonObject();
            root.getAsJsonArray("elements").forEach(element ->
                element.getAsJsonObject().getAsJsonObject("faces").entrySet().forEach(face ->
                    face.getValue().getAsJsonObject().getAsJsonArray("uv").forEach(coordinate -> {
                        double value = coordinate.getAsDouble();
                        assertTrue(value >= 0.0 && value <= 16.0,
                            "tier " + currentTier + " has an out-of-range UV coordinate: " + value);
                    })));
        }
    }

    @Test
    void threeDimensionalHooksUseReadableInventoryAndHandScale() throws IOException {
        for (int tier = 1; tier <= 6; tier++) {
            Path model = Path.of("resourcepack", "java", "assets", "bigcasares",
                "models", "item", "grappling_hook_" + tier + ".json");
            JsonObject display = JsonParser.parseString(Files.readString(model)).getAsJsonObject()
                .getAsJsonObject("display");
            assertEquals(0.95, display.getAsJsonObject("gui").getAsJsonArray("scale").get(0).getAsDouble());
            assertEquals(0.68, display.getAsJsonObject("firstperson_righthand")
                    .getAsJsonArray("scale").get(0).getAsDouble());
            assertEquals(-90, display.getAsJsonObject("firstperson_righthand")
                .getAsJsonArray("rotation").get(0).getAsInt());
            assertEquals(35, display.getAsJsonObject("firstperson_righthand")
                .getAsJsonArray("rotation").get(2).getAsInt());
            assertEquals(125, display.getAsJsonObject("firstperson_lefthand")
                .getAsJsonArray("rotation").get(2).getAsInt());
            assertEquals(0.9, display.getAsJsonObject("thirdperson_righthand")
                    .getAsJsonArray("scale").get(0).getAsDouble());
            assertEquals(-90, display.getAsJsonObject("thirdperson_righthand")
                .getAsJsonArray("rotation").get(0).getAsInt());
            assertEquals(30, display.getAsJsonObject("thirdperson_righthand")
                .getAsJsonArray("rotation").get(2).getAsInt());
            assertEquals(0.1, display.getAsJsonObject("thirdperson_righthand")
                .getAsJsonArray("translation").get(1).getAsDouble());
            assertEquals(120, display.getAsJsonObject("thirdperson_lefthand")
                .getAsJsonArray("rotation").get(2).getAsInt());
            assertEquals(0.8, display.getAsJsonObject("fixed")
                .getAsJsonArray("scale").get(0).getAsDouble());
        }
    }

    @Test
    void crossbowModelIsAGeneratedSpriteOverTheInventoryIcon() throws IOException {
        Path model = Path.of("resourcepack", "java", "assets", "bigcasares",
            "models", "item", "grappling_hook_crossbow.json");
        Path texture = Path.of("resourcepack", "java", "assets", "bigcasares",
            "textures", "item", "grappling_hook_crossbow.png");

        assertTrue(Files.exists(model), "Missing models/item/grappling_hook_crossbow.json");
        String modelJson = Files.readString(model);
        assertTrue(modelJson.contains("\"parent\": \"minecraft:item/generated\""));
        assertTrue(modelJson.contains("\"layer0\": \"bigcasares:item/grappling_hook_crossbow\""),
            "crossbow model must reference the inventory icon as layer0");

        assertTrue(Files.exists(texture), "Missing textures/item/grappling_hook_crossbow.png");
        byte[] png = Files.readAllBytes(texture);
        assertTrue(png.length > 8, "texture is not a PNG");
        // PNG IHDR color type byte: 6 = RGBA with alpha.
        assertTrue(png[25] == 6, "grappling_hook_crossbow texture must have an alpha channel");
    }

    @Test
    void everyTierItemDefinitionReferencesTheTierModel() throws IOException {
        for (int tier = 1; tier <= 6; tier++) {
            Path definition = Path.of("resourcepack", "java", "assets", "bigcasares",
                "items", "grappling_hook_" + tier + ".json");
            assertTrue(Files.exists(definition), "Missing items/grappling_hook_" + tier + ".json");
            String json = Files.readString(definition);
            assertTrue(json.contains("\"model\": \"bigcasares:item/grappling_hook_" + tier + "\""),
                "item definition must reference models/item/grappling_hook_" + tier + ".json");
        }
    }
}
