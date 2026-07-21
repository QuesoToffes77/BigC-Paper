package dev.linqfy.bigCasares.modules.specialitems;

import dev.linqfy.bigCasares.BigCasares;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class NukeAnimationRuntime implements Runnable, AutoCloseable {

    private final BigCasares plugin;
    private final SpecialItemsSettings settings;
    private final List<NukeRing> layout;
    private final List<ActiveAnimation> animations = new ArrayList<>();

    public NukeAnimationRuntime(BigCasares plugin, SpecialItemsSettings settings) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.layout = NukeRingLayout.create(
            settings.ringCount(), settings.totalTnt() - 1, settings.radiusStep());
    }

    public void start(Player owner) {
        Objects.requireNonNull(owner, "owner");
        Location center = owner.getLocation().clone().add(0.0, settings.nukeHeight(), 0.0);
        ActiveAnimation animation = new ActiveAnimation(owner.getUniqueId(), center);
        try {
            animation.displays.add(spawnDisplay(center));
            animation.tntLocations.add(center.clone());
            center.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 0.5f);
            animations.add(animation);
        } catch (RuntimeException failure) {
            cleanup(animation);
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "No se pudo iniciar el Nuke Shot", failure);
        }
    }

    @Override
    public void run() {
        Iterator<ActiveAnimation> iterator = animations.iterator();
        while (iterator.hasNext()) {
            ActiveAnimation animation = iterator.next();
            if (!advance(animation)) {
                iterator.remove();
            }
        }
    }

    @Override
    public void close() {
        for (ActiveAnimation animation : animations) {
            cleanup(animation);
        }
        animations.clear();
    }

    int activeAnimationCount() {
        return animations.size();
    }

    private boolean advance(ActiveAnimation animation) {
        World world = plugin.getServer().getWorld(animation.worldId);
        if (world == null) {
            cleanup(animation);
            return false;
        }
        animation.elapsedTicks++;
        if (animation.nextRing >= layout.size()
            || animation.elapsedTicks < (long) (animation.nextRing + 1) * settings.ringIntervalTicks()) {
            return true;
        }
        try {
            NukeRing ring = layout.get(animation.nextRing);
            for (NukePoint point : ring.points()) {
                Location location = animation.center.clone().add(point.x(), 0.0, point.z());
                animation.displays.add(spawnDisplay(location));
                animation.tntLocations.add(location);
            }
            animation.nextRing++;
            world.playSound(animation.center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.2f,
                0.5f + animation.nextRing * 0.1f);
            if (animation.nextRing == layout.size()) {
                release(animation, world);
                return false;
            }
            return true;
        } catch (RuntimeException failure) {
            cleanup(animation);
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Falló la animación del Nuke Shot", failure);
            return false;
        }
    }

    private BlockDisplay spawnDisplay(Location location) {
        World world = Objects.requireNonNull(location.getWorld(), "display world");
        return world.spawn(location.clone().subtract(0.5, 0.5, 0.5), BlockDisplay.class, display -> {
            display.setBlock(Material.TNT.createBlockData());
            display.setGravity(false);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setViewRange(96.0f);
        });
    }

    private void release(ActiveAnimation animation, World world) {
        List<TNTPrimed> primed = new ArrayList<>(animation.tntLocations.size());
        Player owner = plugin.getServer().getPlayer(animation.ownerId);
        try {
            for (Location location : animation.tntLocations) {
                primed.add(world.spawn(location, TNTPrimed.class, tnt -> {
                    if (owner != null && owner.isOnline()) {
                        tnt.setSource(owner);
                    }
                }));
            }
        } catch (RuntimeException failure) {
            primed.forEach(tnt -> {
                if (tnt.isValid()) {
                    tnt.remove();
                }
            });
            throw failure;
        }
        removeDisplays(animation.displays);
        animation.displays.clear();
        animation.tntLocations.clear();
        world.playSound(animation.center, Sound.ENTITY_TNT_PRIMED, 4.0f, 0.6f);
    }

    private static void cleanup(ActiveAnimation animation) {
        removeDisplays(animation.displays);
        animation.displays.clear();
        animation.tntLocations.clear();
    }

    private static void removeDisplays(List<BlockDisplay> displays) {
        for (BlockDisplay display : displays) {
            if (display != null && display.isValid()) {
                display.remove();
            }
        }
    }

    private static final class ActiveAnimation {

        private final UUID ownerId;
        private final UUID worldId;
        private final Location center;
        private final List<BlockDisplay> displays = new ArrayList<>();
        private final List<Location> tntLocations = new ArrayList<>();
        private long elapsedTicks;
        private int nextRing;

        private ActiveAnimation(UUID ownerId, Location center) {
            this.ownerId = ownerId;
            this.worldId = Objects.requireNonNull(center.getWorld(), "animation world").getUID();
            this.center = center.clone();
        }
    }
}
