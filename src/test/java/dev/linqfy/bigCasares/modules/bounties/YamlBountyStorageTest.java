package dev.linqfy.bigCasares.modules.bounties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YamlBountyStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsActiveBounty() {
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000010");
        BountyPlayerState state = new BountyPlayerState(playerId, 845.5, Instant.parse("2026-04-19T08:34:12Z"));

        YamlBountyStorage storage = new YamlBountyStorage(tempDir);
        storage.save(state);

        BountyPlayerState loaded = storage.load(playerId).orElseThrow();

        assertEquals(845.5, loaded.activeBounty());
        assertEquals(Instant.parse("2026-04-19T08:34:12Z"), loaded.updatedAt());
    }

    @Test
    void returnsEmptyWhenPlayerFileDoesNotExist() {
        YamlBountyStorage storage = new YamlBountyStorage(tempDir);

        Optional<BountyPlayerState> loaded = storage.load(UUID.fromString("00000000-0000-0000-0000-000000000011"));

        assertEquals(Optional.empty(), loaded);
    }

    @Test
    void listsEveryStoredBounty() {
        YamlBountyStorage storage = new YamlBountyStorage(tempDir);
        UUID firstId = UUID.fromString("00000000-0000-0000-0000-000000000021");
        UUID secondId = UUID.fromString("00000000-0000-0000-0000-000000000022");
        storage.save(new BountyPlayerState(firstId, 50.0, Instant.now()));
        storage.save(new BountyPlayerState(secondId, 100.0, Instant.now()));

        assertEquals(2, storage.all().count());
    }
}
