package dev.linqfy.bigCasares.modules.resourcepack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StagedPackBuilderTest {

    @TempDir
    Path tempDir;

    @Test
    void finalMergedBytesDetermineStableIdentityAndStagingIsCloseable() throws Exception {
        ResourcePackFixture fixture = ResourcePackFixture.create(tempDir.resolve("fixture"), true);
        Path layer = zip("bettermodel.zip", Map.of(
            "pack.mcmeta", "{\"pack\":{\"pack_format\":88,\"description\":\"layer\"}}",
            "assets/bettermodel/models/nexus.json", "{\"elements\":[]}"
        ));
        StagedPackBuilder builder = new StagedPackBuilder(
            fixture.sourceRoot(), tempDir.resolve("packs"),
            List.of(JavaPackLayerProvider.fixed("bettermodel", layer))
        );

        StagedPackCandidate first = builder.build(UUID.randomUUID());
        StagedPackCandidate second = builder.build(UUID.randomUUID());

        assertEquals(first.javaSha256(), second.javaSha256());
        assertEquals(first.manifest().javaUuid(), second.manifest().javaUuid());
        assertEquals(first.javaSha256().substring(0, 12), first.manifest().version());
        Path firstStaging = first.stagingDirectory();
        first.close();
        first.close();
        second.close();
        assertFalse(Files.exists(firstStaging));
    }

    @Test
    void betterModelOnlyByteChangeProducesNewIdentity() throws Exception {
        ResourcePackFixture fixture = ResourcePackFixture.create(tempDir.resolve("fixture"), true);
        Path firstLayer = zip("bettermodel-one.zip", Map.of(
            "assets/bettermodel/models/nexus.json", "{\"elements\":[]}"
        ));
        Path secondLayer = zip("bettermodel-two.zip", Map.of(
            "assets/bettermodel/models/nexus.json", "{\"elements\":[{}]}"
        ));

        try (StagedPackCandidate first = new StagedPackBuilder(
            fixture.sourceRoot(), tempDir.resolve("packs"),
            List.of(JavaPackLayerProvider.fixed("bettermodel", firstLayer))
        ).build(UUID.randomUUID()); StagedPackCandidate second = new StagedPackBuilder(
            fixture.sourceRoot(), tempDir.resolve("packs"),
            List.of(JavaPackLayerProvider.fixed("bettermodel", secondLayer))
        ).build(UUID.randomUUID())) {
            assertNotEquals(first.javaSha256(), second.javaSha256());
            assertNotEquals(first.manifest().javaUuid(), second.manifest().javaUuid());
        }
    }

    @Test
    void invalidCandidateCleansItsStagingDirectory() throws Exception {
        ResourcePackFixture fixture = ResourcePackFixture.create(tempDir.resolve("fixture"), true);
        Files.writeString(fixture.javaRoot().resolve("assets/bigcasares/models/nexus.json"), "{broken");
        UUID jobId = UUID.randomUUID();
        Path packsRoot = tempDir.resolve("packs");

        assertThrows(IllegalArgumentException.class, () -> new StagedPackBuilder(
            fixture.sourceRoot(), packsRoot, List.of()).build(jobId));

        assertFalse(Files.exists(packsRoot.resolve(".staging").resolve(jobId.toString())));
    }

    @Test
    void repositoryPackPassesTheRuntimeStagingAndArchiveValidators() throws Exception {
        try (StagedPackCandidate candidate = new StagedPackBuilder(
            Path.of("resourcepack"), tempDir.resolve("real-packs"), List.of()
        ).build(UUID.randomUUID())) {
            assertTrue(Files.isRegularFile(candidate.javaPack()));
            assertTrue(Files.isRegularFile(candidate.bedrockPack()));
            assertEquals(64, candidate.javaSha256().length());
        }
    }

    private Path zip(String name, Map<String, String> entries) throws Exception {
        Path output = tempDir.resolve(name);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(output))) {
            for (Map.Entry<String, String> entry : new LinkedHashMap<>(entries).entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return output;
    }
}
