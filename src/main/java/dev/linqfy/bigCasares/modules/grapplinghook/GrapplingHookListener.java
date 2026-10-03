package dev.linqfy.bigCasares.modules.grapplinghook;

import dev.linqfy.bigCasares.items.CustomItemRegistry;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.Objects;
import java.util.Optional;

/**
 * Activation and targeting handler.
 *
 * <p>Activation is configurable ({@code activation.mode}): right click, left
 * click, the sneaking variants, or the vanilla offhand-swap key (F) through
 * {@link PlayerSwapHandItemsEvent}. A single input produces exactly one
 * session: the event is cancelled only when the configured mode matches and
 * the Grappling Hook is equipped, and re-activating while a session is active
 * retracts it instead of firing a second shot.
 *
 * <p>Targeting is always from the player's eye along the full camera direction
 * (X, Y and Z preserved). A deterministic voxel traversal finds the first
 * solid block up to the tier range, entities are tested with ray-vs-AABB, and
 * the nearest valid target wins — a wall in front of an entity always wins
 * and an entity behind a wall can never be grabbed. A session always starts
 * on activation, so the hook and chain animate immediately; a miss flies to
 * the end of the range and cleans up.
 */
public final class GrapplingHookListener implements Listener {

    /** Entity hit-box expansion so hooks are slightly forgiving. */
    private static final double ENTITY_RAY_SIZE = 0.5;

    /**
     * Two events for the same press (interact + cast) arrive in the same
     * server tick; anything within this window is considered the same click.
     * A second, deliberate press always lands at least one tick later, so it
     * is never swallowed.
     */
    private static final long SAME_CLICK_WINDOW_MILLIS = 50L;

    private final CustomItemRegistry registry;
    private final GrapplingHookSettings settings;
    private final GrappleCooldownService cooldowns;
    private final GrapplingHookRuntime runtime;
    private final GrapplingHookModelManager modelManager;

    private final GrapplingHookInputGate inputGate = new GrapplingHookInputGate(SAME_CLICK_WINDOW_MILLIS);

    public GrapplingHookListener(
        CustomItemRegistry registry,
        GrapplingHookSettings settings,
        GrappleCooldownService cooldowns,
        GrapplingHookRuntime runtime
    ) {
        this(registry, settings, cooldowns, runtime, null);
    }

