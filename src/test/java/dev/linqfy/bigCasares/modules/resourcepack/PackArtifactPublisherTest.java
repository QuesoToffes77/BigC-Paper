package dev.linqfy.bigCasares.modules.resourcepack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackArtifactPublisherTest {

    @TempDir
    Path tempDir;

    @Test
    void publishesContentAddressedArtifactsAndAtomicallyRoundTripsManifest() throws Exception {
        ResourcePackFixture fixture = ResourcePackFixture.create(tempDir.resolve("fixture"), true);
        Path packsRoot = tempDir.resolve("packs");
        PackArtifactPublisher publisher = new PackArtifactPublisher(
            packsRoot,
            Clock.fixed(Instant.parse("2026-07-15T12:00:00Z"), ZoneOffset.UTC)
        );

        try (StagedPackCandidate candidate = new StagedPackBuilder(
            fixture.sourceRoot(), packsRoot, List.of()).build(UUID.randomUUID())) {
            PackPublication first = publisher.publish(candidate,
                file -> URI.create("http://127.0.0.1:8123/packs/" + file));
            PackPublication second = publisher.publish(candidate,
                file -> URI.create("http://127.0.0.1:8123/packs/" + file));

            assertTrue(first.changed());
            assertFalse(second.changed());
            assertEquals(first.manifest(), second.manifest());
            assertEquals(first.manifest(), publisher.activeManifest().orElseThrow());
            assertTrue(first.manifest().javaFile().contains(candidate.javaSha256().substring(0, 16)));
            assertTrue(Files.isRegularFile(packsRoot.resolve("artifacts").resolve(first.manifest().javaFile())));
            assertTrue(Files.isRegularFile(packsRoot.resolve("artifacts").resolve(first.manifest().bedrockFile())));
        }
    }

    @Test
    void failedCandidateDoesNotReplacePreviouslyActiveManifest() throws Exception {
        ResourcePackFixture fixture = ResourcePackFixture.create(tempDir.resolve("fixture"), true);
        Path packsRoot = tempDir.resolve("packs");
        PackArtifactPublisher publisher = new PackArtifactPublisher(packsRoot);
        ActivePackManifest active;
        try (StagedPackCandidate candidate = new StagedPackBuilder(
            fixture.sourceRoot(), packsRoot, List.of()).build(UUID.randomUUID())) {
            active = publisher.publish(candidate, ignored -> null).manifest();
        }
        Files.writeString(fixture.javaRoot().resolve("pack.mcmeta"), "{broken");

        assertThrows(IllegalArgumentException.class,
            () -> new StagedPackBuilder(fixture.sourceRoot(), packsRoot, List.of()).build(UUID.randomUUID()));

        assertEquals(active, publisher.activeManifest().orElseThrow());
    }

    @Test
    void equalCandidateIsNotANoOpWhenTheActiveArtifactIsMissing() throws Exception {
        ResourcePackFixture fixture = ResourcePackFixture.create(tempDir.resolve("fixture"), true);
        Path packsRoot = tempDir.resolve("packs");
        PackArtifactPublisher publisher = new PackArtifactPublisher(packsRoot);
        try (StagedPackCandidate candidate = new StagedPackBuilder(
            fixture.sourceRoot(), packsRoot, List.of()).build(UUID.randomUUID())) {
            ActivePackManifest active = publisher.publish(candidate, ignored -> null).manifest();
            Files.delete(packsRoot.resolve("artifacts").resolve(active.javaFile()));

            assertThrows(IllegalStateException.class,
                () -> publisher.prepare(candidate, ignored -> null));
        }
    }

    @Test
    void changedRuntimeContentReplacesAStaleActivePack() throws Exception {
        ResourcePackFixture fixture = ResourcePackFixture.create(tempDir.resolve("fixture"), true);
        Path packsRoot = tempDir.resolve("packs");
        PackArtifactPublisher publisher = new PackArtifactPublisher(packsRoot);
        ActivePackManifest oldManifest;
        try (StagedPackCandidate candidate = new StagedPackBuilder(
            fixture.sourceRoot(), packsRoot, List.of()).build(UUID.randomUUID())) {
            oldManifest = publisher.publish(candidate, ignored -> null).manifest();
        }

        Files.writeString(fixture.javaRoot().resolve("pack.mcmeta"), """
            {"pack":{"pack_format":88,"description":"updated runtime content"}}
            """);

        try (StagedPackCandidate updated = new StagedPackBuilder(
            fixture.sourceRoot(), packsRoot, List.of()).build(UUID.randomUUID())) {
            PackPublication publication = publisher.publish(updated, ignored -> null);

            assertTrue(publication.changed());
            assertFalse(oldManifest.javaSha256().equals(publication.manifest().javaSha256()));
            assertEquals(publication.manifest(), publisher.activeManifest().orElseThrow());
        }
    }
}
