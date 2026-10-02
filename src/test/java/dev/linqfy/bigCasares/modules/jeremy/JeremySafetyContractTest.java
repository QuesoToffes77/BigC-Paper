package dev.linqfy.bigCasares.modules.jeremy;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JeremySafetyContractTest {

    @Test
    void jeremyCreatesNoResourcePackAssetsOrModels() throws Exception {
        try (var paths = Files.walk(Path.of("resourcepack"))) {
            assertFalse(paths.anyMatch(path -> path.getFileName().toString().toLowerCase().contains("jeremy")));
        }
        try (var paths = Files.walk(Path.of("src/main/resources"))) {
            assertFalse(paths.anyMatch(path -> {
                String name = path.getFileName().toString().toLowerCase();
                return name.contains("jeremy") && (name.endsWith(".png") || name.endsWith(".bbmodel") || name.endsWith(".json"));
            }));
        }
    }

    @Test
    void moduleOwnsExactlyOneRepeatingTaskAndOneListener() throws Exception {
        String module = Files.readString(Path.of(
            "src/main/java/dev/linqfy/bigCasares/modules/jeremy/JeremyModule.java"));

        assertEquals(1, occurrences(module, "scheduleRepeating("));
        assertEquals(1, occurrences(module, "registerListener("));
        assertTrue(module.contains("ownCleanup(\"jeremy-runtime\""));
    }

    @Test
    void identityUsesPdcInsteadOfDisplayName() throws Exception {
        String identity = Files.readString(Path.of(
            "src/main/java/dev/linqfy/bigCasares/modules/jeremy/JeremyIdentity.java"));

        assertTrue(identity.contains("PersistentDataType.STRING"));
        assertTrue(identity.contains("custom_entity"));
        assertFalse(identity.contains("getCustomName"));
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}
