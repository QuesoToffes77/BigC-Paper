package dev.linqfy.bigCasares.modules.resourcepack;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class DeterministicZipWriter {
    private DeterministicZipWriter() {
    }

    static void write(Path sourceRoot, Path destination) throws IOException {
        Files.createDirectories(destination.getParent());
        List<Path> files;
        try (var stream = Files.walk(sourceRoot)) {
            files = stream.filter(Files::isRegularFile)
                .sorted(Comparator.comparing(path -> entryName(sourceRoot, path)))
                .toList();
        }

        try (ZipOutputStream zip = new ZipOutputStream(
            new BufferedOutputStream(Files.newOutputStream(destination)))) {
            zip.setLevel(Deflater.BEST_COMPRESSION);
            for (Path file : files) {
                ZipEntry entry = new ZipEntry(entryName(sourceRoot, file));
                entry.setTime(0L);
                zip.putNextEntry(entry);
                Files.copy(file, zip);
                zip.closeEntry();
            }
        }
    }

    private static String entryName(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }
}
