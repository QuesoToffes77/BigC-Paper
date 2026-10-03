package dev.linqfy.bigCasares.modules.resourcepack;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end audit of the real {@code resourcepack/} tree: runs the production
 * {@link ResourcePackBuilder}, then validates every archive entry (JSON syntax,
 * PNG readability, duplicate entries) and cross-checks every registry item
 * against the Java and Bedrock packs plus the generated bedrock
 * {@code item_texture.json}. A broken or dangling asset fails the build.
 */
class ResourcePackAssetAuditTest {

    @TempDir
    Path tempDir;

    @Test
    void realPackBuildsAndEveryArchiveEntryIsValid() throws IOException {
        Path sourceRoot = findSourceRoot();
        Assumptions.assumeTrue(sourceRoot != null, "resourcepack source tree not found");

        PackOutput output = buildPack(sourceRoot);

        new ResourcePackArchiveValidator().validateJava(output.javaZip());
        new ResourcePackArchiveValidator().validateBedrock(output.bedrockZip());
    }

    @Test
    void everyRegistryItemIsPresentInBothPacks() throws IOException {
        Path sourceRoot = findSourceRoot();
        Assumptions.assumeTrue(sourceRoot != null, "resourcepack source tree not found");

        PackOutput output = buildPack(sourceRoot);
        ResourcePackRegistry registry = ResourcePackRegistry.load(sourceRoot.resolve("shared/registry.yml"));

        try (ZipFile java = new ZipFile(output.javaZip().toFile());
             ZipFile bedrock = new ZipFile(output.bedrockZip().toFile())) {
            String itemTexture = readEntry(bedrock, "textures/item_texture.json");
            for (ResourcePackAsset asset : registry.assets()) {
                if (!"item-model".equals(asset.type())) {
                    continue;
                }
                String model = asset.javaModel();
                int colon = model.indexOf(':');
                String namespace = model.substring(0, colon);
                String modelPath = model.substring(colon + 1);
                String itemName = modelPath.startsWith("item/") ? modelPath.substring(5) : modelPath;

                assertEntry(java, "assets/" + namespace + "/models/" + modelPath + ".json",
                    "model for " + asset.id());
                assertEntry(java, "assets/" + namespace + "/items/" + itemName + ".json",
                    "item definition for " + asset.id());
                if (!asset.usesBuiltinTexture()) {
                    assertEntry(java, "assets/bigcasares/textures/" + asset.texturePath() + ".png",
                        "java texture for " + asset.id());
                    assertEntry(bedrock, "textures/" + asset.texturePath() + ".png",
                        "bedrock texture for " + asset.id());
                }
                String entryKey = "bigcasares." + asset.id().replace('-', '_');
                assertTrue(itemTexture.contains("\"" + entryKey + "\""),
                    "item_texture.json missing entry " + entryKey);
                assertTrue(itemTexture.contains("\"textures/" + asset.texturePath() + "\""),
                    "item_texture.json entry " + entryKey + " must reference textures/" + asset.texturePath());
            }
        }
    }

    @Test
    void hooksAndAcidRainItemsAreComplete() throws IOException {
        Path sourceRoot = findSourceRoot();
        Assumptions.assumeTrue(sourceRoot != null, "resourcepack source tree not found");

        PackOutput output = buildPack(sourceRoot);

        try (ZipFile java = new ZipFile(output.javaZip().toFile());
             ZipFile bedrock = new ZipFile(output.bedrockZip().toFile())) {
            for (int tier = 1; tier <= 6; tier++) {
                String hook = "grappling_hook_" + tier;
                assertEntry(java, "assets/bigcasares/items/" + hook + ".json", "hook item def");
                assertEntry(java, "assets/bigcasares/models/item/" + hook + ".json", "hook model");
                assertEntry(java, "assets/bigcasares/textures/item/" + hook + ".png", "hook java texture");
                assertEntry(bedrock, "textures/item/" + hook + ".png", "hook bedrock texture");
            }
            for (String acid : new String[]{"potassium_nitrate", "nitric_acid"}) {
                assertEntry(java, "assets/bigcasares/models/item/" + acid + ".json", "acid model");
                assertEntry(java, "assets/bigcasares/textures/item/" + acid + ".png", "acid java texture");
                assertEntry(bedrock, "textures/item/" + acid + ".png", "acid bedrock texture");
            }
            assertEntry(java, "assets/bigcasares/lang/es_es.json", "es lang");
            assertEntry(java, "assets/bigcasares/lang/en_us.json", "en lang");
        }
    }

    private PackOutput buildPack(Path sourceRoot) throws IOException {
        Path output = tempDir.resolve("pack-output");
        new ResourcePackBuilder(sourceRoot, output).build();
        return new PackOutput(output.resolve("bigcasares-java.zip"), output.resolve("bigcasares-bedrock.mcpack"));
    }

    private static Path findSourceRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        for (int depth = 0; depth < 4; depth++) {
            Path candidate = current.resolve("resourcepack/shared/registry.yml");
            if (Files.isRegularFile(candidate)) {
                return current.resolve("resourcepack");
            }
            Path parent = current.getParent();
            if (parent == null) {
                return null;
            }
            current = parent;
        }
        return null;
    }

    private static void assertEntry(ZipFile zip, String name, String description) throws IOException {
        assertNotNull(zip.getEntry(name), "missing " + description + ": " + name);
    }

    private static String readEntry(ZipFile zip, String name) throws IOException {
        var entry = zip.getEntry(name);
        assertNotNull(entry, "missing entry: " + name);
        try (var input = zip.getInputStream(entry)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private record PackOutput(Path javaZip, Path bedrockZip) {
    }
}
