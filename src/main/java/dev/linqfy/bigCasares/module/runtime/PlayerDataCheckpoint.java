package dev.linqfy.bigCasares.module.runtime;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

/** Confirms a freshly written player-data receipt even when Bukkit reports save errors only in the log. */
public final class PlayerDataCheckpoint {
    private static final int MAX_PLAYER_DATA = 64 * 1024 * 1024;

    private PlayerDataCheckpoint() {
    }

    public static void save(Player player, NamespacedKey key, String receipt) throws IOException {
        player.getPersistentDataContainer().set(key, PersistentDataType.STRING, receipt);
        player.saveData();
        confirm(player, key, receipt);
    }

    public static void confirm(Player player, NamespacedKey key, String receipt) throws IOException {
        verifyAndForce(playerFile(player), key.toString(), receipt);
    }

    public static boolean contains(Player player, NamespacedKey key, String receipt) throws IOException {
        return contains(playerFile(player), key.toString(), receipt);
    }

    public static void forceFile(Path path) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE)) {
            channel.force(true);
        }
    }

    public static void forceDirectory(Path path) throws IOException {
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows")) {
            return;
        }
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            channel.force(true);
        }
    }

    static void verifyAndForce(Path path, String key, String receipt) throws IOException {
        if (!contains(path, key, receipt)) {
            throw new IOException("The player data file did not contain the expected save receipt");
        }
        forceFile(path);
        forceDirectory(path.getParent());
    }

    private static Path playerFile(Player player) throws IOException {
        return playerFile(player.getServer().getWorlds().getFirst().getWorldFolder().toPath(), player.getUniqueId());
    }

    static Path playerFile(Path worldFolder, UUID player) throws IOException {
        Path root = worldFolder.toAbsolutePath().normalize();
        String fileName = player + ".dat";
        for (int depth = 0; root != null && depth < 4; depth++, root = root.getParent()) {
            Path current = root.resolve("players/data").resolve(fileName);
            if (Files.isRegularFile(current)) return current;
            Path legacy = root.resolve("playerdata").resolve(fileName);
            if (Files.isRegularFile(legacy)) return legacy;
        }
        throw new IOException("The native player data file could not be found");
    }

    static boolean contains(Path path, String key, String receipt) throws IOException {
        // Match the complete named NBT string instead of trusting timestamps or a bare nonce.
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(encoded)) {
            output.writeByte(8);
            output.writeUTF(key);
            output.writeUTF(receipt);
        }
        byte[] pattern = encoded.toByteArray();
        try (InputStream input = new GZIPInputStream(Files.newInputStream(path))) {
            byte[] data = input.readNBytes(MAX_PLAYER_DATA + 1);
            if (data.length > MAX_PLAYER_DATA) {
                throw new IOException("Player data exceeds the receipt verification limit");
            }
            return findSequence(data, pattern);
        }
    }

    private static boolean findSequence(byte[] data, byte[] pattern) {
        for (int index = 0; index <= data.length - pattern.length; index++) {
            if (java.util.Arrays.equals(data, index, index + pattern.length, pattern, 0, pattern.length)) {
                return true;
            }
        }
        return false;
    }
}
