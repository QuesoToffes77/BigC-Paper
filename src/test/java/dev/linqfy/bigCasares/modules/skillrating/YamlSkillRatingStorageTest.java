package dev.linqfy.bigCasares.modules.skillrating;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YamlSkillRatingStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsSkillRatingState() {
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000010");
        SkillRatingState state = new SkillRatingState(playerId, 29.5, 7.5, 7.0, 2, Instant.parse("2026-04-24T08:34:12Z"));

        YamlSkillRatingStorage storage = new YamlSkillRatingStorage(tempDir);
        storage.save(state);

        SkillRatingState loaded = storage.load(playerId).orElseThrow();

        assertEquals(state, loaded);
    }

    @Test
    void returnsEmptyWhenPlayerFileDoesNotExist() {
        YamlSkillRatingStorage storage = new YamlSkillRatingStorage(tempDir);

        Optional<SkillRatingState> loaded = storage.load(UUID.fromString("00000000-0000-0000-0000-000000000011"));

        assertEquals(Optional.empty(), loaded);
    }
}
