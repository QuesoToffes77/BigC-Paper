package dev.linqfy.bigCasares.modules.grapplinghook;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.modules.grapplinghook.GrappleSimulation.GrappleTick;
import dev.linqfy.bigCasares.modules.grapplinghook.GrappleSimulation.Phase;
import org.bukkit.Location;
import org.bukkit.Input;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * One-tick driver for all active grapple sessions. Each session owns a
 * TRIPWIRE_HOOK display (the hook) and a growing list of CHAIN displays (the
 * rope). The hook flies from the eye to the target while the chain extends
 * behind it; on arrival the chain stays tense and the first attach tick fires
 * the pull impulse and starts the tier cooldown. The session ends when the
 * player arrives or the attach budget is exhausted, and every display is
 * removed. Re-activating a session starts a retract: the hook returns to the
 * player, the chain shrinks away, and the session ends without a pull.
 *
 * <p>The tick task is started lazily on the first launch and cancelled as
 * soon as the last session ends, so no scheduler work exists while nobody is
 * grappling. When a session ends naturally (arrival, miss, finished retract)
 * the holder's model plays the {@code reload} one-shot as the retraction
 * visual; cancelled sessions clean up silently.
 */
public final class GrapplingHookRuntime implements Runnable, AutoCloseable {

    /** Below this distance the pull is skipped so a close hook never launches. */
    private static final double MIN_PULL_DISTANCE = 1.5;

    /** Attraction speed for a hooked entity (mobs are yanked, never launched). */
    private static final double ENTITY_PULL_FRACTION = 0.45;
    private static final double MAX_ENTITY_PULL_SPEED = 1.4;

    private final BigCasares plugin;
    private final GrapplingHookSettings settings;
    private final GrappleCooldownService cooldowns;
    /** Cosmetic holder model, may be {@code null} when BetterModel is absent. */
    private final GrapplingHookModelManager modelManager;
    private final Map<UUID, ActiveGrapple> sessions = new HashMap<>();
    private final Map<String, Sound> soundCache = new ConcurrentHashMap<>();
    /** Live one-tick task; only non-null while at least one session exists. */
    private org.bukkit.scheduler.BukkitTask task;

    public GrapplingHookRuntime(BigCasares plugin, GrapplingHookSettings settings, GrappleCooldownService cooldowns) {
        this(plugin, settings, cooldowns, null);
    }

    public GrapplingHookRuntime(BigCasares plugin, GrapplingHookSettings settings, GrappleCooldownService cooldowns,
                                GrapplingHookModelManager modelManager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.cooldowns = Objects.requireNonNull(cooldowns, "cooldowns");
        this.modelManager = modelManager;
    }

    /** Starts a block-target session that hit. */
    public void launch(Player player, GrapplingHookTier tier, Vector start, Vector target, int travelTicks) {
        launch(player, tier, start, target, travelTicks, null, true);
    }

