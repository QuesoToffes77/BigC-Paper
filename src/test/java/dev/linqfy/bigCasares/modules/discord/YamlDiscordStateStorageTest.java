package dev.linqfy.bigCasares.modules.discord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class YamlDiscordStateStorageTest {

    @Test
    void persistsBothManagedMessageIds(@TempDir Path directory) {
        YamlDiscordStateStorage storage = new YamlDiscordStateStorage(directory.resolve("state.yml"));
        DiscordMessageState expected = new DiscordMessageState(123L, 456L);

        storage.save(expected);

        assertEquals(expected, storage.load());
    }
}
