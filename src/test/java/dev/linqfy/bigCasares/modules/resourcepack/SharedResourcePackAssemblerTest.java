package dev.linqfy.bigCasares.modules.resourcepack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedResourcePackAssemblerTest {

    @TempDir
    Path tempDir;

    @Test
    void emitsBuiltinBedrockAtlasPathWithoutCopyingSourcePng() throws Exception {
        Path sourceRoot = tempDir.resolve("source");
        Path javaRoot = tempDir.resolve("java");
        Path bedrockRoot = tempDir.resolve("bedrock");
        ResourcePackRegistry registry = new ResourcePackRegistry(List.of(
            new ResourcePackAsset("nuke-shot", "item-model", null, null,
                "minecraft:blocks/tnt_side", null, null)
        ));

        assertDoesNotThrow(
            () -> new SharedResourcePackAssembler().assemble(sourceRoot, javaRoot, bedrockRoot, registry));

        String atlas = Files.readString(bedrockRoot.resolve("textures/item_texture.json"));
        assertTrue(atlas.contains("\"textures/blocks/tnt_side\""));
        assertFalse(Files.exists(javaRoot.resolve("assets/bigcasares/textures/blocks/tnt_side.png")));
        assertFalse(Files.exists(bedrockRoot.resolve("textures/blocks/tnt_side.png")));
    }
}
