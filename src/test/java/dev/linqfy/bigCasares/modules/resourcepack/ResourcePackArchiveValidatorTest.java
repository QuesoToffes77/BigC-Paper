package dev.linqfy.bigCasares.modules.resourcepack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourcePackArchiveValidatorTest {

    @TempDir
    Path tempDir;

    @Test
    void acceptsValidJsonAndReadablePng() throws Exception {
        Path archive = zip("valid.zip", Map.of(
            "pack.mcmeta", "{\"pack\":{\"pack_format\":88,\"description\":\"test\"}}".getBytes(StandardCharsets.UTF_8),
            "assets/bigcasares/items/example.json", "{\"model\":\"bigcasares:item/example\"}".getBytes(StandardCharsets.UTF_8),
            "assets/bigcasares/textures/item/example.png", png()
        ));

        assertDoesNotThrow(() -> new ResourcePackArchiveValidator().validateJava(archive));
    }

    @Test
    void rejectsInvalidJsonAndUnreadablePng() throws Exception {
        Path invalidJson = zip("invalid-json.zip", Map.of(
            "pack.mcmeta", "{broken".getBytes(StandardCharsets.UTF_8)
        ));
        Path invalidPng = zip("invalid-png.zip", Map.of(
            "pack.mcmeta", "{\"pack\":{}}".getBytes(StandardCharsets.UTF_8),
            "assets/bigcasares/textures/item/example.png", new byte[]{1, 2, 3}
        ));

        assertThrows(IllegalArgumentException.class,
            () -> new ResourcePackArchiveValidator().validateJava(invalidJson));
        assertThrows(IllegalArgumentException.class,
            () -> new ResourcePackArchiveValidator().validateJava(invalidPng));
    }

    @Test
    void rejectsInvalidExponentSignAndNonJsonWhitespace() throws Exception {
        Path invalidExponent = zip("invalid-exponent.zip", Map.of(
            "pack.mcmeta", "{\"value\":1e+-2}".getBytes(StandardCharsets.UTF_8)
        ));
        Path invalidWhitespace = zip("invalid-whitespace.zip", Map.of(
            "pack.mcmeta", "{\u00a0\"pack\":{}}".getBytes(StandardCharsets.UTF_8)
        ));

        assertThrows(IllegalArgumentException.class,
            () -> new ResourcePackArchiveValidator().validateJava(invalidExponent));
        assertThrows(IllegalArgumentException.class,
            () -> new ResourcePackArchiveValidator().validateJava(invalidWhitespace));
    }

    @Test
    void rejectsUnsafeArchivePaths() throws Exception {
        Path archive = zip("unsafe.zip", Map.of(
            "pack.mcmeta", "{\"pack\":{}}".getBytes(StandardCharsets.UTF_8),
            "../outside.json", "{}".getBytes(StandardCharsets.UTF_8)
        ));

        assertThrows(IllegalArgumentException.class,
            () -> new ResourcePackArchiveValidator().validateJava(archive));
    }

    private Path zip(String name, Map<String, byte[]> entries) throws Exception {
        Path archive = tempDir.resolve(name);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (Map.Entry<String, byte[]> entry : new LinkedHashMap<>(entries).entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return archive;
    }

    private static byte[] png() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return bytes.toByteArray();
    }
}
