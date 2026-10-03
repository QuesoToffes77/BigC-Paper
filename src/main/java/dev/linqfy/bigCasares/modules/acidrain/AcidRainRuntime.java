package dev.linqfy.bigCasares.modules.acidrain;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.modules.model.JavaModelGateway;
import dev.linqfy.bigCasares.modules.model.JavaModelHandle;
import dev.linqfy.bigCasares.modules.model.JavaModelKeys;
import dev.linqfy.bigCasares.modules.environment.EnvironmentalVisualService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

final class AcidRainRuntime {
    private static final String BYPASS_PERMISSION = "bigcasares.acidrain.bypass";

    private final BigCasares plugin;
    private final Clock clock;
    private final Random random = new Random();
    private final AcidRainExposureCache exposureCache = new AcidRainExposureCache();
    private final Map<UUID, BossBar> bossBars = new LinkedHashMap<>();
    private final Map<String, WeatherSnapshot> weatherSnapshots = new LinkedHashMap<>();
    private final Map<UUID, AcidRainMobType> spawnedMobs = new LinkedHashMap<>();
    private final Map<UUID, JavaModelHandle> mobModels = new LinkedHashMap<>();
    private final Map<UUID, AcidRainMobAnimation> mobAnimations = new LinkedHashMap<>();
    private final Map<UUID, Long> mobAnimationLocksUntil = new LinkedHashMap<>();
    private AcidRainSettings settings;
    private AcidRainConfigLoadResult configLoad;
    private AcidRainStormService stormService;
    private AcidRainDamageService damageService;
    private AcidRainEnvironmentService environmentService;
    private AcidRainProtectionService protectionService;
    private AcidRainMobSpawnService mobSpawnService;
    private final EnvironmentalVisualService environmentalVisuals;
    private final JavaModelGateway models;
    private AcidRainState lastState = AcidRainState.INACTIVE;
    private long tickCounter;
    private long environmentTicks;
    private long mobParticleTicks;
    private long mobAnimationTicks;
    private long lastWarningMessageEpochSecond = Long.MIN_VALUE;
    private boolean manuallyStoppedPresentation;

    AcidRainRuntime(
        BigCasares plugin,
        AcidRainSettings settings,
        AcidRainConfigLoadResult configLoad,
        Clock clock,
        JavaModelGateway models
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.configLoad = Objects.requireNonNull(configLoad, "configLoad");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.models = Objects.requireNonNull(models, "models");
        this.stormService = new AcidRainStormService(settings, clock);
        this.damageService = new AcidRainDamageService(settings);
        this.environmentService = new AcidRainEnvironmentService(settings.environment());
        this.protectionService = new AcidRainProtectionService(settings.protection());
        this.mobSpawnService = new AcidRainMobSpawnService(settings.mobs());
        this.environmentalVisuals = new EnvironmentalVisualService(settings.feedback().visuals());
    }

    AcidRainStartResult start(AcidRainLevel level) {
        manuallyStoppedPresentation = false;
        AcidRainStartResult result = stormService.startNow(level == null ? settings.defaultLevel() : level, clock.instant());
        if (result.started()) {
            onStateChanged(AcidRainState.INACTIVE, AcidRainState.ACTIVE, stormService.snapshot());
        }
        return result;
    }

    AcidRainTransitionResult stop(String reason) {
        AcidRainTransitionResult result = stormService.stop(clock.instant(), reason);
        mobSpawnService.stop();
        if (result.transitioned()) {
            manuallyStoppedPresentation = true;
            damageService.clear();
            environmentService.resetEvent();
            exposureCache.clear();
            clearBossBars();
            stormService.clearTemporaryState();
            restoreWeather();
        }
        return result;
    }

    void tickLifecycle() {
        Instant now = clock.instant();
        AcidRainState before = stormService.snapshot(now).state();
        if (before == AcidRainState.INACTIVE) {
            AcidRainStartResult automatic = stormService.attemptAutomaticStart(now, random.nextInt(100));
            if (automatic.started()) {
                manuallyStoppedPresentation = false;
            }
        }
        stormService.tick(now);
        AcidRainSnapshot snapshot = stormService.snapshot(now);
        if (before != snapshot.state()) {
            onStateChanged(before, snapshot.state(), snapshot);
        }
        if (snapshot.state() == AcidRainState.WARNING) {
            maybeSendWarning(snapshot, now);
        }
        lastState = snapshot.state();
    }

