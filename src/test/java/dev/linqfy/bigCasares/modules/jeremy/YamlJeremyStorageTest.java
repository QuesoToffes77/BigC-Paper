package dev.linqfy.bigCasares.modules.jeremy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YamlJeremyStorageTest {

    @Test
    void restartKeepsRealTimePhaseDeadline(@TempDir Path directory) throws Exception {
        YamlJeremyStorage storage = new YamlJeremyStorage(directory.resolve("jeremy-state.yml"));
        UUID target = UUID.randomUUID();
        JeremySnapshot snapshot = new JeremySnapshot(JeremyPhase.HUNTING, target, target, 9_999_999L);

        storage.save(snapshot);

        assertEquals(snapshot, storage.load().orElseThrow());
    }

    @Test
    void absentStateStartsEmpty(@TempDir Path directory) throws Exception {
        YamlJeremyStorage storage = new YamlJeremyStorage(directory.resolve("missing.yml"));

        assertEquals(java.util.Optional.empty(), storage.load());
    }
}
