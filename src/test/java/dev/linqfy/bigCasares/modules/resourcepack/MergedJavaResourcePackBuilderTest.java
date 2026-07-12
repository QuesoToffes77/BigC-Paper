package dev.linqfy.bigCasares.modules.resourcepack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MergedJavaResourcePackBuilderTest {

    @TempDir
    Path tempDir;

    @Test
    void retainsAssetsFromBothPacksAndBigCasaresMetadata() throws Exception {
        Path bigCasaresPack = createZip("bigcasares.zip", Map.of(
            "pack.mcmeta", "{\"pack\":{\"description\":\"BigCasares\"}}",
            "pack.png", new byte[]{1, 2, 3},
            "assets/bigcasares/models/nexus.json", "{\"parent\":\"item/generated\"}"
        ));
        Path betterModelPack = createZip("bettermodel.zip", Map.of(
            "pack.mcmeta", "{\"pack\":{\"description\":\"BetterModel\"}}",
            "pack.png", new byte[]{9, 8, 7},
            "assets/bettermodel/models/bigcasares_nexus.json", "{\"elements\":[]}"
        ));

        Path merged = new MergedJavaResourcePackBuilder().merge(
            bigCasaresPack, betterModelPack, tempDir.resolve("merged.zip")
        );

        assertEquals("{\"pack\":{\"description\":\"BigCasares\"}}", readEntry(merged, "pack.mcmeta"));
        assertArrayEquals(new byte[]{1, 2, 3}, readEntryBytes(merged, "pack.png"));
        assertEquals("{\"parent\":\"item/generated\"}",
            readEntry(merged, "assets/bigcasares/models/nexus.json"));
        assertEquals("{\"elements\":[]}",
            readEntry(merged, "assets/bettermodel/models/bigcasares_nexus.json"));
    }

    @Test
    void rejectsConflictingNonMetadataEntries() throws Exception {
        Path first = createZip("first.zip", Map.of("assets/shared/model.json", "first"));
        Path second = createZip("second.zip", Map.of("assets/shared/model.json", "second"));

        assertThrows(IllegalStateException.class, () -> new MergedJavaResourcePackBuilder().merge(
            first, second, tempDir.resolve("merged.zip")
        ));
    }

    @Test
    void writesTheSameBytesForTheSameInputs() throws Exception {
        Path bigCasaresPack = createZip("bigcasares.zip", Map.of(
            "pack.mcmeta", "metadata",
            "assets/bigcasares/models/nexus.json", "nexus"
        ));
        Path betterModelPack = createZip("bettermodel.zip", Map.of(
            "assets/bettermodel/models/nexus.json", "model",
            "assets/bettermodel/textures/nexus.png", new byte[]{4, 5, 6}
        ));

        MergedJavaResourcePackBuilder builder = new MergedJavaResourcePackBuilder();
        Path first = builder.merge(bigCasaresPack, betterModelPack, tempDir.resolve("first.zip"));
        Path second = builder.merge(bigCasaresPack, betterModelPack, tempDir.resolve("second.zip"));

        assertArrayEquals(Files.readAllBytes(first), Files.readAllBytes(second));
    }

    @Test
    void buildMainMergesOnlyTheJavaPackAndUpdatesItsAdvertisedSha() throws Exception {
        ResourcePackFixture fixture = ResourcePackFixture.create(tempDir.resolve("fixture"), true);
        Path betterModelPack = createZip("bettermodel.zip", Map.of(
            "pack.mcmeta", "BetterModel metadata",
            "assets/bettermodel/models/nexus.json", "bettermodel"
        ));

        ResourcePackBuildMain.main(new String[]{
            fixture.sourceRoot().toString(), fixture.outputRoot().toString(),
            "--bettermodel-java-pack=" + betterModelPack
        });

        Path javaPack = fixture.outputRoot().resolve("bigcasares-java.zip");
        assertEquals("bettermodel", readEntry(javaPack, "assets/bettermodel/models/nexus.json"));
        assertFalse(zipContains(fixture.outputRoot().resolve("bigcasares-bedrock.mcpack"),
            "assets/bettermodel/models/nexus.json"));
        ResourcePackManifest manifest = ResourcePackManifest.fromJson(
            Files.readString(fixture.outputRoot().resolve("manifest.json"))
        );
        assertEquals(shaOne(javaPack), manifest.javaSha1());
    }

    private Path createZip(String name, Map<String, ?> entries) throws IOException {
        Path output = tempDir.resolve(name);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(output))) {
            for (Map.Entry<String, ?> entry : new LinkedHashMap<>(entries).entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                Object content = entry.getValue();
                zip.write(content instanceof byte[] bytes ? bytes : content.toString().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return output;
    }

    private static String readEntry(Path zip, String name) throws IOException {
        return new String(readEntryBytes(zip, name), StandardCharsets.UTF_8);
    }

    private static byte[] readEntryBytes(Path zip, String name) throws IOException {
        try (ZipFile archive = new ZipFile(zip.toFile())) {
            try (var input = archive.getInputStream(archive.getEntry(name))) {
                return input.readAllBytes();
            }
        }
    }

    private static boolean zipContains(Path zip, String name) throws IOException {
        try (ZipFile archive = new ZipFile(zip.toFile())) {
            return archive.getEntry(name) != null;
        }
    }

    private static String shaOne(Path file) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(file)));
    }
}
