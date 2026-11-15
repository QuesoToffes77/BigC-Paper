package dev.linqfy.bigCasares.modules.mythicdungeons.boss.enlil;

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
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * The phase-2 flood: real water source blocks, layer by layer, only on air cells of a watertight layer of the room
 * (see {@link EnlilFloodPlanner}). Every changed block is journaled in memory with its original data and restored in
 * {@link #restore()} (called from {@code stop()}, which also runs on plugin disable). The planned cells are also
 * written to {@code plugins/BigCasares/mythicdungeons/enlil-flood-<boss>.txt} before any is placed, so after a crash
 * the module (at enable) and every new Enlil fight remove the leftover sources ({@link #recoverOrphans}). Sources are placed without physics, so
 * they never spread; removing them with physics lets any water that leaked through a broken wall dry up.
 */
final class EnlilFlood {

    private static final Set<UUID> LIVE = ConcurrentHashMap.newKeySet();
    /** Same folder as the datapack backups of the module: {@code plugins/BigCasares/mythicdungeons/}. */
    static final String FOLDER = "mythicdungeons";
    private static final String PREFIX = "enlil-flood-";
    private static final int PLACED_PER_TICK = 40;

    private final JavaPlugin plugin;
    private final UUID bossId;
    private final World world;
    private final EnlilArena arena;
    private final int startX;
    private final int startZ;
    private final Map<Block, BlockData> journal = new LinkedHashMap<>();
    private final Deque<Block> pending = new ArrayDeque<>();
    private int layersPlanned;
    private boolean leaked;
    private boolean restored;

    EnlilFlood(JavaPlugin plugin, UUID bossId, World world, EnlilArena arena, int startX, int startZ) {
        this.plugin = plugin;
        this.bossId = bossId;
        this.world = world;
        this.arena = arena;
        this.startX = startX;
        this.startZ = startZ;
        LIVE.add(bossId);
    }

    /** Plans the layers up to {@code layers}; returns {@code true} when a new layer started to fill. */
    boolean raiseTo(int layers) {
        boolean started = false;
        while (!restored && !leaked && layersPlanned < Math.min(layers, EnlilTuning.FLOOD_LAYERS)) {
            int y = arena.floorY() + layersPlanned;
            Optional<List<EnlilFloodPlanner.Cell>> cells = EnlilFloodPlanner.plan(arena, startX, startZ,
                (x, z) -> world.getBlockAt(x, y, z).getType().isAir());
            layersPlanned++;
            if (cells.isEmpty()) {
                leaked = true;
                plugin.getLogger().warning("[Mazmorras] Enlil: la sala no es estanca en y=" + y
                    + "; no se inunda (" + world.getName() + " " + startX + "," + startZ + ").");
                return started;
            }
            List<Block> blocks = new ArrayList<>(cells.get().size());
            for (EnlilFloodPlanner.Cell cell : cells.get()) {
                blocks.add(world.getBlockAt(cell.x(), y, cell.z()));
            }
            persist(blocks);
            pending.addAll(blocks);
            started = true;
        }
        return started;
    }

    /** Places the next batch of the layer being filled (the water pours in from the walls). */
    void tick() {
        for (int placed = 0; placed < PLACED_PER_TICK && !pending.isEmpty(); placed++) {
            Block block = pending.poll();
            if (!block.getType().isAir() || journal.containsKey(block)) {
                continue;
            }
            journal.put(block, block.getBlockData());
            block.setType(Material.WATER, false);
        }
    }

    boolean leaked() {
        return leaked;
    }

    int layers() {
        return layersPlanned;
    }

    /** Puts back every journaled block that is still our water; idempotent. */
    void restore() {
        if (restored) {
            return;
        }
        restored = true;
        pending.clear();
        for (Map.Entry<Block, BlockData> entry : journal.entrySet()) {
            Block block = entry.getKey();
            if (block.getType() == Material.WATER) {
                block.setBlockData(entry.getValue(), true);
            }
        }
        journal.clear();
        try {
            Files.deleteIfExists(file(plugin, bossId));
        } catch (IOException failure) {
            plugin.getLogger().log(Level.WARNING, "[Mazmorras] Enlil: no se pudo borrar el diario de la inundacion",
                failure);
        }
        LIVE.remove(bossId);
    }

    private void persist(List<Block> blocks) {
        Path path = file(plugin, bossId);
        StringBuilder text = new StringBuilder(blocks.size() * 24);
        for (Block block : blocks) {
            text.append(world.getName()).append(' ').append(block.getX()).append(' ').append(block.getY())
                .append(' ').append(block.getZ()).append('\n');
        }
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, text, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException failure) {
            plugin.getLogger().log(Level.WARNING, "[Mazmorras] Enlil: no se pudo escribir el diario de la inundacion",
                failure);
        }
    }

    private static Path file(JavaPlugin plugin, UUID bossId) {
        return plugin.getDataFolder().toPath().resolve(FOLDER).resolve(PREFIX + bossId + ".txt");
    }

    /**
     * Removes the water of floods whose fight did not end cleanly (a crash): only blocks listed in a journal of an
     * Enlil that is not alive in this run and that are still water. A journal of an unloaded world is kept.
     */
    static void recoverOrphans(JavaPlugin plugin) {
        Path dir = plugin.getDataFolder().toPath().resolve(FOLDER);
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, PREFIX + "*.txt")) {
            for (Path path : files) {
                String name = path.getFileName().toString();
                String id = name.substring(PREFIX.length(), name.length() - ".txt".length());
                if (LIVE.stream().anyMatch(live -> live.toString().equals(id.toLowerCase(Locale.ROOT)))) {
                    continue;
                }
                if (restoreFile(plugin, path)) {
                    Files.deleteIfExists(path);
                }
            }
        } catch (IOException failure) {
            plugin.getLogger().log(Level.WARNING, "[Mazmorras] Enlil: no se pudieron revisar inundaciones viejas",
                failure);
        }
    }

    private static boolean restoreFile(JavaPlugin plugin, Path path) throws IOException {
        int removed = 0;
        boolean complete = true;
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String[] parts = line.trim().split(" ");
            if (parts.length != 4) {
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
                if (block.getType() == Material.WATER) {
                    block.setType(Material.AIR, true);
                    removed++;
                }
            } catch (NumberFormatException ignored) {
                // a damaged line: skip it
            }
        }
        if (removed > 0) {
            plugin.getLogger().info("[Mazmorras] Enlil: se sacaron " + removed + " bloques de agua de una pelea vieja.");
        }
        return complete;
    }
}
