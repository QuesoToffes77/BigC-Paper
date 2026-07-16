package dev.linqfy.bigCasares.modules.resourcepack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class RuntimePackSourceSeeder {

    public RuntimePackSeedResult seed(InputStream seedArchive, Path destination) throws IOException {
        Objects.requireNonNull(seedArchive, "seedArchive");
        Path root = Objects.requireNonNull(destination, "destination").toAbsolutePath().normalize();
        Files.createDirectories(root);
        int copied = 0;
        int preserved = 0;
        Set<String> entries = new HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(seedArchive)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (!entries.add(name)) {
                    throw new IllegalArgumentException("Duplicate seed entry: " + name);
                }
                Path target = safeTarget(root, name);
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else if (Files.exists(target)) {
                    preserved++;
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zip, target);
                    copied++;
                }
                zip.closeEntry();
            }
        }
        return new RuntimePackSeedResult(copied, preserved);
    }

    private static Path safeTarget(Path root, String entryName) {
        if (entryName.isBlank() || entryName.startsWith("/") || entryName.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Unsafe seed entry: " + entryName);
        }
        Path target = root.resolve(entryName).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Unsafe seed entry: " + entryName);
        }
        return target;
    }
}
