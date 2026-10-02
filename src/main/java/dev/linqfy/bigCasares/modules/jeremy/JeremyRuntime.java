package dev.linqfy.bigCasares.modules.jeremy;

import dev.linqfy.bigCasares.modules.loot.DeathLootGuard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

final class JeremyRuntime {
    private final Plugin plugin;
    private final JeremySettings settings;
    private final JeremyStorage storage;
    private final Clock clock;
    private final JeremyIdentity identity;
    private final JeremySpawner spawner;
    private final JeremyTargetSelector targetSelector;
    private final JeremyPowerCalculator powerCalculator = new JeremyPowerCalculator();
    private final BukkitJeremyPowerReader powerReader = new BukkitJeremyPowerReader();
    private final JeremyCycle cycle;
    private final JeremyLootPolicy lootPolicy;
    private final DeathLootGuard lootGuard = new DeathLootGuard(256);
    private JeremySession session;
    private long tickCounter;
    private UUID ultrasoundDamageTarget;
    private boolean shutdown;

    JeremyRuntime(Plugin plugin, JeremySettings settings, JeremyStorage storage) {
        this(plugin, settings, storage, Clock.systemUTC());
    }

    JeremyRuntime(Plugin plugin, JeremySettings settings, JeremyStorage storage, Clock clock) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.storage = Objects.requireNonNull(storage, "storage");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.identity = new JeremyIdentity(plugin);
        this.spawner = new JeremySpawner(plugin, settings, identity);
        this.targetSelector = new JeremyTargetSelector(settings.targeting(), new java.util.Random());
        this.lootPolicy = new JeremyLootPolicy(settings.loot());

