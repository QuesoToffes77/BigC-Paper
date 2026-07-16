package dev.linqfy.bigCasares.modules.resourcepack;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class PackArtifactPublisher {

    private final Path packsRoot;
    private final Clock clock;

    public PackArtifactPublisher(Path packsRoot) {
        this(packsRoot, Clock.systemUTC());
    }

    PackArtifactPublisher(Path packsRoot, Clock clock) {
        this.packsRoot = Objects.requireNonNull(packsRoot, "packsRoot").toAbsolutePath().normalize();
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public PackPublication publish(StagedPackCandidate candidate, Function<String, URI> javaUrlResolver)
        throws IOException {
        PackPublication publication = prepare(candidate, javaUrlResolver);
        activate(publication);
        return publication;
    }

    public PackPublication prepare(StagedPackCandidate candidate, Function<String, URI> javaUrlResolver)
        throws IOException {
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(javaUrlResolver, "javaUrlResolver");
        Optional<ActivePackManifest> current = activeManifest();
        if (current.filter(manifest -> manifest.javaSha256().equals(candidate.javaSha256())).isPresent()) {
            validateActiveArtifacts(current.orElseThrow());
            return new PackPublication(current.orElseThrow(), false);
        }
        Path artifacts = packsRoot.resolve("artifacts");
        Files.createDirectories(artifacts);
        String javaFile = "bigcasares-java-" + candidate.javaSha256().substring(0, 16) + ".zip";
        String bedrockFile = "bigcasares-bedrock-" + candidate.bedrockSha256().substring(0, 16) + ".mcpack";
        publishImmutable(candidate.javaPack(), artifacts.resolve(javaFile), candidate.javaSha256());
        publishImmutable(candidate.bedrockPack(), artifacts.resolve(bedrockFile), candidate.bedrockSha256());
        URI javaUri = javaUrlResolver.apply(javaFile);
        ActivePackManifest active = new ActivePackManifest(
            candidate.manifest().version(),
            candidate.manifest().inputSha256(),
            candidate.manifest().javaSha1(),
            candidate.javaSha256(),
            candidate.manifest().javaUuid(),
            javaFile,
            candidate.bedrockSha256(),
            candidate.manifest().bedrockUuid(),
            bedrockFile,
            javaUri,
            Instant.now(clock)
        );
        return new PackPublication(active, true);
    }

    public void activate(PackPublication publication) throws IOException {
        Objects.requireNonNull(publication, "publication");
        if (publication.changed()) {
            writeActiveManifest(publication.manifest());
        }
    }

    public Optional<ActivePackManifest> activeManifest() throws IOException {
        Path manifest = packsRoot.resolve("active-pack.json");
        return Files.isRegularFile(manifest)
            ? Optional.of(ActivePackManifest.fromJson(Files.readString(manifest)))
            : Optional.empty();
    }

    public void validateActiveArtifacts(ActivePackManifest manifest) throws IOException {
        Objects.requireNonNull(manifest, "manifest");
        Path artifacts = packsRoot.resolve("artifacts");
        validateArtifact(artifacts.resolve(manifest.javaFile()), manifest.javaSha256());
        validateArtifact(artifacts.resolve(manifest.bedrockFile()), manifest.bedrockSha256());
    }

    private void publishImmutable(Path source, Path destination, String expectedSha256) throws IOException {
        if (Files.exists(destination)) {
            String actual = PackFileDigests.digest(destination, "SHA-256");
            if (!actual.equals(expectedSha256)) {
                throw new IllegalStateException("Artifact hash collision at " + destination);
            }
            return;
        }
        Path temporary = Files.createTempFile(destination.getParent(), destination.getFileName().toString(), ".tmp");
        try {
            Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
            if (!PackFileDigests.digest(temporary, "SHA-256").equals(expectedSha256)) {
                throw new IllegalStateException("Published artifact digest changed during copy");
            }
            moveAtomically(temporary, destination);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void validateArtifact(Path artifact, String expectedSha256) throws IOException {
        if (!Files.isRegularFile(artifact)) {
            throw new IllegalStateException("Active resource-pack artifact is missing: " + artifact);
        }
        String actual = PackFileDigests.digest(artifact, "SHA-256");
        if (!actual.equals(expectedSha256)) {
            throw new IllegalStateException("Active resource-pack artifact digest mismatch: " + artifact);
        }
    }

    private void writeActiveManifest(ActivePackManifest manifest) throws IOException {
        Files.createDirectories(packsRoot);
        Path target = packsRoot.resolve("active-pack.json");
        Path temporary = Files.createTempFile(packsRoot, "active-pack-", ".json.tmp");
        try {
            Files.writeString(temporary, manifest.toJson(), StandardCharsets.UTF_8);
            ActivePackManifest.fromJson(Files.readString(temporary));
            moveAtomically(temporary, target);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void moveAtomically(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination,
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
