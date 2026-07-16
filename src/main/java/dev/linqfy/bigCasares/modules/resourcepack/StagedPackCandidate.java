package dev.linqfy.bigCasares.modules.resourcepack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

public final class StagedPackCandidate implements AutoCloseable {

    private final UUID jobId;
    private final Path stagingDirectory;
    private final Path javaPack;
    private final Path bedrockPack;
    private final ResourcePackManifest manifest;
    private final String javaSha256;
    private final String bedrockSha256;
    private boolean closed;

    StagedPackCandidate(
        UUID jobId,
        Path stagingDirectory,
        Path javaPack,
        Path bedrockPack,
        ResourcePackManifest manifest,
        String javaSha256,
        String bedrockSha256
    ) {
        this.jobId = Objects.requireNonNull(jobId, "jobId");
        this.stagingDirectory = Objects.requireNonNull(stagingDirectory, "stagingDirectory");
        this.javaPack = Objects.requireNonNull(javaPack, "javaPack");
        this.bedrockPack = Objects.requireNonNull(bedrockPack, "bedrockPack");
        this.manifest = Objects.requireNonNull(manifest, "manifest");
        this.javaSha256 = requireSha(javaSha256, 64, "javaSha256");
        this.bedrockSha256 = requireSha(bedrockSha256, 64, "bedrockSha256");
    }

    public UUID jobId() {
        return jobId;
    }

    public Path stagingDirectory() {
        return stagingDirectory;
    }

    public Path javaPack() {
        return javaPack;
    }

    public Path bedrockPack() {
        return bedrockPack;
    }

    public ResourcePackManifest manifest() {
        return manifest;
    }

    public String javaSha256() {
        return javaSha256;
    }

    public String bedrockSha256() {
        return bedrockSha256;
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        deleteDirectory(stagingDirectory);
    }

    static void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static String requireSha(String value, int length, String name) {
        if (value == null || !value.matches("[0-9a-f]{" + length + "}")) {
            throw new IllegalArgumentException(name + " must be a lowercase digest");
        }
        return value;
    }
}
