package dev.linqfy.bigCasares.modules.resourcepack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimePackSourceSeederTest {

    @TempDir
    Path tempDir;

    @Test
    void extractsMissingFilesWithoutOverwritingOperatorEdits() throws Exception {
        Path destination = tempDir.resolve("content/pack");
        Files.createDirectories(destination.resolve("java"));
        Files.writeString(destination.resolve("java/pack.mcmeta"), "operator edit");
        byte[] seed = zip(Map.of(
            "java/pack.mcmeta", "default",
            "shared/registry.yml", "assets: {}"
        ));

        RuntimePackSeedResult first = new RuntimePackSourceSeeder().seed(
            new ByteArrayInputStream(seed), destination);
        RuntimePackSeedResult second = new RuntimePackSourceSeeder().seed(
            new ByteArrayInputStream(seed), destination);

        assertEquals("operator edit", Files.readString(destination.resolve("java/pack.mcmeta")));
        assertEquals("assets: {}", Files.readString(destination.resolve("shared/registry.yml")));
        assertEquals(1, first.copiedFiles());
        assertEquals(1, first.preservedFiles());
        assertEquals(0, second.copiedFiles());
        assertEquals(2, second.preservedFiles());
    }

    @Test
    void rejectsTraversalWithoutWritingOutsideDestination() throws Exception {
        byte[] seed = zip(Map.of("../escape.txt", "bad"));

        assertThrows(IllegalArgumentException.class, () -> new RuntimePackSourceSeeder().seed(
            new ByteArrayInputStream(seed), tempDir.resolve("content/pack")));

        assertFalse(Files.exists(tempDir.resolve("escape.txt")));
    }

    @Test
    void rejectsDuplicateSeedEntries() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("java\\pack.mcmeta"));
            zip.write("one".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("java/pack.mcmeta"));
            zip.write("two".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        assertThrows(IllegalArgumentException.class, () -> new RuntimePackSourceSeeder().seed(
            new ByteArrayInputStream(bytes.toByteArray()), tempDir.resolve("content/pack")));
    }

    private static byte[] zip(Map<String, String> entries) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, String> entry : new LinkedHashMap<>(entries).entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }
}
