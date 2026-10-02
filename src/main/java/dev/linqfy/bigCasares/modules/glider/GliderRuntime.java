package dev.linqfy.bigCasares.modules.glider;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.items.CustomItemRegistry;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;

/** One scheduler driver for every active Glider session. */
public final class GliderRuntime implements Runnable, AutoCloseable {

    private static final int MIN_TICKS_FOR_LANDING_PROTECTION = 5;
    private static final long LANDING_PROTECTION_TICKS = 10L;

    private final BigCasares plugin;
    private final CustomItemRegistry registry;
    private final GliderSettings settings;
    private final GliderSessionStore sessions = new GliderSessionStore();
    private final GliderBoostCooldown boosts = new GliderBoostCooldown();
    private final GliderVisualController visuals;
    private final GliderGripPoseController gripPoses;
    private final Map<UUID, RecentLanding> recentLandings = new HashMap<>();
    private final Set<UUID> visualFailures = new java.util.HashSet<>();
    private final Set<UUID> gripPoseFailures = new java.util.HashSet<>();
    private final Map<String, Sound> soundCache = new HashMap<>();
    private long currentTick;

    public GliderRuntime(BigCasares plugin, CustomItemRegistry registry, GliderSettings settings) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.visuals = new GliderVisualController(plugin, registry, settings.visuals());
        this.gripPoses = new GliderGripPoseController(plugin);
    }

    @Override
    public void run() {
        currentTick += settings.tickRate();
        if (currentTick % 1_200L < settings.tickRate()) {
            boosts.prune(currentTick);
        }
        recentLandings.entrySet().removeIf(entry -> entry.getValue().expiresAtTick() < currentTick);

        for (GliderSession session : sessions.activeSessions()) {
            Player player = plugin.getServer().getPlayer(session.playerId());
            if (player == null || !player.isOnline()) {
                stop(session.playerId(), GliderStopReason.QUIT);
                continue;
            }
            advance(player, session);
        }

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!sessions.isActive(player.getUniqueId())) {
                tryStart(player);
            }
        }
    }

    public boolean tryBoost(Player player) {
        if (player == null) {
            return false;
        }
        GliderSession session = sessions.find(player.getUniqueId()).orElse(null);
        Optional<GliderHandResolver.Selection> equipped = equippedGlider(player);
        if (session == null || equipped.isEmpty()) {
            return false;
        }
        session.tier(equipped.get().tier());
        GliderTierStats stats = settings.stats(session.tier());
        if (!boosts.tryUse(player.getUniqueId(), currentTick, stats)) {
            return false;
        }
        player.setVelocity(GliderPhysics.boost(player.getVelocity(), player.getEyeLocation().getDirection(), stats));
        session.boosting();
        playSound(player, settings.sounds().boost());
        if (settings.particles().enabled()) {
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 8, 0.25, 0.15, 0.25, 0.04);
        }
        return true;
    }

    public OptionalDouble consumeFallDamageReduction(Player player, double damage) {
        if (player == null || damage <= 0.0) {
            return OptionalDouble.empty();
        }
        UUID playerId = player.getUniqueId();
        GliderSession active = sessions.find(playerId).orElse(null);
        GliderTier tier = null;
        if (active != null && active.glideTicks() >= MIN_TICKS_FOR_LANDING_PROTECTION
            && equippedGlider(player).filter(value -> value.tier() == active.tier()).isPresent()) {
            tier = active.tier();
            sessions.stop(playerId, GliderStopReason.LANDED);
            visuals.hide(playerId);
            gripPoses.hide(playerId);
        } else {
            RecentLanding recent = recentLandings.remove(playerId);
            if (recent != null && recent.expiresAtTick() >= currentTick
                && equippedGlider(player).filter(value -> value.tier() == recent.tier()).isPresent()) {
                tier = recent.tier();
            }
        }
        if (tier == null) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(settings.stats(tier).reduceFallDamage(damage));
    }

    public void stop(UUID playerId, GliderStopReason reason) {
        if (playerId == null) {
            return;
        }
        sessions.stop(playerId, reason);
        visuals.hide(playerId);
        gripPoses.hide(playerId);
        visualFailures.remove(playerId);
        gripPoseFailures.remove(playerId);
        if (reason != GliderStopReason.LANDED) {
            recentLandings.remove(playerId);
        }
    }

    public boolean isGliding(UUID playerId) {
        return playerId != null && sessions.isActive(playerId);
    }

    public int activeSessionCount() {
        return sessions.size();
    }

    public int activeVisualCount() {
        return visuals.size();
    }

    public int activeGripPoseCount() {
        return gripPoses.size();
    }

    @Override
    public void close() {
        sessions.clear();
        boosts.clear();
        recentLandings.clear();
        soundCache.clear();
        visualFailures.clear();
        gripPoseFailures.clear();
        gripPoses.close();
        visuals.close();
    }

    private void tryStart(Player player) {
        Optional<GliderHandResolver.Selection> equipped = equippedGlider(player);
        GliderActivationContext context = activationContext(player, equipped.isPresent());
        if (!GliderActivationPolicy.canStart(context)) {
            return;
        }
        GliderHandResolver.Selection selection = equipped.orElseThrow();
        GliderTier tier = selection.tier();
        sessions.start(player.getUniqueId(), player.getWorld().getUID(), tier, currentTick,
            player.getLocation().getY());
        syncVisual(player, tier);
        syncGripPose(player, selection);
        playSound(player, settings.sounds().start());
    }

    private void advance(Player player, GliderSession session) {
        Optional<GliderHandResolver.Selection> equipped = equippedGlider(player);
        if (equipped.isPresent()) {
            session.tier(equipped.get().tier());
        }
        GliderStopReason invalid = invalidReason(player, session);
        if (invalid != null) {
            finish(player, session, invalid);
            return;
        }

        syncVisual(player, session.tier());
        syncGripPose(player, equipped.orElseThrow());

        GliderTierStats stats = settings.stats(session.tier());
        Vector velocity = GliderPhysics.step(player.getVelocity(), player.getEyeLocation().getDirection(), stats);
        if (player.getLocation().getY() > session.startY() + settings.maxAltitudeGain()) {
            velocity.setY(Math.min(velocity.getY(), -0.08));
        }
        player.setVelocity(velocity);
        session.tick();

        if (settings.particles().enabled()
            && session.glideTicks() % settings.particles().intervalTicks() == 0) {
            Particle particle = session.tier().number() >= 5 ? Particle.END_ROD : Particle.CLOUD;
            int count = session.tier().number() >= 5 ? 2 : 3;
            player.getWorld().spawnParticle(particle, player.getLocation().add(0.0, 0.4, 0.0),
                count, 0.25, 0.08, 0.25, 0.01);
        }
    }

    private GliderStopReason invalidReason(Player player, GliderSession session) {
        Optional<GliderHandResolver.Selection> equipped = equippedGlider(player);
        if (equipped.isEmpty()) {
            return GliderStopReason.ITEM_REMOVED;
        }
        if (!player.getWorld().getUID().equals(session.worldId())) {
            return GliderStopReason.WORLD_CHANGE;
        }
        if (player.isOnGround()) {
            return GliderStopReason.LANDED;
        }
        if (settings.activationMode() == GliderActivationMode.SNEAK && !player.isSneaking()) {
            return GliderStopReason.INPUT_RELEASED;
        }
        if (player.isGliding()) {
            return GliderStopReason.ELYTRA;
        }
        if (player.isSwimming() || player.isInsideVehicle() || player.isDead() || player.isFlying()
            || !settings.allows(player.getGameMode())) {
            return GliderStopReason.INVALID_STATE;
        }
        if (currentTick - session.startTick() >= settings.maxSessionTicks()) {
            return GliderStopReason.TIMEOUT;
        }
        return null;
    }

    private void finish(Player player, GliderSession session, GliderStopReason reason) {
        sessions.stop(session.playerId(), reason);
        visuals.hide(session.playerId());
        gripPoses.hide(session.playerId());
        visualFailures.remove(session.playerId());
        gripPoseFailures.remove(session.playerId());
        if (reason == GliderStopReason.LANDED && session.glideTicks() >= MIN_TICKS_FOR_LANDING_PROTECTION) {
            recentLandings.put(session.playerId(), new RecentLanding(session.tier(), currentTick + LANDING_PROTECTION_TICKS));
            playSound(player, settings.sounds().landing());
        } else {
            recentLandings.remove(session.playerId());
        }
    }

    private GliderActivationContext activationContext(Player player, boolean equippedGlider) {
        return new GliderActivationContext(
            equippedGlider,
            player.isOnGround(),
            player.getVelocity().getY(),
            player.isSneaking(),
            player.isSwimming(),
            player.isInsideVehicle(),
            player.isGliding(),
            player.getGameMode() == org.bukkit.GameMode.SPECTATOR,
            player.isFlying(),
            settings.allows(player.getGameMode()),
            settings.activationMode(),
            settings.minimumFallSpeed()
        );
    }

    private Optional<GliderHandResolver.Selection> equippedGlider(Player player) {
        String main = registry.resolveItemId(player.getInventory().getItemInMainHand()).orElse(null);
        String off = registry.resolveItemId(player.getInventory().getItemInOffHand()).orElse(null);
        return GliderHandResolver.select(main, off);
    }

    private void playSound(Player player, String configuredName) {
        if (!settings.sounds().enabled() || configuredName == null || configuredName.isBlank()) {
            return;
        }
        Sound sound = soundCache.computeIfAbsent(configuredName, GliderRuntime::resolveSound);
        if (sound != null) {
            player.playSound(player.getLocation(), sound, 0.8f, 1.0f);
        }
    }

    private void syncVisual(Player player, GliderTier tier) {
        if (visualFailures.contains(player.getUniqueId())) {
            return;
        }
        try {
            visuals.sync(player, tier);
        } catch (RuntimeException failure) {
            visualFailures.add(player.getUniqueId());
            visuals.hide(player.getUniqueId());
            plugin.getLogger().warning("[Glider] No se pudo mostrar el planeador desplegado para "
                + player.getName() + ": " + failure.getMessage());
        }
    }

    private void syncGripPose(Player player, GliderHandResolver.Selection selection) {
        if (gripPoseFailures.contains(player.getUniqueId())) {
            return;
        }
        try {
            gripPoses.sync(player, selection);
        } catch (RuntimeException failure) {
            gripPoseFailures.add(player.getUniqueId());
            gripPoses.hide(player.getUniqueId());
            plugin.getLogger().warning("[Glider] No se pudo activar la pose de agarre para "
                + player.getName() + ": " + failure.getMessage());
        }
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

    private record RecentLanding(GliderTier tier, long expiresAtTick) {
    }
}
