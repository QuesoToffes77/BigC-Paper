package dev.linqfy.bigCasares.modules.resourcepack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class ResourcePackBuildMain {
    private ResourcePackBuildMain() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length > 3) {
            throw new IllegalArgumentException(
                "Usage: ResourcePackBuildMain <source-root> <output-directory> [--bettermodel-java-pack=<zip>]"
            );
        }
        Path sourceRoot = Path.of(args[0]);
        Path outputDirectory = Path.of(args[1]).toAbsolutePath().normalize();
        Path betterModelJavaPack = args.length == 3 ? betterModelPack(args[2]) : null;
        if (betterModelJavaPack != null) {
            // A previous merged archive cannot serve as the next merge base.
            Files.deleteIfExists(outputDirectory.resolve("manifest.json"));
        }

        ResourcePackBuildResult result = new ResourcePackBuilder(sourceRoot, outputDirectory).build();
        if (betterModelJavaPack != null) {
            mergeBetterModelJavaPack(outputDirectory, betterModelJavaPack);
        }
        System.out.println((result.skipped() ? "Resource packs unchanged: " : "Resource packs generated: ")
            + result.manifest().version());
    }

    private static Path betterModelPack(String argument) {
        String prefix = "--bettermodel-java-pack=";
        if (!argument.startsWith(prefix) || argument.length() == prefix.length()) {
            throw new IllegalArgumentException("Expected " + prefix + "<zip>");
        }
        return Path.of(argument.substring(prefix.length()));
    }

    private static void mergeBetterModelJavaPack(Path outputDirectory, Path betterModelJavaPack) throws IOException {
        Path javaPack = outputDirectory.resolve("bigcasares-java.zip");
        Path mergedPack = Files.createTempFile(outputDirectory, "bigcasares-java-merged-", ".zip");
        try {
            new MergedJavaResourcePackBuilder().merge(javaPack, betterModelJavaPack, mergedPack);
            Files.move(mergedPack, javaPack, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(mergedPack);
        }
        updateChecksums(outputDirectory, javaPack);
    }

    private static void updateChecksums(Path outputDirectory, Path javaPack) throws IOException {
        Path manifestPath = outputDirectory.resolve("manifest.json");
        ResourcePackManifest previous = ResourcePackManifest.fromJson(Files.readString(manifestPath));
        String javaSha = digest(javaPack, "SHA-1");
        ResourcePackManifest updated = new ResourcePackManifest(
            previous.version(), previous.inputSha256(), javaSha, previous.bedrockUuid()
        );
        Files.writeString(manifestPath, updated.toJson(), StandardCharsets.UTF_8);

        Path bedrockPack = outputDirectory.resolve("bigcasares-bedrock.mcpack");
        Files.writeString(outputDirectory.resolve("checksums.yml"), """
            version: %s
            input-sha256: %s
            java:
              file: bigcasares-java.zip
              sha1: %s
            bedrock:
              file: bigcasares-bedrock.mcpack
              sha256: %s
              uuid: %s
            """.formatted(
                updated.version(),
                updated.inputSha256(),
                updated.javaSha1(),
                digest(bedrockPack, "SHA-256"),
                updated.bedrockUuid()
            ), StandardCharsets.UTF_8);
    }

    private static String digest(Path file, String algorithm) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Required digest algorithm is unavailable: " + algorithm, ex);
        }
    }
}