    void tickDamage() {
        AcidRainSnapshot storm = stormService.snapshot();
        if (storm.state() != AcidRainState.ACTIVE) {
            return;
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            AcidRainExposureSnapshot exposure = exposureSnapshot(player);
            AcidRainDamageDecision decision = damageService.evaluate(exposure, storm, clock.instant());
            if (!decision.applies()) {
                continue;
            }
            player.damage(decision.damage());
            applyLevelEffects(player, storm.level());
            stormService.contaminate(new AcidRainContamination(
                player.getUniqueId().toString(),
                decision.contaminationIntensity(),
                clock.instant().plusSeconds(30)
            ));
        }
    }

    void tickFeedback() {
        tickCounter += 10L;
        AcidRainSnapshot storm = stormService.snapshot();
        if (storm.state() == AcidRainState.INACTIVE || manuallyStoppedPresentation) {
            clearBossBars();
            return;
        }
        updateBossBars(storm);
        if (settings.feedback().actionBar().enabled()
            && tickCounter % settings.feedback().actionBar().intervalTicks() == 0L) {
            updateActionBars(storm);
        }
        if ((storm.state() == AcidRainState.WARNING || storm.state() == AcidRainState.ACTIVE)
            && settings.feedback().particles().enabled()
            && settings.feedback().visuals().enabled()
            && tickCounter % settings.feedback().visuals().intervalTicks() == 0L) {
            spawnParticles(storm);
        }
    }

    void tickEnvironment() {
        AcidRainSnapshot storm = stormService.snapshot();
        if (storm.state() != AcidRainState.ACTIVE || !settings.environment().destruction().canDestroyBlocks()) {
            return;
        }
        AcidRainLevelDestructionSettings levelDestruction = settings.settingsFor(storm.level()).destruction();
        int intervalTicks = Math.max(1, levelDestruction.intervalTicks());
        environmentTicks += 10L;
        if (environmentTicks < intervalTicks) {
            return;
        }
        environmentTicks = environmentTicks % intervalTicks;
        harvestEnvironmentalCandidates(storm, Math.max(0, levelDestruction.candidatesPerCycle()));
        environmentService.processSecond(new BukkitAcidRainBlockAccess(plugin, storm), storm, clock.instant());
    }

    void tickMobs() {
        mobAnimationTicks += 10L;
        AcidRainSnapshot storm = stormService.snapshot();
        syncMobScheduler(storm);
        AcidRainMobCycleDecision decision = mobSpawnService.cycleDecision(storm, activeMobCount(), 10L);
        if (decision.spawnAllowed() && decision.attempts() > 0) {
            spawnMobs(storm, decision.attempts());
        }
        tickMobAnimations();
        tickMobAmbientParticles(storm);
    }

    private void tickMobAnimations() {
        for (UUID entityId : new ArrayList<>(spawnedMobs.keySet())) {
            Entity entity = Bukkit.getEntity(entityId);
            if (!(entity instanceof LivingEntity living) || living.isDead() || !living.isValid()) {
                continue;
            }
            long lockedUntil = mobAnimationLocksUntil.getOrDefault(entityId, 0L);
            if (mobAnimationTicks < lockedUntil) {
                continue;
            }
            var velocity = living.getVelocity();
            double horizontalSpeedSquared = velocity.getX() * velocity.getX() + velocity.getZ() * velocity.getZ();
            setMobLoop(living, AcidRainMobAnimation.loopFor(horizontalSpeedSquared));
        }
    }

