package dev.linqfy.bigCasares.modules.moderation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FileObservationStorageTest {

    @Test
    void retainsRawPrivateValuesAndPurgesExpiredDailyFiles(@TempDir Path directory) throws Exception {
        FileObservationStorage storage = new FileObservationStorage(directory);
        UUID player = UUID.randomUUID();
        storage.append(new Observation(
            Instant.parse("2026-06-01T12:00:00Z"), player, "join", Map.of("ip", "192.0.2.10")
        ));
        Path log = directory.resolve("2026-06-01.log");

        assertTrue(Files.readString(log).contains("192.0.2.10"));

        storage.purgeOlderThan(Instant.parse("2026-07-15T00:00:00Z"));

        assertFalse(Files.exists(log));
    }

    @Test
    void persistsFirstSeenUuidState(@TempDir Path directory) {
        FileObservationStorage storage = new FileObservationStorage(directory);
        UUID player = UUID.randomUUID();

        assertFalse(storage.hasSeen(player));
        storage.markSeen(player);
        assertTrue(new FileObservationStorage(directory).hasSeen(player));
    }
}
