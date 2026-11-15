package dev.linqfy.bigCasares.modules.mythicdungeons.boss.pachamama;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Every block the fight changes (painted floor, water of the blue rounds, the pedestal) with its original data, so
 * it can be put back: per round ({@link #restore(Collection)}) and all at once in {@code stop()}
 * ({@link #restoreAll()}, which also runs on plugin disable). The journal is mirrored to
 * {@code plugins/BigCasares/mythicdungeons/pachamama-blocks-<boss>.txt} before the blocks change, so after a crash
 * {@link #recoverOrphans} (at module enable through {@link PachamamaEncounter#recoverOrphans}, and at the start of
 * every Pachamama fight) puts back whatever is still one of our blocks. Blocks are changed without physics, so
 * water never spreads; removing water with physics dries any that leaked.
 */
final class PachamamaFloor {

    private static final Set<UUID> LIVE = ConcurrentHashMap.newKeySet();
    static final String FOLDER = "mythicdungeons";
    private static final String PREFIX = "pachamama-blocks-";

    /** What we placed on a block and what was there before. */
    private record Change(Material placed, BlockData original) {
    }

    private final JavaPlugin plugin;
    private final UUID bossId;
    private final Map<Block, Change> journal = new LinkedHashMap<>();
    private boolean closed;

    PachamamaFloor(JavaPlugin plugin, UUID bossId) {
        this.plugin = plugin;
        this.bossId = bossId;
        LIVE.add(bossId);
    }

    /**
     * Changes a batch of blocks: the journal file is written first, then the blocks change (without physics). A
     * block already changed keeps its first original data.
     */
    void change(Map<Block, BlockData> batch) {
        if (closed || batch.isEmpty()) {
            return;
        }
        for (Map.Entry<Block, BlockData> entry : batch.entrySet()) {
            Block block = entry.getKey();
            Change previous = journal.get(block);
            BlockData original = previous != null ? previous.original() : block.getBlockData();
            journal.put(block, new Change(entry.getValue().getMaterial(), original));
        }
        persist();
        for (Map.Entry<Block, BlockData> entry : batch.entrySet()) {
            entry.getKey().setBlockData(entry.getValue(), false);
        }
    }

    boolean changed(Block block) {
        return journal.containsKey(block);
    }

    /** Puts back the given blocks (newest first) and forgets them. */
    void restore(Collection<Block> blocks) {
        List<Block> order = new ArrayList<>(blocks);
        for (int index = order.size() - 1; index >= 0; index--) {
            Block block = order.get(index);
            Change change = journal.remove(block);
            if (change != null) {
                put(block, change);
            }
        }
        persist();
    }

    /** Puts back every journaled block and deletes the journal file; idempotent. */
    void restoreAll() {
        if (closed) {
            return;
        }
        closed = true;
        List<Map.Entry<Block, Change>> entries = new ArrayList<>(journal.entrySet());
        for (int index = entries.size() - 1; index >= 0; index--) {
            put(entries.get(index).getKey(), entries.get(index).getValue());
        }
        journal.clear();
        try {
            Files.deleteIfExists(file(plugin, bossId));
        } catch (IOException failure) {
            plugin.getLogger().log(Level.WARNING, "[Mazmorras] Pachamama: no se pudo borrar el diario de bloques",
                failure);
        }
        LIVE.remove(bossId);
    }

    private static void put(Block block, Change change) {
        // water goes back with physics so any flow it caused dries up; the rest without (no neighbour updates)
        block.setBlockData(change.original(), change.placed() == Material.WATER);
    }

    private void persist() {
        Path path = file(plugin, bossId);
        try {
            if (journal.isEmpty()) {
                Files.deleteIfExists(path);
                return;
            }
            StringBuilder text = new StringBuilder(journal.size() * 48);
            for (Map.Entry<Block, Change> entry : journal.entrySet()) {
                Block block = entry.getKey();
                text.append(block.getWorld().getName()).append(' ').append(block.getX()).append(' ')
                    .append(block.getY()).append(' ').append(block.getZ()).append(' ')
                    .append(entry.getValue().placed().name()).append(' ')
                    .append(entry.getValue().original().getAsString()).append('\n');
            }
            Files.createDirectories(path.getParent());
            Files.writeString(path, text, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            plugin.getLogger().log(Level.WARNING, "[Mazmorras] Pachamama: no se pudo escribir el diario de bloques",
                failure);
        }
    }

    private static Path file(JavaPlugin plugin, UUID bossId) {
        return plugin.getDataFolder().toPath().resolve(FOLDER).resolve(PREFIX + bossId + ".txt");
    }

    /**
     * Puts back the blocks of fights that did not end cleanly (a crash): only journals of a Pachamama that is not
     * alive in this run, and only blocks that are still what the fight placed. A journal naming an unloaded world is
     * kept for later.
     */
    static void recoverOrphans(JavaPlugin plugin) {
        Path dir = plugin.getDataFolder().toPath().resolve(FOLDER);
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, PREFIX + "*.txt")) {
            for (Path path : files) {
                String name = path.getFileName().toString();
                String id = name.substring(PREFIX.length(), name.length() - ".txt".length()).toLowerCase(Locale.ROOT);
                if (LIVE.stream().anyMatch(live -> live.toString().equals(id))) {
                    continue;
                }
                if (restoreFile(plugin, path)) {
                    Files.deleteIfExists(path);
                }
            }
        } catch (IOException failure) {
            plugin.getLogger().log(Level.WARNING, "[Mazmorras] Pachamama: no se pudieron revisar peleas viejas",
                failure);
        }
    }

    private static boolean restoreFile(JavaPlugin plugin, Path path) throws IOException {
        int restored = 0;
        boolean complete = true;
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String[] parts = line.trim().split(" ", 6);
            if (parts.length != 6) {
                continue;
            }
            World world = plugin.getServer().getWorld(parts[0]);
            if (world == null) {
                complete = false;
                continue;
            }
            try {
                Block block = world.getBlockAt(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]));
                Material placed = Material.matchMaterial(parts[4]);
                if (placed != null && block.getType() == placed) {
                    block.setBlockData(plugin.getServer().createBlockData(parts[5]), placed == Material.WATER);
                    restored++;
                }
            } catch (IllegalArgumentException ignored) {
                // a damaged line or an unknown block: skip it
            }
        }
        if (restored > 0) {
            plugin.getLogger().info("[Mazmorras] Pachamama: se restauraron " + restored
                + " bloques de una pelea vieja.");
        }
        return complete;
    }
}
