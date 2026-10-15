package dev.linqfy.bigCasares.modules.donpollos;

import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static dev.linqfy.bigCasares.modules.donpollos.DonPollosBossSupport.*;

/**
 * Terreno de la FASE 3: vacia el chunk de cada jugador (el cubo), lo protege mientras duran las Batallas del Cubo
 * (no se rompen ni ponen bloques y no hay forma de escaparse) y al final lo deja como estaba.
 */
final class DonPollosBossTerrain implements Listener {

    /** Las paredes y el piso que cierran el cubo (se sacan al rellenarlo). */
    private static final Material CUBE_WALL = Material.STONE_BRICKS;

    /** Los bosses vivos (el mismo mapa del manager: los cubos de un boss no pueden pisar los de otro). */
    private final Map<UUID, DonPollosBoss> bosses;
    /** Aviso al que intenta escaparse del cubo (el texto lo arma el manager: aca no se carga Adventure). */
    private final java.util.function.Consumer<Player> escapeWarning;
    /** Aviso al que intenta poner o romper bloques en el cubo. */
    private final java.util.function.Consumer<Player> buildWarning;

    DonPollosBossTerrain(Map<UUID, DonPollosBoss> bosses, java.util.function.Consumer<Player> escapeWarning,
                         java.util.function.Consumer<Player> buildWarning) {
        this.bosses = bosses;
        this.escapeWarning = escapeWarning;
        this.buildWarning = buildWarning;
    }

    /** Rellena los cubos del boss y sube a quien haya quedado adentro. */
    void restore(DonPollosBoss boss) {
        // Primero se anota quien esta en cada cubo, despues se rellenan todos (en orden inverso, porque los cubos
        // vecinos comparten paredes) y recien ahi se sube a los jugadores, con el terreno ya completo.
        Map<Player, Integer> exitHeights = new java.util.LinkedHashMap<>();
        for (DonPollosBossPit pit : boss.pits) {
            for (Player p : pit.world().getPlayers()) {
                if (pit.contains(p.getLocation())) {
                    exitHeights.putIfAbsent(p, pit.feet());
                }
            }
        }
        for (BlockState state : DonPollosPitRestore.restoreOrder(boss.pits.stream().map(DonPollosBossPit::changed).toList())) {
            state.update(true, false);
        }
        for (Map.Entry<Player, Integer> exit : exitHeights.entrySet()) {
            Player p = exit.getKey();
            Location up = surface(p.getLocation(), exit.getValue());
            p.teleport(up);
            p.setFallDistance(0);
            p.getWorld().spawnParticle(Particle.CLOUD, up.clone().add(0, 0.5, 0), 30, 0.5, 0.5, 0.5, 0.05);
            p.getWorld().playSound(up, Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.8f);
        }
        boss.pits.clear();
    }

    /** Vacia el chunk donde esta el jugador: {@code depth} capas de bloques debajo de sus pies (nunca la bedrock). */
    static int pitBottom(int feetY, int depth, int minHeight) {
        return Math.max(minHeight, feetY - depth);
    }