    public GrapplingHookListener(
        CustomItemRegistry registry,
        GrapplingHookSettings settings,
        GrappleCooldownService cooldowns,
        GrapplingHookRuntime runtime,
        GrapplingHookModelManager modelManager
    ) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.cooldowns = Objects.requireNonNull(cooldowns, "cooldowns");
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.modelManager = modelManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        GrapplingHookActivationMode.InteractionKind kind = switch (event.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> GrapplingHookActivationMode.InteractionKind.RIGHT_CLICK;
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> GrapplingHookActivationMode.InteractionKind.LEFT_CLICK;
            default -> null;
        };
        if (kind == null) {
            return;
        }
        Player player = event.getPlayer();
        if (!settings.activation().mode().matches(kind, player.isSneaking())) {
            return;
        }
        Optional<GrapplingHookHandResolver.Selection> selected = resolve(player, event.getHand());
        if (selected.isEmpty()) {
            return;
        }
        // Only cancel when this input actually activated the hook; any other
        // use of the held item (or any player without the hook) is untouched.
        event.setCancelled(true);
        long now = System.currentTimeMillis();
        if (inputGate.tryAcquire(player.getUniqueId(), now)) {
            activate(player, selected.get().tier(), now);
        }
    }

    /**
     * The Grappling Hook is a fishing-rod item visually, so the vanilla game
     * wants to cast a fishing line when the rod is used in the air. That line
     * (the "caña") must never appear: whenever the player is holding the hook
     * in the main hand, the cast is cancelled and the line entity is removed.
     * Blocking only the interact event is not enough because the air-use path
     * still spawns the line.
     *
     * <p>The cast is also the activation signal for air clicks: in this Paper
     * version using a rod in the air fires {@link PlayerFishEvent} but not
     * {@code PlayerInteractEvent} (which only fires when a block is in reach).
     * So a right-click mode must start its session here too; without it the
     * hook only ever worked within the 5-block interaction range.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.FISHING) {
            return;
        }
        Player player = event.getPlayer();
        Optional<GrapplingHookHandResolver.Selection> selected = resolve(player, event.getHand());
        if (selected.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        // The vanilla line entity may have spawned before the cancel; drop it
        // so nothing visual is left behind.
        Entity hook = event.getHook();
        if (hook != null && hook.isValid()) {
            hook.remove();
        }
        GrapplingHookActivationMode mode = settings.activation().mode();
        if (!mode.matches(GrapplingHookActivationMode.InteractionKind.RIGHT_CLICK, player.isSneaking())) {
            return;
        }
        // A block click in reach is already handled by the interact event
        // (which also cancels the cast); skip it here so one press never
        // produces two sessions.
        long now = System.currentTimeMillis();
        if (inputGate.tryAcquire(player.getUniqueId(), now)) {
            activate(player, selected.get().tier(), now);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (settings.activation().mode() != GrapplingHookActivationMode.SWAP_HANDS) {
            return;
        }
        Player player = event.getPlayer();
        Optional<GrapplingHookHandResolver.Selection> selected = resolve(player, null);
        if (selected.isEmpty()) {
            return;
        }
        // The hook is in hand: consume the input instead of swapping items.
        event.setCancelled(true);
        long now = System.currentTimeMillis();
        if (inputGate.tryAcquire(player.getUniqueId(), now)) {
            activate(player, selected.get().tier(), now);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        inputGate.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Single entry point for every activation input. One input yields at most
     * one action: re-activating during an active session retracts it (when
     * {@code retract-on-activation} is enabled) instead of firing again.
     */
    private void activate(Player player, GrapplingHookTier tier, long now) {
        if (!settings.canUse()) {
            return;
        }
        GrappleFeedbackPlan plan = new GrappleFeedbackPlan(settings, tier);
        if (runtime.hasActiveSession(player.getUniqueId())) {
            if (settings.activation().retractOnActivation()) {
                runtime.retract(player.getUniqueId());
                playReload(player);
            }
            return;
        }
        if (cooldowns.isOnCooldown(player.getUniqueId(), now)) {
            player.sendActionBar("§cGrappling Hook en cooldown");
            playSound(player, plan.cooldown());
            return;
        }

        GrapplingHookSettings.TierTuning tuning = settings.tuningFor(tier);
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection();
        if (direction.lengthSquared() < 1.0e-9) {
            return;
        }
        double range = tuning.range();
        Vector start = eye.toVector();

        // First solid block along the eye direction (full vector, any
        // orientation: floor, wall, ceiling, diagonal).
        GrappleRaycast.VoxelHit blockHit = settings.targeting().blocks()
            ? GrappleRaycast.firstSolid(start, direction, range, (x, y, z) -> isBlockTarget(world, x, y, z))
                .orElse(null)
            : null;
        double blockDistance = blockHit == null ? Double.MAX_VALUE : blockHit.distance();

        // Nearest valid entity along the ray, compared by distance.
        Entity entityTarget = null;
        double entityDistance = Double.MAX_VALUE;
        if (settings.targeting().entities()) {
            BoundingBox scan = BoundingBox.of(start, GrappleShotMath.endOfRange(start, direction, range))
                .expand(ENTITY_RAY_SIZE + 2.0);
            for (Entity candidate : world.getNearbyEntities(scan)) {
                if (!isGrappleTarget(candidate, player, settings.targeting().players())) {
                    continue;
                }
                BoundingBox box = candidate.getBoundingBox().expand(ENTITY_RAY_SIZE);
                Optional<Double> hit = GrappleRaycast.intersectAabb(start, direction, box.getMin(), box.getMax());
                if (hit.isPresent() && hit.get() <= range && hit.get() < entityDistance) {
                    entityDistance = hit.get();
                    entityTarget = candidate;
                }
            }
        }

        boolean hit = true;
        Vector target;
        switch (GrappleShotMath.nearestTarget(
            blockHit != null, blockDistance, entityTarget != null, entityDistance)) {
            case ENTITY -> target = attachPoint(entityTarget);
            case BLOCK -> target = blockHit.entryPoint();
            default -> {
                // Miss: the hook still flies exactly along the look direction
                // to the end of the tier range, then the session cleans up.
                hit = false;
                target = GrappleShotMath.endOfRange(start, direction, range);
            }
        }
        runtime.launch(player, tier, start, target, travelTicks(start, target), entityTarget, hit);
        playShoot(player);
        if (!hit) {
            cooldowns.start(player.getUniqueId(), (long) settings.failCooldownTicks() * 50L, now);
            player.setCooldown(Material.FISHING_ROD, settings.failCooldownTicks());
            playSound(player, plan.fail());
        }
    }

    /**
     * Solid enough to stop the hook: not air, not a liquid (fluids are never
     * targets, matching {@code FluidCollisionMode.NEVER}) and not passable
     * (a wall, floor, ceiling, glass, slab...). Transparent passable blocks
     * like grass or torches are ignored.
     */
    private static boolean isBlockTarget(World world, int x, int y, int z) {
        org.bukkit.block.Block block = world.getBlockAt(x, y, z);
        Material type = block.getType();
        // Fluids are never targets (matching FluidCollisionMode.NEVER).
        if (type.isAir() || type == Material.WATER || type == Material.LAVA) {
            return false;
        }
        return !block.isPassable();
    }

    private boolean isGrappleTarget(Entity entity, Player shooter, boolean allowPlayers) {
        GrappleTargetRules.Kind kind;
        if (entity == null || entity.equals(shooter)) {
            kind = GrappleTargetRules.Kind.SELF;
        } else if (entity instanceof Player) {
            kind = GrappleTargetRules.Kind.PLAYER;
        } else if (entity instanceof Display || entity instanceof Interaction) {
            kind = GrappleTargetRules.Kind.DISPLAY;
        } else if (entity instanceof Projectile) {
            kind = GrappleTargetRules.Kind.PROJECTILE;
        } else if (entity instanceof Marker) {
            kind = GrappleTargetRules.Kind.MARKER;
        } else if (entity instanceof ArmorStand) {
            kind = GrappleTargetRules.Kind.ARMOR_STAND;
        } else if (entity instanceof LivingEntity) {
            kind = GrappleTargetRules.Kind.LIVING;
        } else {
            kind = GrappleTargetRules.Kind.OTHER;
        }
        return GrappleTargetRules.isAllowed(kind, entity.isValid(), entity.isDead(), allowPlayers);
    }

    private static Vector attachPoint(Entity target) {
        return target.getLocation().add(0.0, target.getHeight() * 0.5, 0.0).toVector();
    }

    private int travelTicks(Vector start, Vector target) {
        return GrappleShotMath.travelTicks(start.distance(target), settings.hookSpeed());
    }

    private void playSound(Player player, SoundTuning tuning) {
        if (tuning == null || tuning.sound().isBlank() || !player.isOnline()) {
            return;
        }
        Sound sound = resolveSound(tuning.sound());
        if (sound != null) {
            player.playSound(player.getLocation(), sound, tuning.volume(), tuning.pitch());
        }
    }

    private Optional<GrapplingHookHandResolver.Selection> resolve(Player player, EquipmentSlot eventHand) {
        String main = registry.resolveItemId(player.getInventory().getItemInMainHand()).orElse(null);
        String off = registry.resolveItemId(player.getInventory().getItemInOffHand()).orElse(null);
        return GrapplingHookHandResolver.select(main, off, eventHand);
    }

    private static Sound resolveSound(String configuredName) {
        NamespacedKey direct = NamespacedKey.fromString(configuredName.toLowerCase(java.util.Locale.ROOT));
        if (direct != null) {
            Sound directSound = Registry.SOUNDS.get(direct);
            if (directSound != null) {
                return directSound;
            }
        }
        String legacy = configuredName.toUpperCase(java.util.Locale.ROOT);
        return Registry.SOUNDS.keyStream()
            .filter(key -> key.getKey().toUpperCase(java.util.Locale.ROOT).replace('.', '_').equals(legacy))
            .findFirst()
            .map(Registry.SOUNDS::get)
            .orElse(null);
    }

    private void playShoot(Player player) {
        if (modelManager != null) {
            modelManager.playShoot(player);
        }
    }

    private void playReload(Player player) {
        if (modelManager != null) {
            modelManager.playReload(player);
        }
    }
}
