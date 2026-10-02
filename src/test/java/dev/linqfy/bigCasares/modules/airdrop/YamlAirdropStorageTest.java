package dev.linqfy.bigCasares.modules.airdrop;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlAirdropStorageTest {

    @TempDir
    Path tempDir;

    private YamlAirdropStorage storage;

    @BeforeEach
    void setUp() {
        storage = new YamlAirdropStorage(tempDir, Logger.getLogger("test"));
    }

    @Test
    void saveThenLoadReturnsEqualData() {
        AirdropPosition pos = new AirdropPosition(100, 94, -200);
        AirdropData data = new AirdropData(pos, AirdropType.LUXURY, AirdropPhase.FALLING);

        storage.save(data);
        Optional<AirdropData> loaded = storage.load();

        assertTrue(loaded.isPresent());
        assertEquals(data, loaded.get());
    }

    @Test
    void loadReturnsEmptyWhenNoFileExists() {
        Optional<AirdropData> result = storage.load();
        assertTrue(result.isEmpty());
    }

    @Test
    void clearRemovesPersistedData() {
        AirdropData data = new AirdropData(new AirdropPosition(0, 64, 0), AirdropType.HE, AirdropPhase.LANDED);
        storage.save(data);
        storage.clear();
        assertTrue(storage.load().isEmpty());
    }

    @Test
    void saveThenLoadPreservesPhase() {
        AirdropData falling = new AirdropData(new AirdropPosition(5, 70, 5), AirdropType.ENCHANT, AirdropPhase.FALLING);
        storage.save(falling);

        AirdropData claimed = falling.withPhase(AirdropPhase.CLAIMED);
        storage.save(claimed);

        Optional<AirdropData> loaded = storage.load();
        assertTrue(loaded.isPresent());
        assertEquals(AirdropPhase.CLAIMED, loaded.get().phase());
    }

    @Test
    void saveThenLoadPreservesAllAirdropTypes() {
        for (AirdropType type : AirdropType.values()) {
            AirdropData data = new AirdropData(new AirdropPosition(10, 65, 10), type, AirdropPhase.CLAIMED);
            storage.save(data);
            Optional<AirdropData> loaded = storage.load();
            assertTrue(loaded.isPresent());
            assertEquals(type, loaded.get().type());
        }
    }

    @Test
    void saveThenLoadNextDropAtReturnsIt() {
        storage.saveNextDropAtMillis(1_800_000L);

        assertEquals(1_800_000L, storage.loadNextDropAtMillis().orElse(-1));
    }

    @Test
    void loadNextDropAtIsEmptyWhenNothingPersisted() {
        assertTrue(storage.loadNextDropAtMillis().isEmpty());
    }

    @Test
    void saveDropPreservesNextDropDeadline() {
        storage.saveNextDropAtMillis(1_800_000L);

        AirdropData data = new AirdropData(new AirdropPosition(10, 65, 10), AirdropType.HE, AirdropPhase.LANDED);
        storage.save(data);

        assertEquals(1_800_000L, storage.loadNextDropAtMillis().orElse(-1));
    }

    @Test
    void saveNextDropPreservesDropData() {
        AirdropData data = new AirdropData(new AirdropPosition(10, 65, 10), AirdropType.HE, AirdropPhase.LANDED);
        storage.save(data);
        storage.saveNextDropAtMillis(1_800_000L);

        assertEquals(data, storage.load().orElseThrow());
    }

    @Test
    void clearRemovesPersistedDeadline() {
        storage.saveNextDropAtMillis(1_800_000L);
        storage.clear();

        assertTrue(storage.loadNextDropAtMillis().isEmpty());
    }

    @Test
    void saveThenLoadDespawnAtReturnsIt() {
        storage.saveDespawnAtMillis(2_700_000L);

        assertEquals(2_700_000L, storage.loadDespawnAtMillis().orElse(-1));
    }

    @Test
    void loadDespawnAtIsEmptyWhenNothingPersisted() {
        assertTrue(storage.loadDespawnAtMillis().isEmpty());
    }

    @Test
    void saveThenLoadUnlockAtReturnsIt() {
        storage.saveUnlockAtMillis(300_000L);

        assertEquals(300_000L, storage.loadUnlockAtMillis().orElse(-1));
    }

    @Test
    void loadUnlockAtIsEmptyWhenNothingPersisted() {
        assertTrue(storage.loadUnlockAtMillis().isEmpty());
    }

    @Test
    void saveDropAndDeadlinesCoexistInTheSameFile() {
        storage.saveNextDropAtMillis(1_800_000L);
        storage.saveUnlockAtMillis(300_000L);
        storage.saveDespawnAtMillis(2_700_000L);

        AirdropData data = new AirdropData(new AirdropPosition(10, 65, 10), AirdropType.HE, AirdropPhase.LANDED);
        storage.save(data);

        assertEquals(data, storage.load().orElseThrow());
        assertEquals(1_800_000L, storage.loadNextDropAtMillis().orElse(-1));
        assertEquals(300_000L, storage.loadUnlockAtMillis().orElse(-1));
        assertEquals(2_700_000L, storage.loadDespawnAtMillis().orElse(-1));
    }

    @Test
    void clearRemovesPersistedDespawnDeadline() {
        storage.saveDespawnAtMillis(2_700_000L);
        storage.clear();

        assertTrue(storage.loadDespawnAtMillis().isEmpty());
    }

    @Test
    void loadMigratesLegacyActiveStateToPhase() throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("position.x", 1);
        yaml.set("position.y", 80);
        yaml.set("position.z", 2);
        yaml.set("type", AirdropType.HE.name());
        yaml.set("active", true);

        Files.createDirectories(tempDir);
        yaml.save(tempDir.resolve("airdrop.yml").toFile());

        Optional<AirdropData> loaded = storage.load();
        assertTrue(loaded.isPresent());
        assertEquals(AirdropPhase.LANDED, loaded.get().phase());
        assertEquals(AirdropQuality.COMMON, loaded.get().quality());
    }

    @Test
    void saveThenLoadPreservesQualityIdentityAndResolvedLoot() {
        AirdropReward reward = AirdropReward.book(AirdropQuality.LEGENDARY, List.of(
            new AirdropStoredEnchantment("mending", 1, 1, true, false)));
        AirdropData data = new AirdropData(
            UUID.randomUUID(), new AirdropPosition(12, 80, -15), AirdropType.ENCHANT,
            AirdropQuality.LEGENDARY, AirdropPhase.LANDED, true, List.of(reward));

        storage.save(data);

        assertEquals(data, storage.load().orElseThrow());
    }
}