        long now = clock.millis();
        JeremySnapshot snapshot = loadSnapshot().orElseGet(() -> new JeremySnapshot(
            JeremyPhase.RESTING, null, null, now + settings.timing().restMillis()));
        this.cycle = JeremyCycle.restore(settings.timing(), snapshot);
        purgeStaleJeremys();
        recover(now);
        persist();
    }

    void tick() {
        if (shutdown || !settings.enabled()) {
            return;
        }
        tickCounter += 2L;
        switch (cycle.phase()) {
            case RESTING -> tickResting();
            case HUNTING -> tickHunting();
            case CELEBRATING -> tickCelebrating();
        }
    }

    JeremyActionResult start(Player target) {
        if (!settings.enabled()) {
            return JeremyActionResult.failure("Jeremy esta desactivado en la configuracion.");
        }
        if (cycle.phase() != JeremyPhase.RESTING) {
            return JeremyActionResult.failure("Jeremy ya esta " + cycle.phase().name().toLowerCase(Locale.ROOT) + ".");
        }
        if (!eligible(target)) {
            return JeremyActionResult.failure("Ese jugador no es un objetivo elegible.");
        }
        return beginHunt(target, true)
            ? JeremyActionResult.success("Jeremy comenzo a cazar a " + target.getName() + ".")
            : JeremyActionResult.failure("No se encontro una ubicacion segura para Jeremy.");
    }

    JeremyActionResult stop() {
        if (cycle.phase() == JeremyPhase.RESTING) {
            return JeremyActionResult.failure("Jeremy ya esta descansando.");
        }
        finishHunt("manual", true);
        return JeremyActionResult.success("Jeremy fue detenido; comenzo el descanso de una hora.");
    }

    JeremyActionResult resetCooldown() {
        if (cycle.phase() != JeremyPhase.RESTING) {
            return JeremyActionResult.failure("No se puede resetear el cooldown durante una caza.");
        }
        cycle.resetCooldown(clock.millis());
        persist();
        return JeremyActionResult.success("Cooldown de Jeremy reseteado.");
    }

    List<String> statusLines() {
        long remaining = Math.max(0L, cycle.phaseEndsAtMillis() - clock.millis());
        List<String> lines = new ArrayList<>();
        lines.add("§8§lJEREMY");
        lines.add("§7Estado: §f" + cycle.phase().name());
        lines.add("§7Tiempo restante: §f" + formatDuration(remaining));
        if (session != null) {
            Player target = plugin.getServer().getPlayer(session.targetUuid());
            lines.add("§7Objetivo: §f" + (target == null ? session.targetUuid() : target.getName()));
            lines.add("§7Power score: §f" + String.format(Locale.ROOT, "%.1f", session.powerScore()));
            lines.add("§7Daño melee: §f" + String.format(Locale.ROOT, "%.1f", session.meleeDamage()));
            lines.add("§7Ultrasound: §f" + session.ultrasound().phase());
        }
        return List.copyOf(lines);
    }

    JeremyPhase phase() {
        return cycle.phase();
    }

    boolean isJeremy(Entity entity) {
        return identity.isJeremy(entity);
    }

    boolean isCurrentJeremy(Entity entity) {
        return session != null && entity != null && entity.getUniqueId().equals(session.entityUuid()) && identity.isJeremy(entity);
    }

    boolean shouldCancelJeremyAttack(Entity damager, Entity victim) {
        if (!isCurrentJeremy(damager)) {
            return false;
        }
        return cycle.phase() != JeremyPhase.HUNTING
            || !(victim instanceof Player player)
            || !player.getUniqueId().equals(session.targetUuid());
    }

    double meleeDamage(Entity damager, Player victim, double originalDamage) {
        if (!isCurrentJeremy(damager) || session == null || !victim.getUniqueId().equals(session.targetUuid())) {
            return originalDamage;
        }
        return ultrasoundDamageTarget != null && ultrasoundDamageTarget.equals(victim.getUniqueId())
            ? originalDamage
            : session.meleeDamage();
    }

    double incomingDamage(Player attacker, Entity victim, double originalDamage) {
        String helmet = attacker.getInventory().getHelmet() == null
            ? null
            : attacker.getInventory().getHelmet().getType().name();
        return JeremyGoldenHelmetPolicy.damage(
            originalDamage, isCurrentJeremy(victim), helmet, settings.goldenHelmet().damageMultiplier());
    }

    void recordJeremyDamage(Entity damager, Player victim) {
        if (isCurrentJeremy(damager) && session != null && victim.getUniqueId().equals(session.targetUuid())) {
            session.lastJeremyDamageMillis(clock.millis());
        }
    }

    void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (session == null) {
            return;
        }
        if (isCurrentJeremy(entity)) {
            rewardJeremyKill(event);
            session.entityUuid(null);
            finishHunt("jeremy-death", false);
            return;
        }
        if (entity instanceof Player player && player.getUniqueId().equals(session.targetUuid())) {
            long elapsed = clock.millis() - session.lastJeremyDamageMillis();
            if (elapsed >= 0L && elapsed <= settings.timing().killCreditMillis()) {
                beginCelebration();
            } else {
                finishHunt("target-death-other-cause", false);
            }
        }
    }

    void onTargetQuit(Player player) {
        if (isTarget(player)) {
            finishHunt("target-logout", false);
        }
    }

    void onTargetWorldChange(Player player) {
        if (!isTarget(player) || cycle.phase() != JeremyPhase.HUNTING) {
            return;
        }
        if (!settings.targeting().allowsWorld(player.getWorld().getName())) {
            finishHunt("target-world-excluded", false);
            return;
        }
        reposition(player, "world-change");
    }

    void onWorldUnload(World world) {
        Entity jeremy = currentJeremy();
        Player target = currentTarget();
        if (jeremy != null && jeremy.getWorld().equals(world)
            || target != null && target.getWorld().equals(world)) {
            finishHunt("world-unload", false);
        }
    }

    void onEntitiesLoaded(Collection<? extends Entity> entities) {
        for (Entity entity : entities) {
            if (identity.isJeremy(entity) && !isCurrentJeremy(entity)) {
                entity.remove();
            }
        }
    }

    void enforceTarget(org.bukkit.event.entity.EntityTargetLivingEntityEvent event) {
        if (!isCurrentJeremy(event.getEntity())) {
            return;
        }
        if (cycle.phase() == JeremyPhase.CELEBRATING) {
            event.setCancelled(true);
            event.setTarget(null);
            return;
        }
        Player target = currentTarget();
        if (target != null && event.getTarget() != target) {
            event.setCancelled(false);
            event.setTarget(target);
        }
    }

    void shutdown() {
        if (shutdown) {
            return;
        }
        shutdown = true;
        removeCurrentEntity();
        persist();
        session = null;
        ultrasoundDamageTarget = null;
        lootGuard.clear();
    }

    private void rewardJeremyKill(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null
            || !lootGuard.markIfNew(event.getEntity().getUniqueId())) {
            return;
        }
        lootPolicy.roll(true).ifPresent(roll -> {
            addDrop(event, Material.EMERALD, roll.emeralds());
            addDrop(event, Material.GOLD_INGOT, roll.goldIngots());
            addDrop(event, Material.DIAMOND, roll.diamonds());
            event.setDroppedExp(Math.min(10_000, event.getDroppedExp() + roll.experience()));
        });
    }

    private static void addDrop(EntityDeathEvent event, Material material, int amount) {
        if (amount > 0) {
            event.getDrops().add(new ItemStack(material, amount));
        }
    }

    private void tickResting() {
        if (tickCounter % 20L != 0L || !cycle.canStart(clock.millis())) {
            return;
        }
        Optional<UUID> selected = targetSelector.select(
            plugin.getServer().getOnlinePlayers().stream().map(this::candidate).toList(),
            cycle.lastTargetUuid());
        if (selected.isEmpty()) {
            cycle.retryWithoutPlayer(clock.millis());
            debug("No eligible players; retry scheduled");
            persist();
            return;
        }
        Player target = plugin.getServer().getPlayer(selected.get());
        if (target == null || !beginHunt(target, false)) {
            cycle.retryWithoutPlayer(clock.millis());
            persist();
        }
    }

    private void tickHunting() {
        long now = clock.millis();
        if (cycle.advance(now) == JeremyTransition.HUNT_TIMEOUT) {
            Player target = currentTarget();
            if (target != null) {
                target.sendMessage("§7Jeremy perdio tu rastro...");
            }
            finishHunt("hunt-timeout", false);
            return;
        }
        Player target = currentTarget();
        Zombie jeremy = currentJeremyZombie();
        if (target == null || !target.isOnline() || target.isDead()) {
            finishHunt("target-unavailable", false);
            return;
        }
        if (jeremy == null || !jeremy.isValid() || jeremy.isDead()) {
            finishHunt("jeremy-missing", false);
            return;
        }
        if (target.getWorld() != jeremy.getWorld()) {
            onTargetWorldChange(target);
            return;
        }
        if (tickCounter % 10L == 0L && jeremy.getTarget() != target) {
            jeremy.setTarget(target);
        }
        if (tickCounter >= session.nextPowerRecalculationTick()) {
            updatePower(target, false);
        }

        double distance = jeremy.getLocation().distance(target.getLocation());
        if (distance > settings.reposition().distance()) {
            if (session.farSinceTick() < 0L) {
                session.farSinceTick(tickCounter);
            } else if (tickCounter - session.farSinceTick() >= settings.reposition().delayTicks()) {
                reposition(target, "distance");
                return;
            }
        } else {
            session.farSinceTick(-1L);
        }

        if (tickCounter % settings.pathfinding().sampleIntervalTicks() == 0L) {
            Location location = jeremy.getLocation();
            boolean stuck = session.stuckDetector().sample(
                tickCounter, new JeremyPosition(location.getX(), location.getY(), location.getZ()), distance, true);
            if (stuck && settings.ultrasound().enabled()
                && JeremyUltrasoundPolicy.canCharge(true, true, true, true, distance,
                    settings.ultrasound().range(), session.ultrasound().phase() != JeremyUltrasoundPhase.CHARGING)
                && session.ultrasound().beginCharge(tickCounter, true)) {
                playSound(jeremy.getLocation(), settings.sounds().ultrasoundCharge());
                debug("Ultrasound charge started");
            }
        }

        boolean targetInRange = distance <= settings.ultrasound().range();
        if (session.ultrasound().phase() == JeremyUltrasoundPhase.CHARGING) {
            face(jeremy, target.getEyeLocation());
            showCharge(jeremy, target);
        }
        JeremyUltrasoundTransition transition = session.ultrasound().advance(
            tickCounter, session.stuckDetector().stuck(), targetInRange);
        if (transition == JeremyUltrasoundTransition.FIRE) {
            fireUltrasound(jeremy, target);
        }
    }

    private void tickCelebrating() {
        if (cycle.advance(clock.millis()) == JeremyTransition.CELEBRATION_FINISHED) {
            finishCelebration();
            return;
        }
        Zombie jeremy = currentJeremyZombie();
        if (jeremy == null || !jeremy.isValid() || jeremy.isDead()) {
            cycle.finishCelebration(clock.millis());
            session = null;
            persist();
            return;
        }
        float yaw = session.celebrationYaw() + settings.celebration().rotationDegreesPerTick() * 2.0f;
        session.celebrationYaw(yaw);
        jeremy.setRotation(yaw, 0.0f);
        jeremy.setVelocity(new Vector(0, Math.min(0.0, jeremy.getVelocity().getY()), 0));
        if (tickCounter % 10L == 0L) {
            jeremy.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                jeremy.getLocation().add(0, 1, 0), 4, 0.35, 0.4, 0.35, 0.0);
        }
    }

    private boolean beginHunt(Player target, boolean forced) {
        long now = clock.millis();
        if (!forced && !cycle.canStart(now) || forced && cycle.phase() != JeremyPhase.RESTING) {
            return false;
        }
        double power = powerCalculator.calculate(powerReader.read(target));
        Optional<Zombie> spawned = spawner.spawn(target, power);
        if (spawned.isEmpty()) {
            return false;
        }
        boolean started = forced ? cycle.forceStart(target.getUniqueId(), now) : cycle.startHunt(target.getUniqueId(), now);
        if (!started) {
            spawned.get().remove();
            return false;
        }
        session = new JeremySession(
            target.getUniqueId(), spawned.get().getUniqueId(), power, settings.damage().damageFor(power),
            settings.pathfinding(), settings.ultrasound());
        session.nextPowerRecalculationTick(tickCounter + settings.damage().powerRecalculationTicks());
        target.sendMessage("§cJeremy te esta cazando durante "
            + formatDuration(settings.timing().huntMillis()) + ".");
        target.showTitle(Title.title(
            Component.text("JEREMY"),
            Component.text("Te ha elegido."),
            Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(2), Duration.ofMillis(700))));
        playSound(spawned.get().getLocation(), settings.sounds().spawn());
        debug("Hunt started: target=" + target.getName() + " power=" + power
            + " damage=" + session.meleeDamage());
        persist();
        return true;
    }

    private void beginCelebration() {
        if (!settings.celebration().enabled() || settings.celebration().durationTicks() <= 0) {
            finishHunt("target-killed", false);
            return;
        }
        if (!cycle.startCelebration(clock.millis())) {
            return;
        }
        Zombie jeremy = currentJeremyZombie();
        if (jeremy != null) {
            jeremy.setTarget(null);
            jeremy.setAware(false);
            jeremy.setVelocity(new Vector());
            session.celebrationYaw(jeremy.getYaw());
            playSound(jeremy.getLocation(), settings.sounds().celebration());
        }
        session.ultrasound().reset();
        debug("Celebration started");
        persist();
    }

    private void finishCelebration() {
        removeCurrentEntity();
        cycle.finishCelebration(clock.millis());
        session = null;
        persist();
        debug("Celebration ended; rest started");
    }

    private void finishHunt(String reason, boolean messageTarget) {
        Player target = currentTarget();
        if (messageTarget && target != null) {
            target.sendMessage("§7La caza de Jeremy termino.");
        }
        removeCurrentEntity();
        cycle.finishHunt(clock.millis());
        session = null;
        ultrasoundDamageTarget = null;
        persist();
        debug("Hunt ended: " + reason + "; rest started");
    }

    private void reposition(Player target, String reason) {
        removeCurrentEntity();
        Optional<Zombie> replacement = spawner.spawn(target, session.powerScore());
        if (replacement.isEmpty()) {
            finishHunt("reposition-failed-" + reason, false);
            return;
        }
        session.entityUuid(replacement.get().getUniqueId());
        session.farSinceTick(-1L);
        session.ultrasound().reset();
        debug("Jeremy repositioned: " + reason);
    }

    private void updatePower(Player target, boolean updateHealth) {
        double power = powerCalculator.calculate(powerReader.read(target));
        session.powerScore(power);
        session.meleeDamage(settings.damage().damageFor(power));
        session.nextPowerRecalculationTick(tickCounter + settings.damage().powerRecalculationTicks());
        if (updateHealth) {
            Zombie jeremy = currentJeremyZombie();
            if (jeremy != null && jeremy.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null) {
                double health = settings.health().scaledHealth(power);
                jeremy.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(health);
                jeremy.setHealth(Math.min(jeremy.getHealth(), health));
            }
        }
        debug("Power recalculated: " + power + " damage=" + session.meleeDamage());
    }

    private void fireUltrasound(Zombie jeremy, Player target) {
        Location origin = jeremy.getEyeLocation();
        Location destination = target.getEyeLocation();
        Vector delta = destination.toVector().subtract(origin.toVector());
        double distance = delta.length();
        if (distance <= 0.01 || distance > settings.ultrasound().range()) {
            return;
        }
        BeamTrace trace = trace(origin, destination, settings.ultrasound().wallPenetration().maxBlocks() + 1);
        if (!JeremyUltrasoundPolicy.canReach(
            trace.solidBlocks() == 0, trace.solidBlocks(), settings.ultrasound().wallPenetration())) {
            debug("Ultrasound blocked by " + trace.solidBlocks() + " solid blocks");
            return;
        }
        Vector direction = delta.normalize();
        for (double travelled = 0.5; travelled < distance; travelled += 0.75) {
            Location point = origin.clone().add(direction.clone().multiply(travelled));
            jeremy.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0, 0, 0, 0);
        }
        jeremy.getWorld().spawnParticle(Particle.SONIC_BOOM, origin.clone().add(direction), 1);
        playSound(origin, settings.sounds().ultrasoundFire());
        double damage = Math.max(0.0, Math.min(settings.damage().maximum(),
            session.meleeDamage() * settings.ultrasound().damageMultiplierVsMelee()));
        ultrasoundDamageTarget = target.getUniqueId();
        try {
            target.damage(damage, jeremy);
        } finally {
            ultrasoundDamageTarget = null;
        }
        Vector knockback = direction.clone().setY(Math.max(0.12, direction.getY() * 0.25))
            .multiply(settings.ultrasound().knockback());
        target.setVelocity(target.getVelocity().add(knockback));
        debug("Ultrasound fired: damage=" + damage + " blocks=" + trace.solidBlocks());
    }

    private BeamTrace trace(Location origin, Location target, int stopAfterBlocks) {
        Vector delta = target.toVector().subtract(origin.toVector());
        double distance = delta.length();
        Vector direction = delta.normalize();
        Set<BlockCoordinate> solids = new HashSet<>();
        for (double travelled = 0.25; travelled < distance - 0.25; travelled += 0.25) {
            Block block = origin.clone().add(direction.clone().multiply(travelled)).getBlock();
            if (!block.getType().isSolid()) {
                continue;
            }
            solids.add(new BlockCoordinate(block.getX(), block.getY(), block.getZ()));
            if (solids.size() >= stopAfterBlocks) {
                break;
            }
        }
        return new BeamTrace(solids.size());
    }

    private void showCharge(Zombie jeremy, Player target) {
        if (tickCounter % 4L != 0L) {
            return;
        }
        Location origin = jeremy.getEyeLocation();
        Vector direction = target.getEyeLocation().toVector().subtract(origin.toVector());
        if (direction.lengthSquared() <= 0.001) {
            return;
        }
        direction.normalize();
        for (int step = 1; step <= 4; step++) {
            jeremy.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                origin.clone().add(direction.clone().multiply(step * 0.55)), 1, 0.04, 0.04, 0.04, 0.0);
        }
    }

    private void recover(long now) {
        if (cycle.phase() == JeremyPhase.HUNTING) {
            if (cycle.phaseEndsAtMillis() <= now) {
                cycle.finishHunt(cycle.phaseEndsAtMillis());
                return;
            }
            Player target = plugin.getServer().getPlayer(cycle.targetUuid());
            if (target == null || !eligible(target)) {
                cycle.finishHunt(now);
                return;
            }
            double power = powerCalculator.calculate(powerReader.read(target));
            Optional<Zombie> spawned = spawner.spawn(target, power);
            if (spawned.isEmpty()) {
                cycle.finishHunt(now);
                return;
            }
            session = new JeremySession(
                target.getUniqueId(), spawned.get().getUniqueId(), power, settings.damage().damageFor(power),
                settings.pathfinding(), settings.ultrasound());
            session.nextPowerRecalculationTick(settings.damage().powerRecalculationTicks());
            debug("Hunt recovered after reload/restart");
        } else if (cycle.phase() == JeremyPhase.CELEBRATING) {
            cycle.finishCelebration(Math.min(now, cycle.phaseEndsAtMillis()));
        }
    }

    private JeremyTargetCandidate candidate(Player player) {
        return new JeremyTargetCandidate(
            player.getUniqueId(), player.isOnline(), player.isDead(), player.hasMetadata("NPC"),
            player.isValid(), mode(player.getGameMode()), player.getWorld().getName());
    }

    private boolean eligible(Player player) {
        return player != null && targetSelector.eligible(candidate(player));
    }

    private static JeremyPlayerMode mode(GameMode gameMode) {
        return switch (gameMode) {
            case SURVIVAL -> JeremyPlayerMode.SURVIVAL;
            case ADVENTURE -> JeremyPlayerMode.ADVENTURE;
            case CREATIVE -> JeremyPlayerMode.CREATIVE;
            case SPECTATOR -> JeremyPlayerMode.SPECTATOR;
        };
    }

    private Player currentTarget() {
        return session == null ? null : plugin.getServer().getPlayer(session.targetUuid());
    }

    private Entity currentJeremy() {
        return session == null || session.entityUuid() == null
            ? null
            : plugin.getServer().getEntity(session.entityUuid());
    }

    private Zombie currentJeremyZombie() {
        Entity entity = currentJeremy();
        return entity instanceof Zombie zombie && identity.isJeremy(zombie) ? zombie : null;
    }

    private boolean isTarget(Player player) {
        return session != null && player != null && player.getUniqueId().equals(session.targetUuid());
    }

    private void removeCurrentEntity() {
        Entity entity = currentJeremy();
        if (entity != null && entity.isValid()) {
            entity.remove();
        }
        if (session != null) {
            session.entityUuid(null);
        }
    }

    private void purgeStaleJeremys() {
        int removed = 0;
        for (World world : plugin.getServer().getWorlds()) {
            for (Zombie zombie : world.getEntitiesByClass(Zombie.class)) {
                if (identity.isJeremy(zombie)) {
                    zombie.remove();
                    removed++;
                }
            }
        }
        if (removed > 0) {
            plugin.getLogger().info("[Jeremy] Removed stale entities: " + removed);
        }
    }

    private Optional<JeremySnapshot> loadSnapshot() {
        try {
            return storage.load();
        } catch (IOException failure) {
            plugin.getLogger().warning("[Jeremy] Could not load state; starting a safe full rest: " + failure.getMessage());
            return Optional.empty();
        }
    }

    private void persist() {
        try {
            storage.save(cycle.snapshot());
        } catch (IOException failure) {
            plugin.getLogger().warning("[Jeremy] Could not save state: " + failure.getMessage());
        }
    }

    private void playSound(Location location, String configured) {
        if (!settings.sounds().enabled() || location == null || configured == null || configured.isBlank()) {
            return;
        }
        NamespacedKey key = NamespacedKey.fromString(configured.toLowerCase(Locale.ROOT));
        Sound sound = key == null ? null : Registry.SOUNDS.get(key);
        if (sound != null) {
            location.getWorld().playSound(location, sound, settings.sounds().volume(), settings.sounds().pitch());
        }
    }

    private static void face(Zombie jeremy, Location target) {
        Vector direction = target.toVector().subtract(jeremy.getEyeLocation().toVector());
        if (direction.lengthSquared() <= 0.001) {
            return;
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ()));
        float pitch = (float) Math.toDegrees(-Math.atan2(direction.getY(),
            Math.sqrt(direction.getX() * direction.getX() + direction.getZ() * direction.getZ())));
        jeremy.setRotation(yaw, pitch);
    }

    private void debug(String message) {
        if (settings.debug()) {
            plugin.getLogger().info("[Jeremy] " + message);
        }
    }

    private static String formatDuration(long millis) {
        long seconds = Math.max(0L, millis / 1_000L);
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60L, seconds % 60L);
    }

    private record BeamTrace(int solidBlocks) {
    }

    private record BlockCoordinate(int x, int y, int z) {
    }
}

record JeremyActionResult(boolean success, String message) {
    static JeremyActionResult success(String message) {
        return new JeremyActionResult(true, message);
    }

    static JeremyActionResult failure(String message) {
        return new JeremyActionResult(false, message);
    }
}
