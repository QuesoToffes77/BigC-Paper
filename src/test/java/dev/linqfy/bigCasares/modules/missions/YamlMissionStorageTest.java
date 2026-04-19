package dev.linqfy.bigCasares.modules.missions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlMissionStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsPlayerState() {
        MissionDefinition definition = new MissionDefinition("tuff-67", MissionScope.DAILY, "Toba exacta", "Consegui 67 bloques de toba", MissionType.HOLD_EXACT_ITEM_COUNT, 67, 100.0, 1, true, Map.of("material", "TUFF"));
        MissionPlayerState state = new MissionPlayerState(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            Instant.parse("2026-04-19T10:00:00Z"),
            Instant.parse("2026-04-20T00:00:00Z"),
            Instant.parse("2026-04-26T00:00:00Z"),
            new LinkedHashMap<>(Map.of("tuff-67", new MissionAssignment(definition, new MissionProgressSnapshot(67, true, true)))),
            Map.of()
        );

        YamlMissionStorage storage = new YamlMissionStorage(tempDir);
        storage.save(state);

        MissionPlayerState loaded = storage.load(state.playerId()).orElseThrow();

        assertEquals(67, loaded.dailyAssignments().get("tuff-67").snapshot().progress());
        assertTrue(loaded.dailyAssignments().get("tuff-67").snapshot().claimed());
    }
}
