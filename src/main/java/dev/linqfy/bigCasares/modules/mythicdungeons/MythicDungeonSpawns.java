package dev.linqfy.bigCasares.modules.mythicdungeons;

import com.destroystokyo.paper.event.entity.PreSpawnerSpawnEvent;
import org.bukkit.Location;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Llena las mazmorras de las culturas desarrolladas con sus mobs: los spawners que estan adentro crean mobs de su
 * cultura (sorteo con pesos) en vez del vanilla, y los hostiles naturales que nacen adentro se reemplazan con cierta
 * probabilidad. Los dos respetan un tope de mobs de mazmorra cerca. Las demas mazmorras siguen vanilla.
 */
final class MythicDungeonSpawns implements Listener {

    private final MythicMobs mobs;
    private final DungeonLocator locator;
    private final MythicDungeonSettings settings;

    MythicDungeonSpawns(MythicMobs mobs, DungeonLocator locator, MythicDungeonSettings settings) {
        this.mobs = mobs;
        this.locator = locator;
        this.settings = settings;
    }

    /**
     * Paper avisa antes de crear la entidad del spawner: cancelarlo aca le reinicia la espera al spawner (no reintenta
     * cada tick) y no se crea el mob vanilla.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPreSpawner(PreSpawnerSpawnEvent event) {
        Optional<MythicCulture> culture = developedAt(event.getSpawnerLocation());
        if (culture.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        event.setShouldAbortSpawn(true);
        spawnFor(culture.get(), event.getSpawnLocation());
    }

    /** Red de seguridad: si un mob vanilla de un spawner de la mazmorra se coló igual, no nace. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawner(SpawnerSpawnEvent event) {
        if (mobs.keys().isOwnEntity(event.getEntity()) || event.getSpawner() == null) {
            return;
        }
        if (developedAt(event.getSpawner().getLocation()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onNatural(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL
            || !(event.getEntity() instanceof Monster)
            || settings.naturalReplaceChance() <= 0.0) {
            return;
        }
        Location location = event.getLocation();
        Optional<MythicCulture> culture = developedAt(location);
        if (culture.isEmpty()) {
            return;
        }
        // adentro de la mazmorra no nacen hostiles vanilla: o se reemplazan o no nacen
        event.setCancelled(true);
        if (ThreadLocalRandom.current().nextDouble() < settings.naturalReplaceChance()) {
            spawnFor(culture.get(), location);
        }
    }

    /** Las mazmorras de culturas sin desarrollar siguen con los spawns vanilla. */
    private Optional<MythicCulture> developedAt(Location location) {
        return locator.cultureAt(location).filter(MythicCulture::developed);
    }

    private void spawnFor(MythicCulture culture, Location location) {
        if (mobs.countNearby(location, settings.capRadius(), true) >= settings.spawnerCap()) {
            return;
        }
        WeightedPicker.pick(mobs.catalog().ofCulture(culture), MythicMobType::weight,
                ThreadLocalRandom.current().nextDouble())
            .ifPresent(type -> mobs.spawn(type, location, true));
    }
}
