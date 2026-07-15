package dev.linqfy.bigCasares.modules.servercontrol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class YamlControlStorageTest {

    @Test
    void roundTripsEveryPersistentControl(@TempDir Path directory) {
        UUID operator = UUID.randomUUID();
        ControlState expected = new ControlState(
            false,
            Optional.of(new TimedPvpOverride(true, Instant.parse("2026-07-15T12:10:00Z"), "Admin")),
            false,
            false,
            ResistanceLevel.II,
            Map.of(operator, false)
        );
        YamlControlStorage storage = new YamlControlStorage(directory.resolve("state.yml"));

        storage.save(expected);

        assertEquals(expected, storage.load());
    }
}