    /**
     * Starts a new session; a player may only have one at a time. The session
     * always starts on activation so the hook and chain animate immediately.
     *
     * @param targetEntity the entity the hook attached to, or {@code null} for
     *                     a block shot. While it stays alive the live target
     *                     follows it; if it dies or vanishes the session is
     *                     cancelled and cleaned up.
     * @param hit          whether the shot attached to a valid target. A miss
     *                     still flies to the end of the range and animates the
     *                     chain, but applies no pull and no tier cooldown.
     */
    public void launch(Player player, GrapplingHookTier tier, Vector start, Vector target, int travelTicks,
                       Entity targetEntity, boolean hit) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(tier, "tier");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(target, "target");
        if (sessions.containsKey(player.getUniqueId())) {
            return;
        }
        GrappleFeedbackPlan plan = new GrappleFeedbackPlan(settings, tier);
        ActiveGrapple grapple = new ActiveGrapple(player, tier, start, target, travelTicks, settings,
            targetEntity == null ? null : targetEntity.getUniqueId(), hit);
        try {
            grapple.hookDisplay = spawnHook(player.getWorld(), start);
            sessions.put(player.getUniqueId(), grapple);
            ensureTask();
            playSound(player.getWorld(), player.getLocation(), plan.fire());
            if (plan.feedbackEnabled()) {
                spawnBurst(player.getWorld(), start.toLocation(player.getWorld()), Particle.CRIT,
                    plan.particleCount());
            }
        } catch (RuntimeException failure) {
            grapple.cleanup();
            sessions.remove(player.getUniqueId());
            plugin.getLogger().log(Level.SEVERE, "No se pudo lanzar el Grappling Hook", failure);
        }
    }

    /**
     * Starts retracting the active session's hook back to its owner. Returns
     * {@code false} when there is no active session; a repeated activation can
     * never create a second session.
     */
    public boolean retract(UUID playerId) {
        ActiveGrapple grapple = playerId == null ? null : sessions.get(playerId);
        if (grapple == null) {
            return false;
        }
        grapple.retracted = true;
        grapple.session.beginRetract();
        return true;
    }

    public boolean hasActiveSession(UUID playerId) {
        return playerId != null && sessions.containsKey(playerId);
    }

    public int activeSessionCount() {
        return sessions.size();
    }

    @Override
    public void run() {
        Iterator<Map.Entry<UUID, ActiveGrapple>> iterator = sessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveGrapple> entry = iterator.next();
            ActiveGrapple grapple = entry.getValue();
            SessionEnd end;
            try {
                end = advance(grapple);
            } catch (RuntimeException failure) {
                plugin.getLogger().log(Level.WARNING, "Se canceló una sesión del Grappling Hook", failure);
                end = SessionEnd.CANCELLED;
            }
            if (end != SessionEnd.CONTINUE) {
                iterator.remove();
                grapple.cleanup();
                // The hook came back (arrival or miss): play the reload
                // one-shot as the retraction visual. An explicit retract
                // already played it when it started, so it is not repeated.
                if (end == SessionEnd.RETURNED && !grapple.retracted && modelManager != null) {
                    Player owner = plugin.getServer().getPlayer(grapple.ownerId);
                    if (owner != null && owner.isOnline() && !owner.isDead()) {
                        modelManager.playReload(owner);
                    }
                }
            }
        }
        if (sessions.isEmpty() && task != null) {
            task.cancel();
            task = null;
        }
    }

    @Override
    public void close() {
        for (ActiveGrapple grapple : sessions.values()) {
            grapple.cleanup();
        }
        sessions.clear();
        cooldowns.clear();
        soundCache.clear();
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /** Starts the one-tick task on the first launch of the module lifetime. */
    private void ensureTask() {
        if (task == null || task.isCancelled()) {
            task = plugin.getServer().getScheduler().runTaskTimer(plugin, this, 1L, 1L);
        }
    }

    private SessionEnd advance(ActiveGrapple grapple) {
        World world = plugin.getServer().getWorld(grapple.worldId);
        if (world == null) {
            return SessionEnd.CANCELLED;
        }
        Player player = plugin.getServer().getPlayer(grapple.ownerId);
        if (player == null || !player.isOnline() || player.isDead()
            || !grapple.worldId.equals(player.getWorld().getUID())) {
            return SessionEnd.CANCELLED;
        }
        Vector anchor = player.getEyeLocation().toVector();
        Entity targetEntity = resolveTarget(grapple, world);
        if (targetEntity == null && grapple.entityId != null) {
            // The hooked entity died or vanished: cancel and clean up quietly.
            return SessionEnd.CANCELLED;
        }
        Vector liveTarget = targetEntity != null ? attachPoint(targetEntity) : grapple.session.target();
        GrappleTick tick;
        try {
            tick = grapple.session.advance(anchor, liveTarget);
        } catch (RuntimeException failure) {
            plugin.getLogger().log(Level.SEVERE, "Falló la simulación del Grappling Hook", failure);
            return SessionEnd.CANCELLED;
        }
        GrappleFeedbackPlan plan = grapple.plan;

        boolean attachMoment = grapple.lastPhaseWasTravel && tick.phase() == Phase.ATTACH;
        grapple.lastPhaseWasTravel = tick.phase() == Phase.TRAVEL;

        boolean attachEntry = tick.impulseVelocity() != null && grapple.hit;
        boolean attached = tick.phase() == Phase.ATTACH && grapple.hit;
        GrapplingHookSettings.TierTuning tuning = settings.tuningFor(grapple.session.tier());
        GrapplingHookMovement movement = settings.movement();

        if (attachEntry) {
            // The shot was consumed: tier cooldown and pull feedback start once.
            long cooldownMillis = (long) (tuning.cooldownSeconds() * 1000.0);
            cooldowns.start(grapple.ownerId, cooldownMillis, System.currentTimeMillis());
            if (player != null && player.isOnline()) {
                player.setCooldown(Material.FISHING_ROD, (int) Math.ceil(tuning.cooldownSeconds() * 20.0));
                playSound(player.getWorld(), player.getLocation(), plan.impulse());
                if (plan.feedbackEnabled()) {
                    spawnBurst(player.getWorld(), player.getLocation(), Particle.CLOUD, plan.particleCount());
                }
            }
        }
        if (attached) {
            boolean farEnough = anchor.distanceSquared(liveTarget) >= MIN_PULL_DISTANCE * MIN_PULL_DISTANCE;
            if (grapple.entityId != null) {
                // Attract the hooked entity toward the player every attach
                // tick; the chain follows it until it arrives.
                if (targetEntity != null && farEnough) {
                    targetEntity.setVelocity(
                        GrappleShotMath.sustainVelocity(liveTarget, anchor, entityPullPower(tuning)));
                }
            } else if (attachEntry) {
                // Block shot: strong launch burst once, while the chain is tense.
                if (player != null && player.isOnline() && !player.isDead() && farEnough) {
                    player.setVelocity(controlledPlayerVelocity(player, grapple, tick.impulseVelocity(), movement));
                }
            } else {
                // Block shot: reel toward the anchor while preserving swing and player input.
                if (player != null && player.isOnline() && !player.isDead() && farEnough) {
                    Vector reel = GrappleShotMath.sustainVelocity(anchor, liveTarget,
                        sustainPower(tuning, movement));
                    player.setVelocity(controlledPlayerVelocity(player, grapple, reel, movement));
                }
            }
        }

        grapple.hookDisplay.teleport(toLocation(world, tick.hookPosition()));
        if (settings.chain().enabled()) {
            updateChain(grapple, world, anchor, tick.chainPositions());
        }
        if (attachMoment && grapple.hit) {
            playSound(world, toLocation(world, tick.hookPosition()), plan.attach());
            if (plan.feedbackEnabled()) {
                spawnBlockBurst(world, toLocation(world, tick.hookPosition()), Material.IRON_BLOCK, plan.particleCount());
            }
        }
        if (tick.phase() == Phase.TRAVEL && plan.soundsEnabled() && settings.chain().enabled()) {
            grapple.ticksSinceChainSound++;
            if (grapple.ticksSinceChainSound >= plan.chainIntervalTicks()) {
                grapple.ticksSinceChainSound = 0;
                playSound(world, toLocation(world, anchor), plan.chain());
            }
        }
        return tick.done() ? SessionEnd.RETURNED : SessionEnd.CONTINUE;
    }

    /** How a session left the runtime this tick. */
    private enum SessionEnd {
        /** The session keeps running. */
        CONTINUE,
        /** The hook returned (arrival, miss or finished retract): play the retraction visual. */
        RETURNED,
        /** The session was cancelled (world gone, target vanished, error): silent cleanup. */
        CANCELLED
    }

    /** Sustained winch pull speed for the player while the chain is tense. */
    private static double sustainPower(
        GrapplingHookSettings.TierTuning tuning,
        GrapplingHookMovement movement
    ) {
        return Math.min(movement.maxReelSpeed(), tuning.impulsePower() * movement.reelPowerMultiplier());
    }

    private static Vector controlledPlayerVelocity(
        Player player,
        ActiveGrapple grapple,
        Vector reel,
        GrapplingHookMovement movement
    ) {
        Input input = player.getCurrentInput();
        boolean jump = grapple.jumps.update(input.isJump(), movement.airJumps());
        return GrappleMovement.controlledVelocity(
            player.getVelocity(), reel, player.getLocation().getYaw(),
            input.isForward(), input.isBackward(), input.isLeft(), input.isRight(), jump,
            movement.airControlAcceleration(), movement.maxHorizontalSpeed(), movement.jumpVelocity()
        );
    }

    /** Attraction speed for a hooked entity (yank, never launch). */
    private static double entityPullPower(GrapplingHookSettings.TierTuning tuning) {
        return Math.min(MAX_ENTITY_PULL_SPEED, tuning.impulsePower() * ENTITY_PULL_FRACTION);
    }

    private static Entity resolveTarget(ActiveGrapple grapple, World world) {
        if (grapple.entityId == null) {
            return null;
        }
        Entity entity = world.getEntity(grapple.entityId);
        return entity != null && entity.isValid() && !entity.isDead() ? entity : null;
    }

    private static Vector attachPoint(Entity target) {
        return target.getLocation().add(0.0, target.getHeight() * 0.5, 0.0).toVector();
    }

    private void updateChain(ActiveGrapple grapple, World world, Vector anchor, List<Vector> positions) {
        while (grapple.chainDisplays.size() < positions.size()) {
            int index = grapple.chainDisplays.size();
            grapple.chainDisplays.add(spawnChain(world, positions.get(index)));
        }
        while (grapple.chainDisplays.size() > positions.size()) {
            BlockDisplay extra = grapple.chainDisplays.remove(grapple.chainDisplays.size() - 1);
            if (extra.isValid()) {
                extra.remove();
            }
        }
        Vector previous = anchor;
        for (int index = 0; index < positions.size(); index++) {
            Vector next = positions.get(index);
            BlockDisplay display = grapple.chainDisplays.get(index);
            display.setTransformationMatrix(GrappleShotMath.chainTransform(previous, next));
            display.teleport(toLocation(world, GrappleShotMath.lerp(previous, next, 0.5)));
            previous = next;
        }
    }

    private void playSound(World world, Location location, SoundTuning tuning) {
        if (tuning == null || tuning.sound().isBlank()) {
            return;
        }
        Sound sound = soundCache.computeIfAbsent(tuning.sound(), GrapplingHookRuntime::resolveSound);
        if (sound == null) {
            return;
        }
        world.playSound(location, sound, tuning.volume(), tuning.pitch());
    }

    private static Sound resolveSound(String name) {
        NamespacedKey direct = NamespacedKey.fromString(name.toLowerCase(java.util.Locale.ROOT));
        if (direct != null) {
            Sound sound = Registry.SOUNDS.get(direct);
            if (sound != null) {
                return sound;
            }
        }
        String legacy = name.toUpperCase(java.util.Locale.ROOT);
        return Registry.SOUNDS.keyStream()
            .filter(key -> key.getKey().toUpperCase(java.util.Locale.ROOT).replace('.', '_').equals(legacy))
            .findFirst()
            .map(Registry.SOUNDS::get)
            .orElse(null);
    }

    private static void spawnBurst(World world, Location location, Particle particle, int count) {
        world.spawnParticle(particle, location, count, 0.3, 0.3, 0.3, 0.02);
    }

    private static void spawnBlockBurst(World world, Location location, Material material, int count) {
        BlockData data = material.createBlockData();
        world.spawnParticle(Particle.BLOCK, location, count, 0.15, 0.15, 0.15, 0.0, data);
    }

    private static BlockDisplay spawnHook(World world, Vector position) {
        return world.spawn(toLocation(world, position), BlockDisplay.class, display -> {
            display.setBlock(Material.TRIPWIRE_HOOK.createBlockData());
            display.setGravity(false);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setViewRange(1.5f);
            display.setTeleportDuration(1);
            display.setTransformationMatrix(new Matrix4f().translate(-0.5f, -0.5f, -0.5f));
        });
    }

    private static BlockDisplay spawnChain(World world, Vector position) {
        return world.spawn(toLocation(world, position), BlockDisplay.class, display -> {
            display.setBlock(Material.IRON_CHAIN.createBlockData());
            display.setGravity(false);
            display.setPersistent(false);
            display.setInvulnerable(true);
            // Vanilla block resources remain overridable by client packs; use ambient lighting.
            display.setViewRange(1.5f);
            display.setTeleportDuration(1);
        });
    }

    private static Location toLocation(World world, Vector vector) {
        return new Location(world, vector.getX(), vector.getY(), vector.getZ());
    }

    private static final class ActiveGrapple {

        private final UUID ownerId;
        private final UUID worldId;
        private final UUID entityId;
        private final boolean hit;
        private final GrappleSimulation.GrappleSession session;
        private final GrappleFeedbackPlan plan;
        private final List<BlockDisplay> chainDisplays = new ArrayList<>();
        private BlockDisplay hookDisplay;
        private boolean lastPhaseWasTravel = true;
        private final GrappleJumpState jumps = new GrappleJumpState();
        private int ticksSinceChainSound;
        /** True when the holder re-activated mid-session (the retraction one-shot already played). */
        private boolean retracted;

        private ActiveGrapple(
            Player owner,
            GrapplingHookTier tier,
            Vector start,
            Vector target,
            int travelTicks,
            GrapplingHookSettings settings,
            UUID entityId,
            boolean hit
        ) {
            this.ownerId = owner.getUniqueId();
            this.worldId = Objects.requireNonNull(owner.getWorld(), "owner world").getUID();
            this.entityId = entityId;
            this.hit = hit;
            GrapplingHookSettings.TierTuning tuning = settings.tuningFor(tier);
            this.session = new GrappleSimulation.GrappleSession(
                ownerId,
                tier,
                start,
                target,
                travelTicks,
                settings.attachTicks(),
                settings.chain().spacing(),
                tuning.impulsePower(),
                tuning.upwardBias(),
                GrapplingHookSettings.MAX_IMPULSE_SPEED,
                hit,
                settings.movement().retractSpeedMultiplier()
            );
            this.plan = new GrappleFeedbackPlan(settings, tier);
        }

        private void cleanup() {
            if (hookDisplay != null && hookDisplay.isValid()) {
                hookDisplay.remove();
            }
            for (BlockDisplay display : chainDisplays) {
                if (display.isValid()) {
                    display.remove();
                }
            }
            chainDisplays.clear();
        }
    }
}
