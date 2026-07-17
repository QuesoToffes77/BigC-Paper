package dev.linqfy.bigCasares.modules.resourcepack;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ResourcePackArchiveValidator {

    public void validateJava(Path archive) throws IOException {
        validate(archive, "pack.mcmeta");
    }

    public void validateBedrock(Path archive) throws IOException {
        validate(archive, "manifest.json");
    }

    private void validate(Path archive, String requiredMetadata) throws IOException {
        Set<String> names = new HashSet<>();
        boolean metadataFound = false;
        JsonSyntaxValidator jsonValidator = new JsonSyntaxValidator();
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = normalize(entry.getName());
                if (!names.add(name)) {
                    throw new IllegalArgumentException("Duplicate resource-pack entry: " + name);
                }
                if (entry.isDirectory()) {
                    continue;
                }
                metadataFound |= requiredMetadata.equals(name);
                byte[] content;
                try (var input = zip.getInputStream(entry)) {
                    content = input.readAllBytes();
                }
                if (name.endsWith(".json") || name.endsWith(".mcmeta")) {
                    jsonValidator.validate(new String(content, StandardCharsets.UTF_8), name);
                } else if (name.endsWith(".png") && ImageIO.read(new ByteArrayInputStream(content)) == null) {
                    throw new IllegalArgumentException("Unreadable PNG resource-pack entry: " + name);
                }
            }
        }
        if (!metadataFound) {
            throw new IllegalArgumentException("Resource-pack archive is missing " + requiredMetadata);
        }
    }

    private static String normalize(String name) {
        String normalized = name.replace('\\', '/');
        if (normalized.isBlank() || normalized.startsWith("/") || normalized.contains("//")) {
            throw new IllegalArgumentException("Unsafe resource-pack entry: " + name);
        }
        Path path = Path.of(normalized).normalize();
        String result = path.toString().replace('\\', '/');
        if (result.startsWith("../") || result.equals("..") || !result.equals(normalized)) {
            throw new IllegalArgumentException("Unsafe resource-pack entry: " + name);
        }
        return result;
    }
}
