package dev.linqfy.bigCasares.modules.resourcepack;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Combines BetterModel's generated Java assets with the generated BigCasares pack.
 * BigCasares remains the owner of root pack metadata and icon files.
 */
public final class MergedJavaResourcePackBuilder {
    private static final Set<String> BIGCASARES_METADATA = Set.of("pack.mcmeta", "pack.png");

    public Path merge(Path bigCasaresPack, Path betterModelPack, Path output) throws IOException {
        Path bigCasares = requireZip(bigCasaresPack, "BigCasares pack");
        Path betterModel = requireZip(betterModelPack, "BetterModel pack");
        Path destination = output.toAbsolutePath().normalize();
        if (destination.equals(bigCasares) || destination.equals(betterModel)) {
            throw new IllegalArgumentException("Merged resource pack output must differ from its inputs");
        }

        Map<String, SourceEntry> entries = new LinkedHashMap<>();
        collect(entries, bigCasares, true);
        collect(entries, betterModel, false);

        Path parent = destination.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (ZipOutputStream zip = new ZipOutputStream(
            new BufferedOutputStream(Files.newOutputStream(destination)))) {
            zip.setLevel(Deflater.BEST_COMPRESSION);
            for (Map.Entry<String, SourceEntry> entry : entries.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .toList()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zipEntry.setTime(0L);
                zip.putNextEntry(zipEntry);
                try (ZipFile source = new ZipFile(entry.getValue().archive().toFile());
                     InputStream input = source.getInputStream(source.getEntry(entry.getValue().name()))) {
                    input.transferTo(zip);
                }
                zip.closeEntry();
            }
        }
        return destination;
    }

    private static Path requireZip(Path pack, String label) {
        Path normalized = pack.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalized)) {
            throw new IllegalArgumentException(label + " does not exist: " + normalized);
        }
        return normalized;
    }

    private static void collect(Map<String, SourceEntry> target, Path archive, boolean bigCasares) throws IOException {
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            var enumeration = zip.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry entry = enumeration.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entryName(entry);
                if (!bigCasares && BIGCASARES_METADATA.contains(name)) {
                    continue;
                }
                SourceEntry existing = target.putIfAbsent(name, new SourceEntry(archive, name));
                if (existing != null) {
                    throw new IllegalStateException("Conflicting Java resource-pack entry: " + name);
                }
            }
        }
    }

    private static String entryName(ZipEntry entry) {
        String name = entry.getName();
        if (name.isBlank() || name.startsWith("/") || name.contains("\\")
            || name.equals("..") || name.startsWith("../") || name.contains("/../")) {
            throw new IllegalArgumentException("Unsafe resource-pack entry: " + name);
        }
        return name;
    }

    private record SourceEntry(Path archive, String name) {
    }
}
