package dev.linqfy.bigCasares.modules.pveboss;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SahurAssetContractTest {

    private static final Path UPLOAD = Path.of("agent_uploads", "boss1");
    private static final Path BETTER_MODEL = Path.of(
        "build", "run-server", "plugins", "BetterModel", "models"
    );
    private static final Path BEDROCK = Path.of("resourcepack", "bedrock");

    @Test
    void javaModelsExposeEverySahurAndSummonedBatAnimation() throws Exception {
        Path sahur = BETTER_MODEL.resolve("boss_sahur.bbmodel");
        Path bat = BETTER_MODEL.resolve("bat_boss.bbmodel");

        assertTrue(Files.exists(sahur), "Missing installed boss_sahur.bbmodel");
        assertTrue(Files.exists(bat), "Missing installed bat_boss.bbmodel");

        assertAnimationNames(sahur, "idle", "walk", "sprint", "bat_hit", "spin_in_place", "stomp");
        assertAnimationNames(bat, "rise_type_throw", "spin_type_throw", "type_shield", "type_launch_attack");
        assertAnimationNames(Path.of("src/main/resources/bettermodel/models/boss_sahur.bbmodel"),
            "idle", "walk", "sprint", "bat_hit", "spin_in_place", "stomp");
        assertAnimationNames(Path.of("src/main/resources/bettermodel/models/bat_boss.bbmodel"),
            "rise_type_throw", "spin_type_throw", "type_shield", "type_launch_attack");
    }

    @Test
    void betterModelSourcesPreserveUploadedModelsWithA128PixelCanvas() throws Exception {
        assertUploadedModelInstalledWithNormalizedCanvas("boss_sahur.bbmodel");
        assertUploadedModelInstalledWithNormalizedCanvas("bat_boss.bbmodel");
    }

    @Test
    void bedrockExportsAndClientEntitiesAreInstalled() throws Exception {
        assertContains(BEDROCK.resolve("models/entity/sahur.geo.json"), "geometry.bigcasares.sahur");
        assertContains(BEDROCK.resolve("models/entity/sahur_bat.geo.json"), "geometry.bigcasares.sahur_bat");
        assertContains(
            BEDROCK.resolve("animations/sahur.animation.json"),
            "animation.bigcasares.sahur.idle",
            "animation.bigcasares.sahur.sprint",
            "139.28"
        );
        assertContains(
            BEDROCK.resolve("animations/sahur_bat.animation.json"),
            "animation.bigcasares.sahur_bat.rise_type_throw",
            "animation.bigcasares.sahur_bat.type_launch_attack"
        );

        assertContains(
            BEDROCK.resolve("entity/sahur.entity.json"),
            "\"identifier\": \"bigcasares:sahur\"",
            "\"geometry\": {\"default\": \"geometry.bigcasares.sahur\"}",
            "\"default\": \"textures/boss/sahur\"",
            "animation.bigcasares.sahur.sprint"
        );
        assertContains(
            BEDROCK.resolve("entity/sahur_bat.entity.json"),
            "\"identifier\": \"bigcasares:sahur_bat\"",
            "\"geometry\": {\"default\": \"geometry.bigcasares.sahur_bat\"}",
            "\"default\": \"textures/boss/sahur_bat\"",
            "animation.bigcasares.sahur_bat.type_launch_attack"
        );
    }

    @Test
    void bossAndSummonedBatTexturesReuseUploadedSakurBytes() throws Exception {
        byte[] expected = Files.readAllBytes(UPLOAD.resolve("sakur.png"));

        assertArrayEquals(expected, Files.readAllBytes(
            Path.of("src/main/resources/bettermodel/models/sakur.png")
        ));
        assertArrayEquals(expected, Files.readAllBytes(BETTER_MODEL.resolve("sakur.png")));
        assertArrayEquals(expected, Files.readAllBytes(BEDROCK.resolve("textures/boss/sahur.png")));
        assertArrayEquals(expected, Files.readAllBytes(BEDROCK.resolve("textures/boss/sahur_bat.png")));
    }

    @Test
    void betterModelUvCanvasMatchesTheUploaded128PixelTexture() throws Exception {
        assertNormalizedUvCanvas(Path.of("src/main/resources/bettermodel/models/boss_sahur.bbmodel"));
        assertNormalizedUvCanvas(Path.of("src/main/resources/bettermodel/models/bat_boss.bbmodel"));
    }

    private static void assertAnimationNames(Path path, String... names) throws Exception {
        String content = Files.readString(path);
        for (String name : names) {
            assertTrue(
                content.matches("(?s).*\\\"name\\\"\\s*:\\s*\\\"" + name + "\\\".*"),
                path + " is missing animation " + name
            );
        }
    }

    private static void assertContains(Path path, String... values) throws Exception {
        String content = Files.readString(path);
        for (String value : values) {
            assertTrue(content.contains(value), path + " is missing " + value);
        }
    }

    private static void assertNormalizedUvCanvas(Path path) throws Exception {
        JsonObject model = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        assertEquals(128, model.getAsJsonObject("resolution").get("width").getAsInt());
        assertEquals(128, model.getAsJsonObject("resolution").get("height").getAsInt());
        JsonObject texture = model.getAsJsonArray("textures").get(0).getAsJsonObject();
        assertEquals(128, texture.get("uv_width").getAsInt());
        assertEquals(128, texture.get("uv_height").getAsInt());
        assertTrue(maxUvCoordinate(model) > 19.0 && maxUvCoordinate(model) < 20.0,
            path + " must use the original 32-unit mesh UV coordinates on the 128px texture");
    }

    private static void assertUploadedModelInstalledWithNormalizedCanvas(String fileName) throws Exception {
        JsonObject expected = JsonParser.parseString(
            Files.readString(UPLOAD.resolve(fileName))
        ).getAsJsonObject();
        for (Path installed : new Path[] {
            Path.of("src", "main", "resources", "bettermodel", "models", fileName),
            BETTER_MODEL.resolve(fileName)
        }) {
            JsonObject actual = JsonParser.parseString(Files.readString(installed)).getAsJsonObject();
            assertNormalizedUvCanvas(installed);
            assertUvsScaledFromUpload(expected, actual, 0.25, installed);
            JsonObject comparableExpected = expected.deepCopy();
            comparableExpected.remove("resolution");
            removeFaceUvs(comparableExpected);
            JsonObject comparableActual = actual.deepCopy();
            comparableActual.remove("resolution");
            removeFaceUvs(comparableActual);
            assertEquals(comparableExpected, comparableActual,
                installed + " changed uploaded model data outside canvas and face UV normalization");
        }
    }

    private static void assertUvsScaledFromUpload(
        JsonObject expected,
        JsonObject actual,
        double scale,
        Path installed
    ) {
        for (int elementIndex = 0; elementIndex < expected.getAsJsonArray("elements").size(); elementIndex++) {
            JsonObject expectedFaces = expected.getAsJsonArray("elements").get(elementIndex)
                .getAsJsonObject().getAsJsonObject("faces");
            JsonObject actualFaces = actual.getAsJsonArray("elements").get(elementIndex)
                .getAsJsonObject().getAsJsonObject("faces");
            for (var faceEntry : expectedFaces.entrySet()) {
                JsonObject expectedUv = faceEntry.getValue().getAsJsonObject().getAsJsonObject("uv");
                JsonObject actualUv = actualFaces.getAsJsonObject(faceEntry.getKey()).getAsJsonObject("uv");
                for (var pointEntry : expectedUv.entrySet()) {
                    for (int coordinate = 0; coordinate < pointEntry.getValue().getAsJsonArray().size(); coordinate++) {
                        assertEquals(
                            pointEntry.getValue().getAsJsonArray().get(coordinate).getAsDouble() * scale,
                            actualUv.getAsJsonArray(pointEntry.getKey()).get(coordinate).getAsDouble(),
                            0.000001,
                            installed + " has an incorrect normalized UV coordinate"
                        );
                    }
                }
            }
        }
    }

    private static void removeFaceUvs(JsonObject model) {
        for (JsonElement element : model.getAsJsonArray("elements")) {
            JsonObject faces = element.getAsJsonObject().getAsJsonObject("faces");
            for (var faceEntry : faces.entrySet()) {
                faceEntry.getValue().getAsJsonObject().remove("uv");
            }
        }
    }

    private static double maxUvCoordinate(JsonObject model) {
        double maximum = Double.NEGATIVE_INFINITY;
        for (JsonElement element : model.getAsJsonArray("elements")) {
            JsonObject faces = element.getAsJsonObject().getAsJsonObject("faces");
            for (var faceEntry : faces.entrySet()) {
                JsonObject uv = faceEntry.getValue().getAsJsonObject().getAsJsonObject("uv");
                for (var pointEntry : uv.entrySet()) {
                    for (JsonElement coordinate : pointEntry.getValue().getAsJsonArray()) {
                        maximum = Math.max(maximum, coordinate.getAsDouble());
                    }
                }
            }
        }
        return maximum;
    }

}
