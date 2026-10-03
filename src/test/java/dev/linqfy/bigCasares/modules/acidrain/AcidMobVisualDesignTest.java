package dev.linqfy.bigCasares.modules.acidrain;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.joml.Quaterniond;
import org.joml.Vector3d;

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
    void crawlerLegSegmentsReachTheirJointsWithBetterModelRotationOrder() throws Exception {
        JsonObject m = model("toxic_mob");
        for (int leg = 1; leg <= 4; leg++) {
            for (String side : Set.of("Left", "Right")) {
                String name = "Leg" + leg;
                Vector3d hip = null;
                for (var bone : m.getAsJsonArray("groups")) {
                    JsonObject b = bone.getAsJsonObject();
                    if (b.get("name").getAsString().equals(name + side)) hip = vector(b, "origin");
                }
                assertNotNull(hip);
                JsonObject joint = cube(m, name + "Joint" + side);
                Vector3d knee = vector(joint, "from").add(vector(joint, "to")).mul(0.5);
                assertEndpoint(cube(m, name + "Upper" + side), hip);
                assertEndpoint(cube(m, name + "Upper" + side), knee);
                assertEndpoint(cube(m, name + "Lower" + side), knee);
            }
        }
    }

    @Test
    void bowStringReachesBothLimbTips() throws Exception {
        JsonObject m = model("toxic_spitter");
        JsonObject string = cube(m, "BowString");
        for (Vector3d tip : new Vector3d[]{new Vector3d(8, 17, -2), new Vector3d(8, 4, -2)}) {
            assertEndpoint(string, tip);
        }
    }

    @Test
    void crawlerFangsRemainConnectedAfterExportRotation() throws Exception {
        JsonObject m = model("toxic_mob");
        for (String side : Set.of("Left", "Right")) {
            int sign = side.equals("Left") ? -1 : 1;
            Vector3d joint = new Vector3d(sign * 3.6, 2.7, -12);
            assertEndpoint(cube(m, "FrontClaw" + side), joint);
            assertEndpoint(cube(m, "FangTip" + side), joint);
        }
    }

    @Test
    void bowStaysUprightWhileArmRaisesToAim() throws Exception {
        JsonObject m = model("toxic_spitter");
        for (var animation : m.getAsJsonArray("animations")) {
            JsonObject a = animation.getAsJsonObject();
            if (!a.get("name").getAsString().equals("attack")) continue;
            for (double time : new double[]{0.2, 0.4}) {
                Quaterniond arm = keyframeRotation(a, "ArmRight", time);
                Quaterniond bow = keyframeRotation(a, "AcidBow", time);
                Vector3d up = new Vector3d(0, 1, 0).rotate(arm.mul(bow));
                assertTrue(up.y > 0.99, "bow must not turn sideways when aiming");
            }
            return;
        }
        fail("missing attack animation");
    }

    private static Quaterniond keyframeRotation(JsonObject animation, String bone, double time) {
        for (var value : animation.getAsJsonObject("animators").entrySet()) {
            JsonObject animator = value.getValue().getAsJsonObject();
            if (!animator.get("name").getAsString().equals(bone)) continue;
            for (var frame : animator.getAsJsonArray("keyframes")) {
                JsonObject f = frame.getAsJsonObject();
                if (!f.get("channel").getAsString().equals("rotation")
                    || Math.abs(f.get("time").getAsDouble() - time) > 0.0001) continue;
                JsonObject r = f.getAsJsonArray("data_points").get(0).getAsJsonObject();
                return new Quaterniond().rotationZYX(Math.toRadians(r.get("z").getAsDouble()),
                    Math.toRadians(r.get("y").getAsDouble()), Math.toRadians(r.get("x").getAsDouble()));
            }
        }
        throw new AssertionError("missing rotation: " + bone + " at " + time);
    }

    @Test
    void everyVisibleCubeHasSixTexturedFacesAndPositiveVolume() throws Exception {
        for (String name : Set.of("toxic_mob", "toxic_brute", "toxic_spitter")) {
            for (var element : model(name).getAsJsonArray("elements")) {
                JsonObject c = element.getAsJsonObject();
                assertEquals(Set.of("north", "south", "east", "west", "up", "down"),
                    c.getAsJsonObject("faces").keySet());
                Vector3d size = vector(c, "to").sub(vector(c, "from"));
                assertTrue(size.x > 0 && size.y > 0 && size.z > 0);
                for (var face : c.getAsJsonObject("faces").entrySet()) {
                    var uv = face.getValue().getAsJsonObject().getAsJsonArray("uv");
                    assertTrue(uv.get(2).getAsDouble() > uv.get(0).getAsDouble());
                    assertTrue(uv.get(3).getAsDouble() > uv.get(1).getAsDouble());
                }
            }
        }
    }

    private static JsonObject cube(JsonObject model, String name) {
        for (var e : model.getAsJsonArray("elements")) {
            if (e.getAsJsonObject().get("name").getAsString().equals(name)) return e.getAsJsonObject();
        }
        throw new AssertionError("Missing cube " + name);
    }

    private static Vector3d vector(JsonObject object, String field) {
        var a = object.getAsJsonArray(field);
        return new Vector3d(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble());
    }

    private static void assertEndpoint(JsonObject cube, Vector3d expected) {
        Vector3d from = vector(cube, "from"), to = vector(cube, "to");
        Vector3d origin = vector(cube, "origin");
        Vector3d rotation = cube.has("rotation") ? vector(cube, "rotation") : new Vector3d();
        Quaterniond q = new Quaterniond().rotationZYX(Math.toRadians(rotation.z),
            Math.toRadians(rotation.y), Math.toRadians(rotation.x));
        Vector3d start = new Vector3d((from.x + to.x)/2, from.y, (from.z + to.z)/2).sub(origin).rotate(q).add(origin);
        Vector3d end = new Vector3d((from.x + to.x)/2, to.y, (from.z + to.z)/2).sub(origin).rotate(q).add(origin);
        assertTrue(Math.min(start.distance(expected), end.distance(expected)) < 0.0001,
            cube.get("name").getAsString() + " is detached from joint " + expected);
    }

    @Test
    void creatureMeshesStayInsideTheirVisualBudget() throws Exception {
        for (String name : Set.of("toxic_mob", "toxic_brute", "toxic_spitter")) {
            JsonObject model = model(name);
            assertTrue(model.getAsJsonArray("elements").size() <= 128);
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
