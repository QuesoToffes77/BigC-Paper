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
    /** Zonas donde no se cava (los nexus en BigCasares). */
    private final DonPollosProtection protection;
    /** Aviso al que intenta escaparse del cubo (el texto lo arma el manager: aca no se carga Adventure). */
    private final java.util.function.Consumer<Player> escapeWarning;
    /** Aviso al que intenta poner o romper bloques en el cubo. */
    private final java.util.function.Consumer<Player> buildWarning;

    DonPollosBossTerrain(Map<UUID, DonPollosBoss> bosses, DonPollosProtection protection,
                         java.util.function.Consumer<Player> escapeWarning,
                         java.util.function.Consumer<Player> buildWarning) {
        this.bosses = bosses;
        this.protection = protection;
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

    /** Si el bloque es parte (paredes, piso o interior) del cubo de cualquier boss vivo. */
    boolean isCubeBlock(Block block) {
        for (DonPollosBoss boss : bosses.values()) {
            for (DonPollosBossPit pit : boss.pits) {
                if (pit.containsBlock(block)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Los jugadores atacables que estan realmente adentro de algun cubo de este boss, lejos o cerca del Ravager. */
    static List<Player> occupants(DonPollosBoss boss) {
        List<Player> inside = new ArrayList<>();
        for (DonPollosBossPit pit : boss.pits) {
            for (Player p : pit.world().getPlayers()) {
                if (attackable(p) && pit.contains(p.getLocation()) && !inside.contains(p)) {
                    inside.add(p);
                }
            }
        }
        return inside;
    }

    /** A quien le toca el turno entre los que estan adentro (rotando de a uno, orden estable); null si no hay nadie. */
    static Player pickFighter(List<Player> occupants, long turn) {
        if (occupants.isEmpty()) {
            return null;
        }
        List<Player> sorted = new ArrayList<>(occupants);
        sorted.sort(java.util.Comparator.comparing(Player::getUniqueId));
        return sorted.get((int) (turn % sorted.size()));
    }

    /** Cuanto puede diferir en vertical la superficie de un chunk de reemplazo de la altura del jugador. */
    static final int MAX_RELOCATE_VERTICAL = 24;

    /** Si la superficie del chunk candidato esta a una altura razonable de donde estaba el jugador. */
    static boolean reasonableRelocation(int candidateFeet, int playerFeet) {
        return Math.abs(candidateFeet - playerFeet) <= MAX_RELOCATE_VERTICAL;
    }

    static boolean inAnyPit(DonPollosBoss boss, Location location) {
        return boss.pits.stream().anyMatch(pit -> pit.contains(location));
    }

    /**
     * Si en ese chunk se puede cavar un cubo de este boss: no toca una zona protegida (ni sus paredes) ni el cubo de
     * otro boss.
     */
    private boolean canDig(DonPollosBoss owner, World world, int chunkX, int chunkZ, int bottom, int feet) {
        return !touchesOtherBossPit(owner, world, chunkX, chunkZ, bottom, feet)
            && !protection.isProtected(world, chunkX * 16 - 1, chunkX * 16 + 16, bottom - 1, feet,
                chunkZ * 16 - 1, chunkZ * 16 + 16);
    }

    /** Si este boss ya tiene un cubo en ese chunk. */
    private static DonPollosBossPit ownPit(DonPollosBoss owner, World world, int chunkX, int chunkZ) {
        for (DonPollosBossPit pit : owner.pits) {
            if (pit.world() == world && pit.chunkX() == chunkX && pit.chunkZ() == chunkZ) {
                return pit;
            }
        }
        return null;
    }

    /**
     * Cava el cubo del jugador en su chunk. Si ahi no se puede (zona protegida o el cubo de otro boss), lo cava en el
     * chunk permitido mas cercano y lo lleva adentro; si ese chunk ya es un cubo de este boss, lo lleva a ese. Si no
     * hay ninguno cerca, pelea en la superficie (devuelve null).
     */
    DonPollosBossPit digPit(DonPollosBoss owner, Player player, int depth) {
        if (depth <= 0) {
            return null;
        }
        World world = player.getWorld();
        Chunk here = player.getLocation().getChunk();
        int feetHere = player.getLocation().getBlockY();
        int bottomHere = pitBottom(feetHere, depth, world.getMinHeight());
        if (canDig(owner, world, here.getX(), here.getZ(), bottomHere, feetHere)) {
            return dig(world, here, player, feetHere, bottomHere, depth, false);
        }
        // Su chunk no se puede: el permitido mas cercano. Si no hubo ninguno hace poco, no se busca de nuevo enseguida
        // (digNewcomers vuelve a intentar cada medio segundo).
        long now = org.bukkit.Bukkit.getCurrentTick();
        if (now < noRoomUntil.getOrDefault(player.getUniqueId(), 0L)) {
            return null;
        }
        for (int[] candidate : DonPollosPitPlacement.byDistance(here.getX(), here.getZ(),
            DonPollosPitPlacement.MAX_SEARCH_CHUNKS)) {
            int chunkX = candidate[0];
            int chunkZ = candidate[1];
            DonPollosBossPit existing = ownPit(owner, world, chunkX, chunkZ);
            if (existing != null) {
                moveInto(player, world, chunkX, chunkZ, existing.bottom());
                return null;  // a un cubo que este boss ya tiene
            }
            // descarte barato a la altura del jugador; despues, el chunk de verdad a su propia altura
            if (!canDig(owner, world, chunkX, chunkZ, bottomHere, feetHere)) {
                continue;
            }
            int feet = world.getHighestBlockYAt(chunkX * 16 + 8, chunkZ * 16 + 8,
                org.bukkit.HeightMap.MOTION_BLOCKING_NO_LEAVES) + 1;
            // Cambio de comportamiento: si el unico chunk permitido esta a mas de MAX_RELOCATE_VERTICAL de altura se lo
            // descarta; sin ningun pozo lejano valido (digPit devuelve null) se pelea en la superficie.
            if (!reasonableRelocation(feet, feetHere)) {
                continue;  // una meseta o un valle muy lejos en vertical: no se lo manda ahi
            }
            int bottom = pitBottom(feet, depth, world.getMinHeight());
            if (canDig(owner, world, chunkX, chunkZ, bottom, feet)) {
                return dig(world, world.getChunkAt(chunkX, chunkZ), player, feet, bottom, depth, true);
            }
        }
        noRoomUntil.put(player.getUniqueId(), now + NO_ROOM_RETRY_TICKS);
        return null;  // pelea en la superficie: no hay donde cavar cerca
    }

    /** Despues de no encontrar lugar para un jugador, cuando se vuelve a buscar (5 segundos). */
    private static final long NO_ROOM_RETRY_TICKS = 100;
    private final Map<UUID, Long> noRoomUntil = new java.util.HashMap<>();

    /**
     * Vacia el chunk y le pone paredes y piso. {@code moved}: es otro chunk que el del jugador, asi que lo lleva
     * adentro; si no se lo puede llevar (otro plugin cancelo el teletransporte), deja el terreno como estaba.
     */
    private DonPollosBossPit dig(World world, Chunk chunk, Player player, int feet, int bottom, int depth, boolean moved) {
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
        if (moved) {
            if (!moveInto(player, world, chunk.getX(), chunk.getZ(), bottom)) {
                for (BlockState state : DonPollosPitRestore.restoreOrder(List.of(changed))) {
                    state.update(true, false);
                }
                return null;
            }
            player.sendMessage("§eAca no se puede cavar (zona protegida): §6tu cubo esta en el chunk mas cercano.");
        }
        return new DonPollosBossPit(world, chunk.getX(), chunk.getZ(), feet, bottom, changed);
    }

    /** Lleva al jugador al centro del piso del cubo de ese chunk. */
    private static boolean moveInto(Player player, World world, int chunkX, int chunkZ, int bottom) {
        Location at = new Location(world, chunkX * 16 + 8.5, bottom, chunkZ * 16 + 8.5,
            player.getLocation().getYaw(), player.getLocation().getPitch());
        if (!player.teleport(at)) {
            return false;
        }
        player.setFallDistance(0);
        return true;
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
