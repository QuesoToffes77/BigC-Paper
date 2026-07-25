package dev.linqfy.bigCasares.modules.endevent;

import org.bukkit.GameMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YamlEndEventStorageTest {

    @TempDir
    Path temp;

    @Test
    void roundTripsDomainAndRuntimeRecoveryState() {
        UUID participant = UUID.randomUUID();
        UUID winner = UUID.randomUUID();
        YamlEndEventStorage storage = new YamlEndEventStorage(temp.resolve("state.yml"));
        EndEventSnapshot domain = new EndEventSnapshot(
            EndEventPhase.HUNT,
            Optional.of(Instant.parse("2026-07-26T01:30:00Z")),
            Optional.of(participant),
            Map.of(participant, new EndEventParticipantSnapshot(
                false, false, 2, Optional.of(Instant.parse("2026-07-26T01:31:00Z"))
            )),
            Set.of(EndEventMilestone.POTIONS_LOCKED)
        );
        EndEventRuntimeState runtime = new EndEventRuntimeState(
            true,
            Optional.of(new EndEventLocation("world", 12.5, 44.0, -9.5)),
            Optional.of(new EndEventLocation("world_the_end", 0.0, 65.0, 0.0)),
            Set.of(participant),
            Optional.of(new EndEventBorderSnapshot("world", 0.5, -0.5, 6000.0)),
            Map.of(participant, GameMode.SURVIVAL.name()),
            Map.of(participant, List.of()),
            Optional.of(winner),
            Optional.of(Instant.parse("2026-07-26T02:00:10Z"))
        );

        storage.save(domain);
        storage.saveRuntime(runtime);

        YamlEndEventStorage restored = new YamlEndEventStorage(temp.resolve("state.yml"));
        assertEquals(Optional.of(domain), restored.load());
        assertEquals(runtime, restored.loadRuntime());
    }
}