    private void tickMobAmbientParticles(AcidRainSnapshot storm) {
        AcidRainMobAmbientSettings ambient = settings.mobs().ambient();
        if (!settings.mobs().enabled() || !ambient.enabled()
            || storm == null || storm.state() != AcidRainState.ACTIVE) {
            mobParticleTicks = 0;
            return;
        }
        mobParticleTicks += 10L;
        if (mobParticleTicks % ambient.intervalTicks() != 0L) {
            return;
        }
        int count = Math.max(0, ambient.count());
        if (count == 0) {
            return;
        }
        Particle particle;
        try {
            particle = Particle.valueOf(ambient.particle().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException failure) {
            particle = Particle.SPORE_BLOSSOM_AIR;
        }
        activeMobCount();
        for (Map.Entry<UUID, AcidRainMobType> entry : spawnedMobs.entrySet()) {
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (entity == null || entity.isDead() || !entity.isValid()) {
                continue;
            }
            entity.getWorld().spawnParticle(
                particle,
                entity.getLocation().add(0.0, 1.0, 0.0),
                count,
                0.3,
                0.4,
                0.3,
                0.0
            );
        }
    }

    private void syncMobScheduler(AcidRainSnapshot storm) {
        boolean shouldRun = settings.mobs().enabled()
            && storm != null
            && storm.state() == AcidRainState.ACTIVE;
        if (shouldRun) {
            mobSpawnService.start();
        } else {
            mobSpawnService.stop();
        }
    }

    private int activeMobCount() {
        spawnedMobs.keySet().removeIf(uuid -> {
            Entity entity = Bukkit.getEntity(uuid);
            return entity == null || entity.isDead() || !entity.isValid();
        });
        pruneMobModels();
        return spawnedMobs.size();
    }

    /**
     * Releases model trackers whose entities are gone. Mobs that survive past
     * the TOXIC phase keep their models (they stay visible vanilla-less mobs),
     * so this map is intentionally NOT cleared on storm end - only here, on
     * death and on module shutdown.
     */
    private void pruneMobModels() {
        mobModels.keySet().removeIf(uuid -> {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity == null || entity.isDead() || !entity.isValid()) {
                JavaModelHandle handle = mobModels.get(uuid);
                if (handle != null) {
                    try {
                        models.close(handle);
                    } catch (RuntimeException ignored) {
                        // Tracker may already be gone with its entity; nothing to do.
                    }
                }
                mobAnimations.remove(uuid);
                mobAnimationLocksUntil.remove(uuid);
                return true;
            }
            return false;
        });
    }

    private void spawnMobs(AcidRainSnapshot storm, int attempts) {
        List<Player> candidates = new ArrayList<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (storm.worlds().isAffected(player.getWorld().getName())) {
                candidates.add(player);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        for (int index = 0; index < attempts; index++) {
            AcidRainMobType type = mobSpawnService.pickType();
            if (type == null) {
                continue;
            }
            Player target = candidates.get(random.nextInt(candidates.size()));
            spawnMobAt(target, type);
        }
    }

    private void spawnMobAt(Player target, AcidRainMobType type) {
        World world = target.getWorld();
        int radius = Math.max(1, settings.mobs().radius());
        Location spawn = findMobSpawnLocation(world, target.getLocation(), radius);
        if (spawn == null) {
            return;
        }
        try {
            LivingEntity entity = (LivingEntity) world.spawnEntity(
                spawn,
                BukkitAcidRainMobFactory.entityTypeFor(type)
            );
            BukkitAcidRainMobFactory.configure(entity, type);
            AcidRainMobIdentity.tag(new BukkitAcidRainMobTagStore(entity.getPersistentDataContainer()), type);
            spawnedMobs.put(entity.getUniqueId(), type);
            attachMobModel(entity, type);
            playMobSpawnEffect(entity.getLocation());
        } catch (RuntimeException failure) {
            plugin.getLogger().warning("[AcidRain] Failed to spawn " + type + ": " + failure.getMessage());
        }
    }

    private Location findMobSpawnLocation(World world, Location center, int radius) {
        for (int attempt = 0; attempt < 8; attempt++) {
            int x = center.getBlockX() + random.nextInt(radius * 2 + 1) - radius;
            int z = center.getBlockZ() + random.nextInt(radius * 2 + 1) - radius;
            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }
            int y = Math.max(world.getMinHeight() + 1,
                Math.min(world.getMaxHeight() - 2, world.getHighestBlockYAt(x, z) + 1));
            if (hasMobClearance(world, x, y, z)) {
                return new Location(world, x + 0.5, y, z + 0.5);
            }
        }
        return null;
    }

