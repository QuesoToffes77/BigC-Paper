package dev.linqfy.bigCasares.modules.smokebomb;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class SmokeBombProjectileListener implements Listener {

    private final BigCasares plugin;
    private final SmokeBombItem smokeBombItem;
    private final SmokeBombSettings settings;
    private final SmokeCloudService cloudService;
    private final SmokeConcealmentService concealmentService;
    private final AtomicLong tickCounter = new AtomicLong();
    private final Map<String, ActiveSmokeCloud> activeClouds = new LinkedHashMap<>();
    private final BukkitTask heartbeatTask;
    private boolean shutdown;

    public SmokeBombProjectileListener(
        BigCasares plugin,
        SmokeBombItem smokeBombItem,
        SmokeBombSettings settings,
        SmokeCloudService cloudService,
        SmokeConcealmentService concealmentService,
        BukkitRuntimeRegistrations registrations
    ) {
        this.plugin = plugin;
        this.smokeBombItem = smokeBombItem;
        this.settings = settings;
        this.cloudService = cloudService;
        this.concealmentService = concealmentService;
        this.heartbeatTask = registrations.scheduleRepeating("heartbeat", this::heartbeat, 1L, 1L);
    }

    public void shutdown() {
        if (shutdown) {
            return;
        }
        shutdown = true;
        if (!heartbeatTask.isCancelled()) {
            heartbeatTask.cancel();
        }
        activeClouds.clear();
        concealmentService.revealAll();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile instanceof Snowball snowball)) {
            return;
        }
        if (!smokeBombItem.matches(snowball.getItem())) {
            return;
        }

        PersistentDataContainer container = projectile.getPersistentDataContainer();
        container.set(smokeBombItem.getItemKey(), PersistentDataType.BYTE, (byte) 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile instanceof Snowball snowball)) {
            return;
        }

        PersistentDataContainer container = projectile.getPersistentDataContainer();
        Byte marker = container.get(smokeBombItem.getItemKey(), PersistentDataType.BYTE);
        if (marker == null || marker != (byte) 1) {
            return;
        }

        Location impactLocation = event.getHitBlock() != null
            ? event.getHitBlock().getLocation().add(0.5, 0.5, 0.5)
            : snowball.getLocation().clone();

        createCloud(impactLocation, projectile.getShooter() instanceof Player player ? player : null);
        projectile.remove();
    }

    private void createCloud(Location location, Player thrower) {
        long currentTick = tickCounter.get();
        String cloudId = UUID.randomUUID().toString();
        ActiveSmokeCloud cloud = new ActiveSmokeCloud(
            cloudId,
            location.clone(),
            currentTick + settings.durationTicks(),
            currentTick
        );

        activeClouds.put(cloudId, cloud);
        cloudService.createCloud(cloudId, currentTick);

        playImpactEffects(location);
        if (thrower != null) {
            thrower.playSound(thrower.getLocation(), Sound.ENTITY_SQUID_SQUIRT, 1.0f, 0.6f);
        }
    }

    private void heartbeat() {
        long currentTick = tickCounter.incrementAndGet();
        activeClouds.entrySet().removeIf(entry -> currentTick >= entry.getValue().expiresAtTick());

        for (ActiveSmokeCloud cloud : activeClouds.values()) {
            if (shouldEmitBurst(cloud, currentTick)) {
                emitSustainedSmoke(cloud.center());
                cloud.lastBurstTick = currentTick;
            }
        }

        if (currentTick % settings.occupancyIntervalTicks() != 0L) {
            return;
        }

        Set<UUID> occupants = new LinkedHashSet<>();
        for (ActiveSmokeCloud cloud : activeClouds.values()) {
            for (var nearby : cloud.center().getWorld().getNearbyEntities(
                cloud.center(),
                settings.halfWidth(),
                settings.halfHeight(),
                settings.halfDepth()
            )) {
                if (!(nearby instanceof LivingEntity entity)) {
                    continue;
                }
                occupants.add(entity.getUniqueId());
                concealmentService.refresh(entity);
            }
        }

        SmokeCloudService.TickResult result = cloudService.tick(currentTick, occupants);
        for (UUID concealedId : result.newlyConcealed()) {
            LivingEntity entity = livingEntity(concealedId);
            if (entity != null) {
                concealmentService.conceal(entity);
            }
        }
        for (UUID revealedId : result.newlyRevealed()) {
            concealmentService.reveal(revealedId);
        }
    }

    private boolean shouldEmitBurst(ActiveSmokeCloud cloud, long currentTick) {
        return currentTick == cloud.createdAtTick()
            || currentTick - cloud.lastBurstTick >= settings.smokeBurstIntervalTicks();
    }

    private void playImpactEffects(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        emitToNearbyPlayers(Particle.SQUID_INK, location, settings.impactParticles());
        world.playSound(location, Sound.ENTITY_GLOW_SQUID_SQUIRT, 1.0f, 0.8f);
        world.playSound(location, Sound.BLOCK_FIRE_EXTINGUISH, 1.2f, 0.6f);
        emitSustainedSmoke(location);
    }

    private void emitSustainedSmoke(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        emitToNearbyPlayers(Particle.CAMPFIRE_SIGNAL_SMOKE, location, settings.cloudParticles());
        world.playSound(location, Sound.BLOCK_CAMPFIRE_CRACKLE, 0.5f, 0.7f);
    }

    private void emitToNearbyPlayers(Particle particle, Location location, int count) {
        World world = location.getWorld();
        if (world == null || count <= 0) {
            return;
        }
        double maximumDistanceSquared = settings.particleViewDistance() * settings.particleViewDistance();
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(location) <= maximumDistanceSquared) {
                player.spawnParticle(particle, location, count, 3.0, 2.0, 3.0, 0.0);
            }
        }
    }

    private LivingEntity livingEntity(UUID entityId) {
        var entity = Bukkit.getEntity(entityId);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static final class ActiveSmokeCloud {
        private final String id;
        private final Location center;
        private final long expiresAtTick;
        private final long createdAtTick;
        private long lastBurstTick;

        private ActiveSmokeCloud(String id, Location center, long expiresAtTick, long createdAtTick) {
            this.id = id;
            this.center = center;
            this.expiresAtTick = expiresAtTick;
            this.createdAtTick = createdAtTick;
            this.lastBurstTick = createdAtTick - 1L;
        }

        public String id() {
            return id;
        }

        public Location center() {
            return center;
        }

        public long expiresAtTick() {
            return expiresAtTick;
        }

        public long createdAtTick() {
            return createdAtTick;
        }
    }
}
