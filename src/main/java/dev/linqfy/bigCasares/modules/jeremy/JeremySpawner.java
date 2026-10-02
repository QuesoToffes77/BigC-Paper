package dev.linqfy.bigCasares.modules.jeremy;

import net.kyori.adventure.text.Component;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Objects;
import java.util.Optional;
import java.util.Random;

final class JeremySpawner {
    private final Plugin plugin;
    private final JeremySettings settings;
    private final JeremyIdentity identity;
    private final Random random = new Random();

    JeremySpawner(Plugin plugin, JeremySettings settings, JeremyIdentity identity) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.identity = Objects.requireNonNull(identity, "identity");
    }

    Optional<Zombie> spawn(Player target, double powerScore) {
        Optional<Location> location = findSpawnLocation(target);
        if (location.isEmpty()) {
            return Optional.empty();
        }
        Zombie zombie = target.getWorld().spawn(
            location.get(), Zombie.class, CreatureSpawnEvent.SpawnReason.CUSTOM, true,
            spawned -> prepareBeforeSpawn(spawned, target)
        );
        normalizeAfterSpawn(zombie, target, powerScore);
        target.getWorld().spawnParticle(Particle.POOF, zombie.getLocation().add(0, 1, 0), 24, 0.5, 0.8, 0.5, 0.03);
        return Optional.of(zombie);
    }

    private Optional<Location> findSpawnLocation(Player target) {
        Location visibleFallback = null;
        for (int attempt = 0; attempt < settings.spawn().attempts(); attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = settings.spawn().minDistance()
                + random.nextDouble() * (settings.spawn().maxDistance() - settings.spawn().minDistance());
            int x = (int) Math.floor(target.getLocation().getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(target.getLocation().getZ() + Math.sin(angle) * distance);
            World world = target.getWorld();
            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }
            int y = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES) + 1;
            Location candidate = new Location(world, x + 0.5, y, z + 0.5);
            if (!safe(candidate)) {
                continue;
            }
            if (hiddenFrom(target, candidate)) {
                return Optional.of(candidate);
            }
            if (visibleFallback == null) {
                visibleFallback = candidate;
            }
        }
        return Optional.ofNullable(visibleFallback);
    }

    private static boolean safe(Location location) {
        Material below = location.clone().subtract(0, 1, 0).getBlock().getType();
        Material feet = location.getBlock().getType();
        Material head = location.clone().add(0, 1, 0).getBlock().getType();
        return below.isSolid() && !below.isAir() && !below.equals(Material.MAGMA_BLOCK)
            && feet.isAir() && head.isAir();
    }

    private static boolean hiddenFrom(Player target, Location candidate) {
        Location eye = target.getEyeLocation();
        Vector direction = candidate.clone().add(0, 1, 0).toVector().subtract(eye.toVector());
        double distance = direction.length();
        if (distance <= 0.01) {
            return false;
        }
        RayTraceResult result = target.getWorld().rayTraceBlocks(eye, direction.normalize(), distance);
        return result != null && result.getHitBlock() != null;
    }

    private void prepareBeforeSpawn(Zombie zombie, Player target) {
        identity.mark(zombie, target.getUniqueId());
        zombie.customName(Component.text(settings.displayName()));
        zombie.setCustomNameVisible(true);
        zombie.setAdult();
        zombie.setShouldBurnInDay(false);
        zombie.setCanPickupItems(false);
        zombie.setPersistent(true);
        zombie.setRemoveWhenFarAway(false);
        zombie.setAware(true);
        zombie.setTarget(target);
    }

    private void normalizeAfterSpawn(Zombie zombie, Player target, double powerScore) {
        setBase(zombie, Attribute.MOVEMENT_SPEED, settings.movement().effectiveSpeed());
        double health = settings.health().scaledHealth(powerScore);
        setBase(zombie, Attribute.MAX_HEALTH, health);
        zombie.setHealth(Math.min(health, Objects.requireNonNull(zombie.getAttribute(Attribute.MAX_HEALTH)).getValue()));
        EntityEquipment equipment = zombie.getEquipment();
        equipment.clear();
        zombie.setTarget(target);
    }

    private static void setBase(Zombie zombie, Attribute attribute, double value) {
        AttributeInstance instance = zombie.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }
}
