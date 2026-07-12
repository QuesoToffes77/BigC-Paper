package dev.linqfy.bigCasares.modules.resourcepack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourcePackBuilderTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesBothPacksAndDeterministicJavaShaOne() throws Exception {
        ResourcePackFixture fixture = ResourcePackFixture.create(tempDir, true);
        ResourcePackBuilder builder = new ResourcePackBuilder(fixture.sourceRoot(), fixture.outputRoot());

        ResourcePackBuildResult first = builder.build();
        Files.delete(fixture.outputRoot().resolve("manifest.json"));
        ResourcePackBuildResult second = builder.build();

        assertTrue(Files.isRegularFile(fixture.outputRoot().resolve("bigcasares-java.zip")));
        assertTrue(Files.isRegularFile(fixture.outputRoot().resolve("bigcasares-bedrock.mcpack")));
        assertTrue(Files.isRegularFile(fixture.outputRoot().resolve("checksums.yml")));
        assertEquals(first.manifest().javaSha1(), second.manifest().javaSha1());
        assertEquals(40, first.manifest().javaSha1().length());
    }

    @Test
    void preservesBedrockUuidAndSkipsUnchangedInputs() throws Exception {
        ResourcePackFixture fixture = ResourcePackFixture.create(tempDir, true);
        ResourcePackBuilder builder = new ResourcePackBuilder(fixture.sourceRoot(), fixture.outputRoot());

        ResourcePackBuildResult first = builder.build();
        ResourcePackBuildResult second = builder.build();

        assertEquals(UUID.fromString("7e1cc35b-7a9f-4af4-b930-93f936f9cb64"), first.manifest().bedrockUuid());
        assertFalse(first.skipped());
        assertTrue(second.skipped());
        assertEquals(first.manifest(), second.manifest());
    }
}
