package dev.linqfy.bigCasares.modules.resourcepack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourcePackValidatorTest {

    @TempDir
    Path tempDir;

    @Test
    void rejectsDuplicateLogicalIdentifiers() throws IOException {
        Path registry = tempDir.resolve("registry.yml");
        Files.writeString(registry, """
            assets:
              nexus:
                type: entity-model
                java-model: bigcasares:nexus
              nexus:
                type: boss-model
                java-model: bigcasares:abyss_guardian
            """);

        assertThrows(IllegalArgumentException.class, () -> ResourcePackRegistry.load(registry));
    }

    @Test
    void rejectsRegisteredAssetsWhoseRequiredFilesAreMissing() throws IOException {
        ResourcePackFixture fixture = ResourcePackFixture.create(tempDir, false);
        ResourcePackRegistry registry = ResourcePackRegistry.load(fixture.registry());

        assertThrows(IllegalArgumentException.class,
            () -> new ResourcePackValidator().validate(registry, fixture.javaRoot(), fixture.bedrockRoot()));
    }

    @Test
    void acceptsBuiltinTextureWithoutLocalPngFiles() throws IOException {
        Path javaRoot = tempDir.resolve("java");
        Path bedrockRoot = tempDir.resolve("bedrock");
        Files.createDirectories(javaRoot);
        Files.createDirectories(bedrockRoot);
        Files.writeString(javaRoot.resolve("pack.mcmeta"), "{}");
        Files.writeString(bedrockRoot.resolve("manifest.json"), "{}");
        ResourcePackRegistry registry = new ResourcePackRegistry(java.util.List.of(
            new ResourcePackAsset("tracker-compass", "item-model", null, null,
                "minecraft:items/compass_item", null, null)
        ));

        assertDoesNotThrow(() -> new ResourcePackValidator().validate(registry, javaRoot, bedrockRoot));
    }

    @Test
    void rejectsBuiltinTextureWithoutPath() {
        assertThrows(IllegalArgumentException.class, () -> new ResourcePackAsset(
            "tracker-compass", "item-model", null, null, "minecraft:", null, null));
    }

    @Test
    void rejectsTraversalInLoadedBuiltinTexturePath() throws IOException {
        Path registry = tempDir.resolve("registry.yml");
        Files.writeString(registry, """
            assets:
              nuke-shot:
                type: item-model
                texture: minecraft:../outside
            """);

        assertThrows(IllegalArgumentException.class, () -> ResourcePackRegistry.load(registry));
    }
}
