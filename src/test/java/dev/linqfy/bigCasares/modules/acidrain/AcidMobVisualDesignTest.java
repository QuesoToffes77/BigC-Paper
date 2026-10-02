package dev.linqfy.bigCasares.modules.acidrain;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AcidMobVisualDesignTest {
    private static final Path MODELS = Path.of("src/main/resources/bettermodel/models");

    @Test
    void crawlerHasEightIndependentlyAnimatedLegs() throws Exception {
        JsonObject model = model("toxic_mob");
        Set<String> names = boneNames(model);
        for (int leg = 1; leg <= 4; leg++) {
            assertTrue(names.contains("Leg" + leg + "Left"));
            assertTrue(names.contains("Leg" + leg + "Right"));
        }
        assertTrue(names.contains("Jaw"));
        assertArticulatedWalk(model, 8);
    }

    @Test
    void humanoidsHaveArticulatedLimbsAndFaces() throws Exception {
        for (String name : Set.of("toxic_brute", "toxic_spitter")) {
            JsonObject model = model(name);
            assertTrue(boneNames(model).containsAll(Set.of(
                "Head", "Jaw", "ArmLeft", "ArmRight", "LegLeft", "LegRight")));
            assertArticulatedWalk(model, 4);
        }
    }

    @Test
    void allUvsAndBoneReferencesAreValid() throws Exception {
        for (String name : Set.of("toxic_mob", "toxic_brute", "toxic_spitter")) {
            JsonObject model = model(name);
            Set<String> ids = new HashSet<>();
            for (var element : model.getAsJsonArray("elements")) {
                JsonObject cube = element.getAsJsonObject();
                assertTrue(ids.add(cube.get("uuid").getAsString()));
                for (var face : cube.getAsJsonObject("faces").entrySet()) {
                    JsonObject data = face.getValue().getAsJsonObject();
                    assertEquals(0, data.get("texture").getAsInt());
                    for (var uv : data.getAsJsonArray("uv")) {
                        assertTrue(uv.getAsDouble() >= 0 && uv.getAsDouble() <= 128);
                    }
                }
            }
            for (var group : model.getAsJsonArray("groups")) {
                assertTrue(ids.add(group.getAsJsonObject().get("uuid").getAsString()));
            }
            Set<String> referenced = new HashSet<>();
            collectOutliner(model.getAsJsonArray("outliner"), ids, referenced);
            assertEquals(ids, referenced, "every cube/bone must be reachable exactly once");
            for (var animation : model.getAsJsonArray("animations")) {
                JsonObject a = animation.getAsJsonObject();
                for (var animator : a.getAsJsonObject("animators").entrySet()) {
                    assertTrue(ids.contains(animator.getKey()));
                    for (var frame : animator.getValue().getAsJsonObject().getAsJsonArray("keyframes")) {
                        double time = frame.getAsJsonObject().get("time").getAsDouble();
                        assertTrue(time >= 0 && time <= a.get("length").getAsDouble());
                    }
                }
            }
        }
    }

    @Test
    void betterModelTextureMetadataIsCanonical() throws Exception {
        for (String name : Set.of("toxic_mob", "toxic_brute", "toxic_spitter")) {
            JsonObject texture = model(name).getAsJsonArray("textures").get(0).getAsJsonObject();
            assertTrue(texture.has("frame_time"), "texture timing must not default to zero");
            assertEquals(1, texture.get("frame_time").getAsInt());
            assertFalse(texture.get("frame_interpolate").getAsBoolean());
            assertEquals(128, texture.get("uv_width").getAsInt());
            assertEquals(128, texture.get("uv_height").getAsInt());
        }
    }

    @Test
    void referenceCreaturesHavePhysicalAcidDripsInsteadOfMechanicalTanks() throws Exception {
        for (String name : Set.of("toxic_mob", "toxic_brute", "toxic_spitter")) {
            Set<String> cubes = cubeNames(model(name));
            assertTrue(cubes.stream().filter(c -> c.startsWith("AcidDrip")).count() >= 10,
                name + " needs hanging acid geometry, not just painted stains");
            assertFalse(cubes.stream().anyMatch(c -> c.contains("Tank") || c.contains("Valve")
                || c.contains("Plate") || c.contains("Mantle")), "references have no mechanical armor/tanks");
        }
    }

    @Test
    void referenceAnatomyIncludesExposedSkullRibsAndMeltedSpiderFlesh() throws Exception {
        assertTrue(cubeNames(model("toxic_brute")).containsAll(Set.of(
            "ExposedSkull", "ExposedChestRib0", "ExposedChestRib1", "SkullWound")));
        assertTrue(cubeNames(model("toxic_spitter")).containsAll(Set.of(
            "SkullFracture", "SkullCrown", "BowUpperLimb", "BowLowerLimb")));
        assertTrue(cubeNames(model("toxic_mob")).containsAll(Set.of(
            "MeltedAbdomen", "SkullFace", "ExposedAbdomenRib0")));
    }

    @Test
    void creatureMeshesStayInsideTheirVisualBudget() throws Exception {
        for (String name : Set.of("toxic_mob", "toxic_brute", "toxic_spitter")) {
            JsonObject model = model(name);
            assertTrue(model.getAsJsonArray("elements").size() <= 96);
            assertTrue(model.getAsJsonArray("groups").size() <= 16);
        }
    }

    private static Set<String> cubeNames(JsonObject model) {
        Set<String> names = new HashSet<>();
        for (var cube : model.getAsJsonArray("elements")) names.add(cube.getAsJsonObject().get("name").getAsString());
        return names;
    }

    private static void collectOutliner(com.google.gson.JsonArray nodes, Set<String> ids, Set<String> visited) {
        for (var node : nodes) {
            String id = node.isJsonPrimitive() ? node.getAsString() : node.getAsJsonObject().get("uuid").getAsString();
            assertTrue(ids.contains(id));
            assertTrue(visited.add(id), "duplicate outliner node: " + id);
            if (node.isJsonObject()) collectOutliner(node.getAsJsonObject().getAsJsonArray("children"), ids, visited);
        }
    }

    private static void assertArticulatedWalk(JsonObject model, int limbs) {
        for (var animation : model.getAsJsonArray("animations")) {
            JsonObject a = animation.getAsJsonObject();
            if (a.get("name").getAsString().equals("walk")) {
                assertTrue(a.getAsJsonObject("animators").size() >= limbs + 1);
                return;
            }
        }
        fail("missing walk animation");
    }

    private static Set<String> boneNames(JsonObject model) {
        Set<String> names = new HashSet<>();
        for (var group : model.getAsJsonArray("groups")) names.add(group.getAsJsonObject().get("name").getAsString());
        return names;
    }

    private static JsonObject model(String name) throws Exception {
        return JsonParser.parseString(Files.readString(MODELS.resolve(name + ".bbmodel"))).getAsJsonObject();
    }
}