    /** Si un cubo nuevo de este boss tocaria los bloques de un cubo de otro boss (ver DonPollosPitRestore). */
    boolean touchesOtherBossPit(DonPollosBoss owner, World world, int chunkX, int chunkZ, int bottom, int feet) {
        for (DonPollosBoss other : bosses.values()) {
            if (other == owner) {
                continue;
            }
            for (DonPollosBossPit pit : other.pits) {
                if (pit.world() == world && DonPollosPitRestore.regionsOverlap(chunkX, chunkZ, bottom, feet,
                    pit.chunkX(), pit.chunkZ(), pit.bottom(), pit.feet())) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean inAnyPit(DonPollosBoss boss, Location location) {
        return boss.pits.stream().anyMatch(pit -> pit.contains(location));
    }

    DonPollosBossPit digPit(DonPollosBoss owner, Player player, int depth) {
        if (depth <= 0) {
            return null;
        }
        World world = player.getWorld();
        Chunk chunk = player.getLocation().getChunk();
        int feet = player.getLocation().getBlockY();
        int bottom = pitBottom(feet, depth, world.getMinHeight());
        if (touchesOtherBossPit(owner, world, chunk.getX(), chunk.getZ(), bottom, feet)) {
            return null;  // pelea en la superficie: el cubo del otro boss queda intacto
        }
        List<BlockState> changed = new ArrayList<>();
        for (int y = feet - 1; y >= bottom; y--) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    Block block = chunk.getBlock(x, y, z);
                    if (!block.getType().isAir() && block.getType() != Material.BEDROCK) {
                        changed.add(block.getState());
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }
        // paredes de todo el alto del cubo alrededor del chunk (aunque afuera el terreno sea mas bajo, como en
        // una pendiente) y piso abajo: no se puede salir caminando ni caerse a una cueva
        int baseX = chunk.getX() * 16;
        int baseZ = chunk.getZ() * 16;
        for (int y = bottom; y <= feet - 1; y++) {
            for (int i = -1; i <= 16; i++) {
                closeGap(world.getBlockAt(baseX + i, y, baseZ - 1), changed);
                closeGap(world.getBlockAt(baseX + i, y, baseZ + 16), changed);
                closeGap(world.getBlockAt(baseX - 1, y, baseZ + i), changed);
                closeGap(world.getBlockAt(baseX + 16, y, baseZ + i), changed);
            }
        }
        if (bottom - 1 >= world.getMinHeight()) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    closeGap(chunk.getBlock(x, bottom - 1, z), changed);
                }
            }
        }
        Location center = new Location(world, chunk.getX() * 16 + 8, feet - depth / 2.0, chunk.getZ() * 16 + 8);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 2, 4, 1, 4, 0, null, true);
        world.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.5f);
        world.playSound(player.getLocation(), Sound.BLOCK_ROOTED_DIRT_BREAK, 2f, 0.5f);
        return new DonPollosBossPit(world, chunk.getX(), chunk.getZ(), feet, bottom, changed);
    }

    /** Tapa un hueco (aire, agua, pasto...) de la pared o el piso del cubo, guardando como estaba. */
    static void closeGap(Block block, List<BlockState> changed) {
        if (block.isPassable() || block.isLiquid()) {
            changed.add(block.getState());
            block.setType(CUBE_WALL, false);
        }
    }

    /** Primer lugar libre (2 bloques de alto) desde la altura original del piso para arriba. */
    static Location surface(Location from, int feet) {
        Location at = from.clone();
        at.setY(feet);
        for (int i = 0; i < 64; i++) {
            if (at.getBlock().isPassable() && at.clone().add(0, 1, 0).getBlock().isPassable()) {
                return at;
            }
            at.add(0, 1, 0);
        }
        return at;
    }

    /**
     * Mientras duran las Batallas del Cubo no se tocan bloques si estas adentro del cubo, ni en el cubo o sus
     * paredes aunque estes afuera (los de creativo si pueden).
     */
    boolean cubeLocked(Player player, Block block) {
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }
        for (DonPollosBoss boss : bosses.values()) {
            if (boss.cube == null) {
                continue;
            }
            for (DonPollosBossPit pit : boss.pits) {
                if (pit.contains(player.getLocation()) || (block != null && pit.containsBlock(block))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** El cubo en el que esta encerrado este jugador ahora (con las Batallas del Cubo en curso), o null. */
    DonPollosBossPit trappingPit(Player player, Location at) {
        if (!attackable(player)) {
            return null;
        }
        for (DonPollosBoss boss : bosses.values()) {
            if (boss.cube == null) {
                continue;
            }
            for (DonPollosBossPit pit : boss.pits) {
                if (pit.contains(at)) {
                    return pit;
                }
            }
        }
        return null;
    }

    void blockEscape(Player player) {
        escapeWarning.accept(player);
    }

    /** Perlas, fruta chorus, comandos (/home, /tpa, /spawn...): nada te saca del cubo. */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onCubeTeleport(org.bukkit.event.player.PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        DonPollosBossPit pit = trappingPit(player, event.getFrom());
        if (pit != null && DonPollosCubeEscape.escapes(true, pit.contains(event.getTo()))) {
            event.setCancelled(true);
            blockEscape(player);
        }
    }

    /** Elytras, tridente con Corriente, cargas de viento, saltos: si salis volando por arriba, volves adentro. */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onCubeMove(org.bukkit.event.player.PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;  // sin cambiar de bloque no se cruza el borde del cubo
        }
        Player player = event.getPlayer();
        DonPollosBossPit pit = trappingPit(player, from);
        if (pit == null || !DonPollosCubeEscape.escapes(true, pit.contains(to))) {
            return;
        }
        Location back = from.clone();
        back.setYaw(to.getYaw());
        back.setPitch(to.getPitch());
        event.setTo(back);
        player.setGliding(false);
        player.setVelocity(new Vector(0, Math.min(0, player.getVelocity().getY()), 0));
        blockEscape(player);
    }

    /** Adentro del cubo no se abren las elytras. */
    @EventHandler(ignoreCancelled = true)
    public void onCubeGlide(org.bukkit.event.entity.EntityToggleGlideEvent event) {
        if (event.isGliding() && event.getEntity() instanceof Player player
            && trappingPit(player, player.getLocation()) != null) {
            event.setCancelled(true);
            blockEscape(player);
        }
    }

    /** Adentro del cubo no se tiran perlas ni cargas de viento. */
    @EventHandler(ignoreCancelled = true)
    public void onCubeProjectile(org.bukkit.event.entity.ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player player
            && DonPollosCubeEscape.blocksProjectile(event.getEntity().getType().name())
            && trappingPit(player, player.getLocation()) != null) {
            event.setCancelled(true);
            blockEscape(player);
        }
    }

    void noEscape(org.bukkit.event.Cancellable event, Player player) {
        event.setCancelled(true);
        buildWarning.accept(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCubePlace(org.bukkit.event.block.BlockPlaceEvent event) {
        if (cubeLocked(event.getPlayer(), event.getBlock())) {
            noEscape(event, event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCubeBreak(org.bukkit.event.block.BlockBreakEvent event) {
        if (cubeLocked(event.getPlayer(), event.getBlock())) {
            noEscape(event, event.getPlayer());
        }
    }

    /** Tampoco agua ni lava: con una columna de agua se sale nadando. */
    @EventHandler(ignoreCancelled = true)
    public void onCubeBucket(org.bukkit.event.player.PlayerBucketEmptyEvent event) {
        if (cubeLocked(event.getPlayer(), event.getBlock())) {
            noEscape(event, event.getPlayer());
        }
    }
}
