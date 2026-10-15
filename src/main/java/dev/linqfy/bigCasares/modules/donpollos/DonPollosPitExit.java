package dev.linqfy.bigCasares.modules.donpollos;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongSupplier;

/**
 * Salida para quien se desconecta adentro de un cubo: al salir se anota (en el propio jugador, asi sobrevive a que se
 * rellenen los cubos y a los reinicios) la superficie donde subirlo, y al volver a entrar se lo lleva ahi si ya no
 * esta adentro de un cubo. La marca se borra solo cuando el teletransporte salio bien.
 */
final class DonPollosPitExit implements Listener {

    /** Una marca mas vieja que esto (7 dias) ya no sirve: se borra sin teletransportar. */
    static final long MAX_AGE_MILLIS = 7L * 24 * 60 * 60 * 1000;

    private final Map<UUID, DonPollosBoss> bosses;
    private final NamespacedKey key;
    /** Corre la tarea un tick despues (cuando el jugador ya termino de entrar). */
    private final Consumer<Runnable> nextTick;
    private final Function<UUID, World> worlds;
    private final LongSupplier clock;

    DonPollosPitExit(Map<UUID, DonPollosBoss> bosses, NamespacedKey key, Consumer<Runnable> nextTick,
                     Function<UUID, World> worlds, LongSupplier clock) {
        this.bosses = bosses;
        this.key = key;
        this.nextTick = nextTick;
        this.worlds = worlds;
        this.clock = clock;
    }

    /** El cubo de unas Batallas del Cubo en curso que contiene este lugar, o null. */
    private DonPollosBossPit activePitAt(Location at) {
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

    /** Lo que se anota al desconectarse adentro de un cubo. */
    record Marker(UUID worldId, int chunkX, int chunkZ, int feet, double x, double z, float yaw, float pitch,
                  long savedAt) {

        String encode() {
            return worldId + ";" + chunkX + ";" + chunkZ + ";" + feet + ";" + x + ";" + z + ";" + yaw + ";" + pitch
                + ";" + savedAt;
        }

        static Marker decode(String text) {
            try {
                String[] p = text.split(";");
                return new Marker(UUID.fromString(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]),
                    Integer.parseInt(p[3]), Double.parseDouble(p[4]), Double.parseDouble(p[5]),
                    Float.parseFloat(p[6]), Float.parseFloat(p[7]), Long.parseLong(p[8]));
            } catch (RuntimeException e) {
                return null;
            }
        }

        /** Si ese lugar sigue siendo el hueco del cubo: mismo mundo, dentro de su chunk y debajo de la superficie. */
        boolean isInTheHole(Location at) {
            return at.getWorld() != null && at.getWorld().getUID().equals(worldId)
                && (at.getBlockX() >> 4) == chunkX && (at.getBlockZ() >> 4) == chunkZ && at.getY() < feet;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Location at = player.getLocation();
        DonPollosBossPit pit = activePitAt(at);
        if (pit != null) {
            Marker marker = new Marker(at.getWorld().getUID(), pit.chunkX(), pit.chunkZ(), pit.feet(), at.getX(),
                at.getZ(), at.getYaw(), at.getPitch(), clock.getAsLong());
            player.getPersistentDataContainer().set(key, PersistentDataType.STRING, marker.encode());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            nextTick.accept(() -> recover(player));
        }
    }

    /**
     * Lo lleva a la superficie si sigue donde se desconecto (en el hueco del cubo); la salida se recalcula ahora, asi
     * una construccion posterior no lo ahoga. La marca queda si el mundo no esta cargado o el teletransporte fallo, y
     * se borra si ya no sirve (vieja, ilegible, lo movio otro plugin o vuelve a un cubo activo).
     */
    void recover(Player player) {
        String saved = player.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (saved == null) {
            return;
        }
        Marker marker = Marker.decode(saved);
        if (marker == null || clock.getAsLong() - marker.savedAt() > MAX_AGE_MILLIS) {
            player.getPersistentDataContainer().remove(key);
            return;
        }
        World world = worlds.apply(marker.worldId());
        if (world == null) {
            return;  // el mundo no esta cargado ahora: la marca queda para la proxima
        }
        Location here = player.getLocation();
        if (!marker.isInTheHole(here) || activePitAt(here) != null) {
            player.getPersistentDataContainer().remove(key);
            return;
        }
        Location exit = DonPollosBossTerrain.surface(
            new Location(world, marker.x(), marker.feet(), marker.z(), marker.yaw(), marker.pitch()), marker.feet());
        if (player.teleport(exit)) {
            player.setFallDistance(0);
            player.getPersistentDataContainer().remove(key);
        }
    }
}