    private static boolean hasMobClearance(World world, int x, int y, int z) {
        if (world.getBlockAt(x, y - 1, z).isPassable()) {
            return false;
        }
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                if (!world.getBlockAt(x + offsetX, y, z + offsetZ).isPassable()
                    || !world.getBlockAt(x + offsetX, y + 1, z + offsetZ).isPassable()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Attaches the model dedicated to this mob family and hides the vanilla
     * base entity, so the custom mutant is the mob's only visual. The vanilla hitbox
     * and AI remain untouched (invisible entities stay fully targetable). A
     * failed attach only logs a warning and leaves the vanilla mob visible.
     */
    private void attachMobModel(LivingEntity entity, AcidRainMobType type) {
        try {
            JavaModelHandle handle = models.attach(entity, AcidRainMobModels.modelKeyFor(type));
            models.scale(handle, scaleFor(type));
            mobModels.put(entity.getUniqueId(), handle);
            setMobLoop(entity, AcidRainMobAnimation.IDLE);
        } catch (RuntimeException failure) {
            plugin.getLogger().warning(
                "[AcidRain] Could not attach model to Toxic " + type.label() + ": " + failure.getMessage());
        }
    }

    /**
     * Per-model sizing against the three vanilla hitboxes.
     */
    private static float scaleFor(AcidRainMobType type) {
        return switch (type) {
            case CRAWLER -> 0.95f;
            case BRUTE -> 1.0f;
            case SPITTER -> 0.95f;
        };
    }

    /**
     * Called when a tagged Acid Rain mob dies: releases its model tracker and
     * adds the catalog {@code nitric_acid} drop (no crafting recipe) to the
     * vanilla drop list according to {@code mobs.drops}.
     */
    void onMobDeath(LivingEntity entity, AcidRainMobTagStore store, List<ItemStack> drops) {
        spawnedMobs.remove(entity.getUniqueId());
        releaseMobModel(entity.getUniqueId());
        int amount = AcidRainMobDrops.rollAmount(settings.mobs().drops(), random);
        if (amount <= 0) {
            return;
        }
        try {
            drops.add(plugin.getCustomItemRegistry().createItemStack("nitric_acid", amount));
        } catch (RuntimeException failure) {
            plugin.getLogger().warning(
                "[AcidRain] Could not create nitric acid drop: " + failure.getMessage());
        }
    }

    void onMobAttack(LivingEntity entity) {
        playMobOneShot(entity, AcidRainMobAnimation.ATTACK);
    }

    void onMobHurt(LivingEntity entity) {
        playMobOneShot(entity, AcidRainMobAnimation.HURT);
    }

    private void playMobOneShot(LivingEntity entity, AcidRainMobAnimation animation) {
        UUID entityId = entity.getUniqueId();
        JavaModelHandle handle = mobModels.get(entityId);
        if (handle == null || !animation.oneShot()) {
            return;
        }
        mobAnimationLocksUntil.put(entityId, mobAnimationTicks + animation.lockTicks());
        mobAnimations.put(entityId, animation);
        boolean started;
        try {
            started = models.animateOnce(handle, animation.key(), () -> {
                mobAnimationLocksUntil.remove(entityId);
                mobAnimations.remove(entityId);
            });
        } catch (RuntimeException failure) {
            started = false;
        }
        if (!started) {
            mobAnimationLocksUntil.remove(entityId);
            mobAnimations.remove(entityId);
        }
    }

    private void setMobLoop(LivingEntity entity, AcidRainMobAnimation animation) {
        UUID entityId = entity.getUniqueId();
        if (mobAnimations.get(entityId) == animation) {
            return;
        }
        JavaModelHandle handle = mobModels.get(entityId);
        if (handle == null) {
            return;
        }
        try {
            if (models.animate(handle, animation.key())) {
                mobAnimations.put(entityId, animation);
            }
        } catch (RuntimeException ignored) {
            mobAnimations.remove(entityId);
        }
    }

    private void releaseMobModel(UUID entityId) {
        mobAnimations.remove(entityId);
        mobAnimationLocksUntil.remove(entityId);
        JavaModelHandle handle = mobModels.remove(entityId);
        if (handle != null) {
            try {
                models.close(handle);
            } catch (RuntimeException ignored) {
                // Tracker may already be gone with its entity; nothing to do.
            }
        }
    }

    private void releaseAllMobModels() {
        for (JavaModelHandle handle : new ArrayList<>(mobModels.values())) {
            try {
                models.close(handle);
            } catch (RuntimeException ignored) {
                // Tracker may already be gone with its entity; nothing to do.
            }
        }
        mobModels.clear();
        mobAnimations.clear();
        mobAnimationLocksUntil.clear();
    }

    private void playMobSpawnEffect(Location location) {
        if (!settings.mobs().enabled()) {
            return;
        }
        AcidRainMobSpawnEffectSettings effect = settings.mobs().spawnEffect();
        if (!effect.enabled()) {
            return;
        }
        int particles = Math.max(0, effect.particles());
        if (particles > 0) {
            location.getWorld().spawnParticle(
                Particle.SMOKE,
                location.clone().add(0.0, 0.5, 0.0),
                particles,
                0.4,
                0.4,
                0.4,
                0.02
            );
        }
        AcidRainSoundSettings sound = effect.sound();
        if (sound.enabled()) {
            Sound resolved = resolveSound(sound.sound());
            if (resolved != null) {
                location.getWorld().playSound(location, resolved, sound.volume(), sound.pitch());
            }
        }
    }

    void invalidateExposure(UUID playerId) {
        exposureCache.invalidate(playerId);
    }

    void removePlayer(UUID playerId) {
        exposureCache.invalidate(playerId);
        BossBar bossBar = bossBars.remove(playerId);
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    void updateSettings(AcidRainSettings settings, AcidRainConfigLoadResult load) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.configLoad = Objects.requireNonNull(load, "load");
        stormService.updateSettings(settings);
        damageService.updateSettings(settings);
        environmentService.updateSettings(settings.environment());
        protectionService.updateSettings(settings.protection());
        mobSpawnService.updateSettings(settings.mobs());
        environmentalVisuals.updateSettings(settings.feedback().visuals());
        exposureCache.clear();
        for (BossBar bossBar : bossBars.values()) {
            bossBar.setStyle(parseStyle(settings.feedback().bossBar().style()));
        }
    }

    void markConfigLoad(AcidRainConfigLoadResult load) {
        this.configLoad = load;
    }

    AcidRainSnapshot snapshot() {
        return stormService.snapshot();
    }

    List<String> infoLines(AcidRainConfigLoadResult load) {
        AcidRainSnapshot snapshot = stormService.snapshot();
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.GREEN + "Acid Rain");
        lines.add(ChatColor.GRAY + "State: " + ChatColor.YELLOW + snapshot.state());
        lines.add(ChatColor.GRAY + "Level: " + ChatColor.YELLOW + snapshot.level());
        lines.add(ChatColor.GRAY + "Remaining: " + ChatColor.YELLOW + formatTime(snapshot.remainingSeconds()));
        lines.add(ChatColor.GRAY + "Affected worlds: " + ChatColor.YELLOW + String.join(", ", settings.worlds().enabled()));
        lines.add(ChatColor.GRAY + "Mobs: " + (settings.mobs().enabled()
            ? ChatColor.GREEN + "enabled (all active levels)"
            : ChatColor.RED + "disabled"));
        lines.add(ChatColor.GRAY + "Config: " + (load.valid() ? ChatColor.GREEN + "valid" : ChatColor.RED + "invalid"));
        if (!load.valid()) {
            lines.add(ChatColor.RED + "Errors: " + load.errors().size());
        }
        return lines;
    }

    void shutdown() {
        stormService.shutdown();
        mobSpawnService.stop();
        spawnedMobs.clear();
        releaseAllMobModels();
        damageService.clear();
        environmentService.resetEvent();
        exposureCache.clear();
        clearBossBars();
        restoreWeather();
    }

    private AcidRainExposureSnapshot exposureSnapshot(Player player) {
        boolean bypass = player.hasPermission(BYPASS_PERMISSION)
            || player.getGameMode() == GameMode.CREATIVE
            || player.getGameMode() == GameMode.SPECTATOR;
        boolean exposed = exposureCache.isExposed(player, tickCounter, settings);
        double protection = protectionService.protectionFor(player);
        return new AcidRainExposureSnapshot(
            player.getUniqueId().toString(),
            player.getWorld().getName(),
            player.isOnline(),
            exposed,
            bypass,
            protection
        );
    }

    private void onStateChanged(AcidRainState before, AcidRainState after, AcidRainSnapshot snapshot) {
        if (after == AcidRainState.WARNING) {
            environmentService.resetEvent();
            broadcastMessages(settings.warning().messages());
            playSound(plugin.getServer().getOnlinePlayers(), settings.warning().sound());
            lastWarningMessageEpochSecond = clock.instant().getEpochSecond();
            return;
        }
        if (after == AcidRainState.ACTIVE) {
            manuallyStoppedPresentation = false;
            environmentService.resetEvent();
            environmentTicks = 0;
            applyWeather(snapshot);
            plugin.getServer().broadcastMessage(ChatColor.DARK_GREEN + "[Acid Rain] "
                + ChatColor.GREEN + "La lluvia acida comenzo. Intensidad: " + snapshot.level());
            playSound(affectedPlayers(), settings.feedback().startSound());
            return;
        }
        if (after == AcidRainState.ENDING) {
            mobSpawnService.stop();
            damageService.clear();
            environmentService.resetEvent();
            stormService.clearTemporaryState();
            plugin.getServer().broadcastMessage(ChatColor.YELLOW + "[Acid Rain] La tormenta entra en recuperacion.");
            playSound(affectedPlayers(), settings.feedback().stopSound());
            return;
        }
        if (after == AcidRainState.INACTIVE) {
            mobSpawnService.stop();
            spawnedMobs.clear();
            damageService.clear();
            environmentService.resetEvent();
            stormService.clearTemporaryState();
            clearBossBars();
            restoreWeather();
            manuallyStoppedPresentation = false;
        }
    }

    private void maybeSendWarning(AcidRainSnapshot snapshot, Instant now) {
        int interval = Math.max(1, settings.warning().messageIntervalSeconds());
        long epoch = now.getEpochSecond();
        if (epoch - lastWarningMessageEpochSecond >= interval) {
            broadcastMessages(settings.warning().messages());
            playSound(affectedPlayers(), settings.warning().sound());
            lastWarningMessageEpochSecond = epoch;
        }
    }

    private void broadcastMessages(List<String> messages) {
        for (String message : messages) {
            plugin.getServer().broadcastMessage(color(message));
        }
    }

    private void updateBossBars(AcidRainSnapshot storm) {
        if (!settings.feedback().bossBar().enabled()) {
            clearBossBars();
            return;
        }
        List<UUID> active = new ArrayList<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!settings.worlds().isAffected(player.getWorld().getName())) {
                continue;
            }
            active.add(player.getUniqueId());
            AcidRainState state = storm.state();
            BossBar bossBar = bossBars.computeIfAbsent(player.getUniqueId(), ignored -> {
                BossBar created = Bukkit.createBossBar(
                    "Acid Rain",
                    parseColor(settings.feedback().bossBar().colorFor(state)),
                    parseStyle(settings.feedback().bossBar().style())
                );
                created.addPlayer(player);
                return created;
            });
            if (!bossBar.getPlayers().contains(player)) {
                bossBar.addPlayer(player);
            }
            bossBar.setColor(parseColor(settings.feedback().bossBar().colorFor(state)));
            AcidRainExposureSnapshot exposure = exposureSnapshot(player);
            bossBar.setTitle(bossBarTitle(storm, exposure));
            bossBar.setProgress(progress(storm));
            bossBar.setVisible(true);
        }
        bossBars.keySet().removeIf(playerId -> {
            if (active.contains(playerId)) {
                return false;
            }
            BossBar bar = bossBars.get(playerId);
            if (bar != null) {
                bar.removeAll();
            }
            return true;
        });
    }

    private void updateActionBars(AcidRainSnapshot storm) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!settings.worlds().isAffected(player.getWorld().getName())) {
                continue;
            }
            AcidRainExposureSnapshot exposure = exposureSnapshot(player);
            String status = exposure.exposed() && !exposure.bypass()
                ? ChatColor.RED + "Expuesto"
                : ChatColor.GREEN + "Refugio seguro";
            if (exposure.bypass()) {
                status = ChatColor.AQUA + "Bypass";
            }
            player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                status + ChatColor.GRAY + " | Proteccion: "
                    + Math.round(Math.max(0, Math.min(100, exposure.protectionPercent()))) + "%"
            ));
        }
    }

    private void spawnParticles(AcidRainSnapshot storm) {
        dev.linqfy.bigCasares.modules.environment.EnvironmentalVisualProfile profile =
            AcidRainVisualProfiles.forLevel(storm.level());
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            AcidRainExposureSnapshot exposure = exposureSnapshot(player);
            if (!storm.worlds().isAffected(player.getWorld().getName())) {
                continue;
            }
            if (profile == dev.linqfy.bigCasares.modules.environment.EnvironmentalVisualProfile.ACID_RAIN
                && !exposure.exposed()) {
                continue;
            }
            environmentalVisuals.show(player, profile);
        }
    }

    private String bossBarTitle(AcidRainSnapshot storm, AcidRainExposureSnapshot exposure) {
        String stateLabel = switch (storm.state()) {
            case WARNING -> "ALERTA AMBIENTAL";
            case ACTIVE -> "LLUVIA ACIDA";
            case ENDING -> "RECUPERACION";
            case INACTIVE -> "NORMAL";
        };
        return ChatColor.GREEN + stateLabel
            + ChatColor.GRAY + " | Intensidad: " + ChatColor.YELLOW + storm.level()
            + ChatColor.GRAY + " | Tiempo: " + ChatColor.WHITE + formatTime(storm.remainingSeconds())
            + ChatColor.GRAY + " | Proteccion: " + ChatColor.AQUA
            + Math.round(Math.max(0, Math.min(100, exposure.protectionPercent()))) + "%";
    }

    private double progress(AcidRainSnapshot storm) {
        if (storm.totalSeconds() <= 0) {
            return 1.0;
        }
        return Math.max(0.0, Math.min(1.0, (double) storm.remainingSeconds() / storm.totalSeconds()));
    }

    private void harvestEnvironmentalCandidates(AcidRainSnapshot storm, int levelCandidates) {
        AcidRainDestructionSettings destruction = settings.environment().destruction();
        int radius = Math.max(0, destruction.radius());
        int candidates = Math.max(0, levelCandidates);
        int verticalScanDepth = Math.max(0, destruction.verticalScanDepth());
        if (radius == 0 || candidates == 0) {
            return;
        }
        Set<Material> whitelist = destruction.whitelist();
        AcidRainBlockSafetyPolicy safetyPolicy = AcidRainBlockSafetyPolicy.fromSettings(settings.environment());
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            World world = player.getWorld();
            if (!storm.worlds().isAffected(world.getName())) {
                continue;
            }
            Location origin = player.getLocation();
            int originX = origin.getBlockX();
            int originZ = origin.getBlockZ();
            int originY = origin.getBlockY();
            for (int index = 0; index < candidates; index++) {
                int x = originX + random.nextInt(radius * 2 + 1) - radius;
                int z = originZ + random.nextInt(radius * 2 + 1) - radius;
                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }
                findErosionCandidateY(world, x, z, originY, verticalScanDepth, whitelist, safetyPolicy)
                    .ifPresent(y -> environmentService.enqueue(
                        new AcidRainErosionCandidate(world.getName(), originX, originZ, radius, x, y, z)));
            }
        }
    }

    private java.util.OptionalInt findErosionCandidateY(
        World world,
        int x,
        int z,
        int playerY,
        int verticalScanDepth,
        Set<Material> whitelist,
        AcidRainBlockSafetyPolicy safetyPolicy
    ) {
        int highestY = world.getHighestBlockYAt(x, z);
        int surfaceMinY = Math.max(world.getMinHeight(), highestY - verticalScanDepth);
        for (int y = highestY; y >= surfaceMinY; y--) {
            Material material = world.getBlockAt(x, y, z).getType();
            if (safetyPolicy.canErode(material)) {
                return java.util.OptionalInt.of(y);
            }
        }

        int localMaxY = Math.min(world.getMaxHeight() - 1, playerY + verticalScanDepth);
        int localMinY = Math.max(world.getMinHeight(), playerY - verticalScanDepth);
        for (int y = localMaxY; y >= localMinY; y--) {
            Material material = world.getBlockAt(x, y, z).getType();
            if (!whitelist.contains(material)) {
                continue;
            }
            if (safetyPolicy.canErode(material)) {
                return java.util.OptionalInt.of(y);
            }
        }
        return java.util.OptionalInt.empty();
    }

    private void applyLevelEffects(Player player, AcidRainLevel level) {
        AcidRainLevelSettings levelSettings = settings.settingsFor(level);
        int durationTicks = Math.max(40, levelSettings.intervalSeconds() * 20);
        for (String raw : levelSettings.effects()) {
            org.bukkit.potion.PotionEffectType type = potionEffect(raw);
            if (type == null) {
                continue;
            }
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(type, durationTicks, 0, true, false, true));
        }
    }

    private void playSound(Collection<? extends Player> players, AcidRainSoundSettings settings) {
        if (!settings.enabled()) {
            return;
        }
        Sound sound = resolveSound(settings.sound());
        if (sound == null) {
            return;
        }
        for (Player player : players) {
            player.playSound(player.getLocation(), sound, settings.volume(), settings.pitch());
        }
    }

    private static org.bukkit.potion.PotionEffectType potionEffect(String raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "POISON" -> org.bukkit.potion.PotionEffectType.POISON;
            case "WEAKNESS" -> org.bukkit.potion.PotionEffectType.WEAKNESS;
            case "SLOWNESS", "SLOW" -> org.bukkit.potion.PotionEffectType.SLOWNESS;
            case "NAUSEA", "CONFUSION" -> org.bukkit.potion.PotionEffectType.NAUSEA;
            case "BLINDNESS" -> org.bukkit.potion.PotionEffectType.BLINDNESS;
            case "WITHER" -> org.bukkit.potion.PotionEffectType.WITHER;
            default -> null;
        };
    }

    private static Sound resolveSound(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String configured = raw.trim();
        NamespacedKey direct = NamespacedKey.fromString(configured.toLowerCase(java.util.Locale.ROOT));
        if (direct != null) {
            Sound sound = Registry.SOUNDS.get(direct);
            if (sound != null) {
                return sound;
            }
        }
        String legacy = configured.toUpperCase(java.util.Locale.ROOT);
        int namespace = legacy.indexOf(':');
        if (namespace >= 0) {
            legacy = legacy.substring(namespace + 1);
        }
        String finalLegacy = legacy;
        return Registry.SOUNDS.keyStream()
            .filter(key -> key.getKey().toUpperCase(java.util.Locale.ROOT).replace('.', '_').equals(finalLegacy))
            .findFirst()
            .map(Registry.SOUNDS::get)
            .orElse(null);
    }

    private Collection<? extends Player> affectedPlayers() {
        return plugin.getServer().getOnlinePlayers().stream()
            .filter(player -> settings.worlds().isAffected(player.getWorld().getName()))
            .toList();
    }

    private void applyWeather(AcidRainSnapshot snapshot) {
        for (World world : plugin.getServer().getWorlds()) {
            if (!snapshot.worlds().isAffected(world.getName())) {
                continue;
            }
            weatherSnapshots.putIfAbsent(world.getName(), new WeatherSnapshot(
                world.hasStorm(),
                world.isThundering(),
                world.getWeatherDuration(),
                world.getThunderDuration()
            ));
            world.setStorm(true);
            world.setWeatherDuration(Math.max(20, snapshot.remainingSeconds() * 20));
            if (snapshot.level() == AcidRainLevel.CHEMICAL) {
                world.setThundering(true);
                world.setThunderDuration(Math.max(20, snapshot.remainingSeconds() * 20));
            }
        }
    }

    private void restoreWeather() {
        for (Map.Entry<String, WeatherSnapshot> entry : weatherSnapshots.entrySet()) {
            World world = plugin.getServer().getWorld(entry.getKey());
            if (world == null) {
                continue;
            }
            WeatherSnapshot snapshot = entry.getValue();
            world.setStorm(snapshot.storm());
            world.setThundering(snapshot.thundering());
            world.setWeatherDuration(snapshot.weatherDuration());
            world.setThunderDuration(snapshot.thunderDuration());
        }
        weatherSnapshots.clear();
    }

    private void clearBossBars() {
        for (BossBar bossBar : bossBars.values()) {
            bossBar.removeAll();
        }
        bossBars.clear();
    }

    private static String color(String raw) {
        return ChatColor.translateAlternateColorCodes('&', raw == null ? "" : raw);
    }

    private static String formatTime(int seconds) {
        int safe = Math.max(0, seconds);
        return "%02d:%02d".formatted(safe / 60, safe % 60);
    }

    private static BarColor parseColor(String raw) {
        try {
            return BarColor.valueOf(raw == null ? "GREEN" : raw.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException failure) {
            return BarColor.GREEN;
        }
    }

    private static BarStyle parseStyle(String raw) {
        try {
            return BarStyle.valueOf(raw == null ? "SEGMENTED_10" : raw.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException failure) {
            return BarStyle.SEGMENTED_10;
        }
    }

    private record WeatherSnapshot(boolean storm, boolean thundering, int weatherDuration, int thunderDuration) {
    }
}
