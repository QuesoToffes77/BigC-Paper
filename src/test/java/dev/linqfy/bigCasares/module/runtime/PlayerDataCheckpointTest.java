package dev.linqfy.bigCasares.module.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDataCheckpointTest {
    @TempDir
    Path directory;

    @Test
    void staleOrUnrelatedReceiptDoesNotConfirmAnInventorySave() throws IOException {
        Path data = directory.resolve("player.dat");
        write(data, "bigcasares:other", "after-42");
        assertFalse(PlayerDataCheckpoint.contains(data, "bigcasares:burial", "after-42"));
        write(data, "bigcasares:burial", "before-42");
        assertThrows(IOException.class, () -> PlayerDataCheckpoint.verifyAndForce(data, "bigcasares:burial", "after-42"));
        write(data, "bigcasares:burial", "after-42");
        assertTrue(PlayerDataCheckpoint.contains(data, "bigcasares:burial", "after-42"));
        PlayerDataCheckpoint.verifyAndForce(data, "bigcasares:burial", "after-42");
    }

    @Test
    void findsTheExistingPlayerFileInPaperAndLegacyWorldLayouts() throws IOException {
        UUID player = UUID.randomUUID();
        Path layout = Files.createDirectories(directory.resolve("dimensions/minecraft/overworld"));
        Files.createDirectories(directory.resolve("players/data"));
        Path legacy = Files.createDirectories(directory.resolve("playerdata")).resolve(player + ".dat");
        Files.createFile(legacy);
        assertEquals(legacy, PlayerDataCheckpoint.playerFile(layout, player));
        Path current = directory.resolve("players/data").resolve(player + ".dat");
        Files.createFile(current);
        assertEquals(current, PlayerDataCheckpoint.playerFile(layout, player));
    }

    private static void write(Path path, String key, String value) throws IOException {
        try (DataOutputStream output = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(path)))) {
            output.writeByte(8);
            output.writeUTF(key);
            output.writeUTF(value);
        }
    }
}
