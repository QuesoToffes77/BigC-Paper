package dev.linqfy.bigCasares.modules.pveboss;

import dev.linqfy.bigCasares.modules.model.JavaModelGateway;
import dev.linqfy.bigCasares.modules.model.JavaModelHandle;
import dev.linqfy.bigCasares.platform.ClientEntityPresentationRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Warden;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class PaperSahurCombatController implements AutoCloseable {
    private static final EnumSet<BossAbilityEffectType> SUPPORTED = EnumSet.of(
        BossAbilityEffectType.SAHUR_BAT_HIT,
        BossAbilityEffectType.SAHUR_STOMP,
        BossAbilityEffectType.SAHUR_CHARGE,
        BossAbilityEffectType.SAHUR_SPIN,
        BossAbilityEffectType.SAHUR_HUNTER_BAT,
        BossAbilityEffectType.SAHUR_SHIELD_BATS,
        BossAbilityEffectType.SAHUR_LAUNCH_BAT
    );
    private static final Particle.DustOptions BROWN_DUST =
        new Particle.DustOptions(Color.fromRGB(115, 78, 48), 1.5f);

    private final JavaPlugin plugin;
    private final UUID instanceId;
    private final Warden boss;
    private final JavaModelHandle bossModel;
    private final JavaModelGateway models;
    private final Supplier<List<Player>> players;
    private final Consumer<String> animateBoss;
    private final SahurOwnedEntities owned;
    private final SahurCombatPressure pressure =
        new SahurCombatPressure(Duration.ofSeconds(2), 4, Duration.ofMillis(500));
    private final SahurChargePlanner chargePlanner = new SahurChargePlanner(8.0, 40.0, 2.25);
    private final NamespacedKey batMarker;
    private BukkitTask movementTask;
    private long tick;
    private long busyUntil;
    private long shieldUntil;
    private long nextPressureSpinTick;
    private boolean walkingAnimationActive;
    private boolean exclusiveMovement;
    private Vector roamDirection = new Vector(1, 0, 0);
    private final List<Instant> recentCriticalHits = new ArrayList<>();

    PaperSahurCombatController(
        JavaPlugin plugin,
        UUID instanceId,
        Warden boss,
        JavaModelHandle bossModel,
        JavaModelGateway models,
        Supplier<List<Player>> players,
        Consumer<String> animateBoss
    ) {
        this.plugin = plugin;
        this.instanceId = instanceId;
        this.boss = boss;
        this.bossModel = bossModel;
        this.models = models;
        this.players = players;
        this.animateBoss = animateBoss;
        this.owned = new SahurOwnedEntities(models);
        this.batMarker = new NamespacedKey(plugin, "sahur_bat_id");
    }

    static EnumSet<BossAbilityEffectType> supportedEffects() {
        return EnumSet.copyOf(SUPPORTED);
    }

    void start() {
        movementTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickMovement, 1L, 1L);
        owned.own(movementTask);
    }

    boolean execute(BossAbilityEffectType effect, java.util.Collection<UUID> targets) {
        if (!SUPPORTED.contains(effect)) return false;
        if ((effect == BossAbilityEffectType.SAHUR_CHARGE || effect == BossAbilityEffectType.SAHUR_SPIN)
            && exclusiveMovement) return true;
        List<Player> resolved = targets.stream()
            .map(Bukkit::getPlayer)
            .filter(java.util.Objects::nonNull)
            .filter(Player::isOnline)
            .toList();
        switch (effect) {
            case SAHUR_BAT_HIT -> batHit(resolved);
            case SAHUR_STOMP -> stomp();
            case SAHUR_CHARGE -> charge();
            case SAHUR_SPIN -> spin();
            case SAHUR_HUNTER_BAT -> hunterBat();
            case SAHUR_SHIELD_BATS -> shieldBats();
            case SAHUR_LAUNCH_BAT -> launchBat(resolved.isEmpty() ? farthestPlayer() : resolved.getFirst());
            default -> throw new IllegalStateException("Unhandled Sahur effect " + effect);
        }
        return true;
    }

    boolean blocks(Projectile projectile) {
        return tick <= shieldUntil
            && projectile.getWorld().equals(boss.getWorld())
            && projectile.getLocation().distanceSquared(boss.getLocation()) <= 25.0;
    }

    void recordCriticalHit(Instant now) {
        recentCriticalHits.add(now);
        recentCriticalHits.removeIf(hit -> hit.isBefore(now.minusSeconds(2)));
    }

    private void tickMovement() {
        tick++;
        if (!boss.isValid() || boss.isDead() || tick < busyUntil) return;
        Instant now = Instant.now();
        int nearby = (int) players.get().stream()
            .filter(player -> player.getLocation().distanceSquared(boss.getLocation()) <= 36.0)
            .count();
        if (tick >= nextPressureSpinTick && pressure.shouldSpin(nearby, recentCriticalHits, now)) {
            recentCriticalHits.clear();
            spin();
            return;
        }
        if (tick % ThreadLocalRandom.current().nextInt(20, 61) == 0) {
            double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2.0);
            roamDirection = new Vector(Math.cos(angle), 0, Math.sin(angle));
        }
        Location next = boss.getLocation().add(roamDirection.clone().multiply(0.27));
        if (next.getBlock().isPassable() && next.clone().add(0, 1, 0).getBlock().isPassable()) {
            faceAndTeleport(next, roamDirection);
            if (!walkingAnimationActive) {
                animateBoss.accept("walk");
                walkingAnimationActive = true;
            }
        } else {
            roamDirection.multiply(-1);
        }
    }

    private void batHit(List<Player> selected) {
        busyUntil = Math.max(busyUntil, tick + 28);
        playBossAnimation("bat_hit");
        int combo = pressure.comboLength(ThreadLocalRandom.current());
        for (int hit = 0; hit < combo; hit++) {
            long delay = hit * 7L;
            schedule(delay, () -> {
                playBossAnimation("bat_hit");
                Vector forward = visualForward();
                for (Player player : players.get()) {
                    Vector offset = player.getLocation().toVector().subtract(boss.getLocation().toVector());
                    double along = offset.dot(forward);
                    double sideways = offset.clone().subtract(forward.clone().multiply(along)).length();
                    if (along >= 0 && along <= 4.5 && sideways <= 2.0 && pressure.mayHit(player.getUniqueId(), Instant.now())) {
                        player.damage(14.0, boss);
                        player.setVelocity(forward.clone().multiply(0.7).setY(0.25));
                    }
                }
            });
        }
    }

    private void stomp() {
        busyUntil = Math.max(busyUntil, tick + 20);
        playBossAnimation("stomp");
        schedule(8L, () -> {
            Location center = boss.getLocation();
            for (int ring = 1; ring <= 8; ring++) {
                double radius = ring;
                for (int point = 0; point < 36; point++) {
                    double angle = point * Math.PI / 18.0;
                    center.getWorld().spawnParticle(Particle.DUST,
                        center.clone().add(Math.cos(angle) * radius, 0.15, Math.sin(angle) * radius),
                        1, 0, 0, 0, 0, BROWN_DUST);
                }
            }
            center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.6f, 0.65f);
            for (Player player : players.get()) {
                if (player.getLocation().distanceSquared(center) <= 64.0) {
                    Vector away = player.getLocation().toVector().subtract(center.toVector()).setY(0).normalize();
                    player.damage(10.0, boss);
                    player.setVelocity(away.multiply(1.65).setY(0.75));
                }
            }
        });
    }

    private void spin() {
        if (exclusiveMovement) return;
        exclusiveMovement = true;
        busyUntil = Math.max(busyUntil, tick + 80);
        nextPressureSpinTick = Math.max(nextPressureSpinTick, tick + 200);
        playBossAnimation("spin_in_place");
        Vector direction = randomDirection();
        runOwnedTimer(new BukkitRunnable() {
            int age;
            @Override public void run() {
                if (!boss.isValid() || age++ >= 80) {
                    exclusiveMovement = false;
                    cancelOwned(this);
                    return;
                }
                if (age % 12 == 0) {
                    Vector replacement = randomDirection();
                    direction.setX(replacement.getX()).setY(0).setZ(replacement.getZ());
                }
                Location next = boss.getLocation().add(direction.clone().multiply(0.48));
                if (!next.getBlock().isPassable()) direction.multiply(-1);
                else faceAndTeleport(next, direction);
                for (Player player : players.get()) {
                    if (player.getLocation().distanceSquared(boss.getLocation()) <= 12.25
                        && pressure.mayHit(player.getUniqueId(), Instant.now())) {
                        player.damage(9.0, boss);
                        player.setVelocity(player.getLocation().toVector()
                            .subtract(boss.getLocation().toVector()).normalize().multiply(0.9).setY(0.3));
                    }
                }
            }
        });
    }

    private void charge() {
        List<SahurChargePlanner.Corridor> corridors = chargeCorridors();
        List<BossPosition> positions = players.get().stream().map(p -> position(p.getLocation())).toList();
        SahurChargePlanner.Corridor corridor = chargePlanner.choose(position(boss.getLocation()), corridors, positions).orElse(null);
        if (corridor == null) return;
        if (exclusiveMovement) return;
        exclusiveMovement = true;
        busyUntil = Math.max(busyUntil, tick + 60);
        playBossAnimation("sprint");
        Vector direction = new Vector(corridor.directionX(), 0, corridor.directionZ()).normalize();
        Set<UUID> mounted = new HashSet<>();
        List<Pig> carriers = new ArrayList<>();
        int maximumTicks = Math.max(1, (int) Math.ceil(corridor.length() / 1.35));
        runOwnedTimer(new BukkitRunnable() {
            int age;
            @Override public void run() {
                if (!boss.isValid() || age++ >= maximumTicks) {
                    impact();
                    cancelOwned(this);
                    return;
                }
                Location next = boss.getLocation().add(direction.clone().multiply(1.35));
                if (!next.getBlock().isPassable() || !next.clone().add(0, 1, 0).getBlock().isPassable()) {
                    impact();
                    cancelOwned(this);
                    return;
                }
                faceAndTeleport(next, direction);
                for (Player player : players.get()) {
                    if (!mounted.contains(player.getUniqueId())
                        && player.getLocation().distanceSquared(boss.getLocation()) <= 5.0) {
                        Pig carrier = boss.getWorld().spawn(boss.getLocation(), Pig.class, pig -> {
                            pig.setAI(false); pig.setInvisible(true); pig.setInvulnerable(true);
                            pig.setSilent(true); pig.setSaddle(true); pig.setGravity(false);
                        });
                        carrier.addPassenger(player);
                        carriers.add(carrier);
                        owned.own(carrier);
                        mounted.add(player.getUniqueId());
                    }
                }
                carriers.forEach(carrier -> carrier.teleport(boss.getLocation().add(0, 0.25, 0)));
            }

            private void impact() {
                exclusiveMovement = false;
                Location hit = boss.getLocation();
                hit.getWorld().playSound(hit, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                hit.getWorld().spawnParticle(Particle.BLOCK, hit.clone().add(0, 1, 0), 80,
                    1.5, 1.5, 1.5, 0.2, hit.getBlock().getBlockData());
                for (Pig carrier : carriers) {
                    for (Entity passenger : new ArrayList<>(carrier.getPassengers())) {
                        carrier.removePassenger(passenger);
                        if (passenger instanceof Player player) {
                            player.damage(18.0, boss);
                            player.setVelocity(direction.clone().multiply(0.9).setY(0.65));
                        }
                    }
                    owned.release(carrier);
                }
            }
        });
    }

    private List<SahurChargePlanner.Corridor> chargeCorridors() {
        List<Vector> directions = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            double angle = i * Math.PI * 2.0 / 24.0;
            directions.add(new Vector(Math.cos(angle), 0, Math.sin(angle)));
        }
        for (Player player : players.get()) {
            Vector toward = player.getLocation().toVector().subtract(boss.getLocation().toVector()).setY(0);
            if (toward.lengthSquared() > 0.01) directions.add(toward.normalize());
        }
        List<SahurChargePlanner.Corridor> result = new ArrayList<>();
        Location origin = boss.getLocation();
        for (Vector direction : directions) {
            boolean solid = false;
            double length = 40.0;
            for (double distance = 8.0; distance <= 40.0; distance += 1.0) {
                Location probe = origin.clone().add(direction.clone().multiply(distance));
                if (!probe.getBlock().isPassable() || !probe.clone().add(0, 1, 0).getBlock().isPassable()) {
                    solid = true;
                    length = distance;
                    break;
                }
            }
            result.add(new SahurChargePlanner.Corridor(direction.getX(), direction.getZ(), length, solid));
        }
        return result;
    }

    private void hunterBat() {
        Player target = farthestPlayer();
        if (target == null) return;
        Location origin = boss.getLocation().add(0, -1, 0);
        Bat bat = spawnModeledBat(origin);
        JavaModelHandle handle = owned.ownModel(bat, "bat_boss");
        models.animate(handle, "rise_type_throw");
        runOwnedTimer(new BukkitRunnable() {
            int age;
            int bounces;
            Player current = target;
            @Override public void run() {
                if (!bat.isValid() || age++ > 180 || bounces >= 6 || current == null) {
                    owned.release(bat); cancelOwned(this); return;
                }
                if (age == 12) models.animate(handle, "spin_type_throw");
                if (age < 12) bat.teleport(bat.getLocation().add(0, 0.18, 0));
                else {
                    Vector toward = current.getEyeLocation().toVector().subtract(bat.getLocation().toVector()).normalize();
                    bat.teleport(bat.getLocation().add(toward.multiply(0.85)));
                    if (bat.getLocation().distanceSquared(current.getLocation()) <= 2.25) {
                        current.damage(8.0, boss);
                        bounces++;
                        Player previous = current;
                        current = players.get().stream()
                            .filter(player -> !player.equals(previous))
                            .max(Comparator.comparingDouble(player -> player.getLocation().distanceSquared(bat.getLocation())))
                            .orElse(null);
                    }
                }
            }
        });
    }

    private void shieldBats() {
        shieldUntil = tick + 100;
        List<Bat> bats = new ArrayList<>();
        List<JavaModelHandle> handles = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Bat bat = spawnModeledBat(boss.getLocation());
            bats.add(bat);
            JavaModelHandle handle = owned.ownModel(bat, "bat_boss");
            handles.add(handle);
            models.animate(handle, "type_shield");
        }
        runOwnedTimer(new BukkitRunnable() {
            int age;
            @Override public void run() {
                if (!boss.isValid() || age++ >= 100) {
                    bats.forEach(owned::release); cancelOwned(this); return;
                }
                Player bowThreat = players.get().stream()
                    .filter(player -> isBow(player.getInventory().getItemInMainHand()))
                    .min(Comparator.comparingDouble(player -> player.getLocation().distanceSquared(boss.getLocation())))
                    .orElseGet(PaperSahurCombatController.this::farthestPlayer);
                Vector forward = bowThreat == null ? visualForward()
                    : bowThreat.getLocation().toVector().subtract(boss.getLocation().toVector()).setY(0).normalize();
                Vector side = new Vector(-forward.getZ(), 0, forward.getX());
                for (int i = 0; i < bats.size(); i++) {
                    Location position = boss.getLocation().add(forward.clone().multiply(2.8))
                        .add(side.clone().multiply((i - 2) * 1.25)).add(0, 1.5, 0);
                    bats.get(i).teleport(position);
                }
            }
        });
    }

    private void launchBat(Player target) {
        if (target == null) return;
        Location below = target.getLocation().clone().add(0, -2, 0);
        Bat bat = spawnModeledBat(below);
        JavaModelHandle handle = owned.ownModel(bat, "bat_boss");
        models.animate(handle, "type_launch_attack");
        runOwnedTimer(new BukkitRunnable() {
            int age;
            @Override public void run() {
                if (!bat.isValid() || age++ >= 30) { owned.release(bat); cancelOwned(this); return; }
                bat.teleport(bat.getLocation().add(0, 0.23, 0));
                if (age == 10 && target.isOnline()
                    && target.getLocation().distanceSquared(bat.getLocation()) <= 16.0) {
                    target.damage(8.0, boss);
                    target.setVelocity(target.getVelocity().setY(2.25));
                }
            }
        });
    }

    private Bat spawnModeledBat(Location location) {
        Bat bat = location.getWorld().spawn(location, Bat.class, entity -> {
            entity.setAI(false); entity.setInvisible(true); entity.setInvulnerable(true);
            entity.setSilent(true); entity.setAwake(true); entity.setGravity(false);
            entity.getPersistentDataContainer().set(batMarker, PersistentDataType.STRING, instanceId.toString());
        });
        ClientEntityPresentationRegistry.register(bat.getUniqueId(), "BAT", "sahur_bat_id");
        owned.own(bat);
        return bat;
    }

    private Player farthestPlayer() {
        return players.get().stream()
            .max(Comparator.comparingDouble(player -> player.getLocation().distanceSquared(boss.getLocation())))
            .orElse(null);
    }

    private static boolean isBow(ItemStack stack) {
        return stack != null && (stack.getType() == Material.BOW || stack.getType() == Material.CROSSBOW);
    }

    private void schedule(long delay, Runnable action) {
        BukkitTask[] holder = new BukkitTask[1];
        holder[0] = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            action.run();
            owned.release(holder[0]);
        }, delay);
        owned.own(holder[0]);
    }

    private void runOwnedTimer(BukkitRunnable runnable) {
        owned.own(runnable.runTaskTimer(plugin, 0L, 1L));
    }

    private void cancelOwned(BukkitRunnable runnable) {
        try { runnable.cancel(); } catch (IllegalStateException ignored) { }
    }

    private void faceAndTeleport(Location location, Vector direction) {
        OptionalInt feetY = SahurMovementGeometry.findFeetY(
            boss.getLocation().getBlockY(),
            y -> !location.getWorld().getBlockAt(location.getBlockX(), y, location.getBlockZ()).isPassable(),
            y -> location.getWorld().getBlockAt(location.getBlockX(), y, location.getBlockZ()).isPassable()
        );
        if (feetY.isEmpty()) return;
        location.setY(feetY.getAsInt());
        location.setDirection(new Vector(
            SahurMovementGeometry.anchorDirectionX(direction.getX()),
            direction.getY(),
            SahurMovementGeometry.anchorDirectionZ(direction.getZ())
        ));
        boss.teleport(location);
    }

    private Vector visualForward() {
        Vector anchorForward = boss.getLocation().getDirection().setY(0);
        if (anchorForward.lengthSquared() < 0.0001) return roamDirection.clone().normalize();
        return anchorForward.multiply(-1).normalize();
    }

    private void playBossAnimation(String animation) {
        walkingAnimationActive = false;
        animateBoss.accept(animation);
    }

    private static Vector randomDirection() {
        double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2.0);
        return new Vector(Math.cos(angle), 0, Math.sin(angle));
    }

    private static BossPosition position(Location location) {
        return new BossPosition(location.getX(), location.getY(), location.getZ());
    }

    @Override
    public void close() {
        exclusiveMovement = false;
        owned.close();
    }
}
