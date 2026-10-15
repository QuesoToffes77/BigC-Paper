package dev.linqfy.bigCasares.modules.donpollos;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Ravager;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static dev.linqfy.bigCasares.modules.donpollos.DonPollosBossSupport.*;

/**
 * Ataques del Don Pollo Boss: persecucion, laser de los ojos, meteoritos y gallinas (FASE 2), rayo de warden, mega
 * salto con onda expansiva y Sanders (FASE FINAL). Tambien recuerda que ataque le pego a cada jugador, para el
 * mensaje de muerte.
 */
final class DonPollosBossAttacks {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private static final int LASER_CHARGE_TICKS = 8;
    private static final int LASER_FIRE_TICKS = 28;
    private static final double CAR_TOP = 15.0 / 16;
    private static final double CAR_FRONT = 1.0;
    private static final double METEOR_RANGE = 32;
    private static final int BEAM_CHARGE_TICKS = 40;
    private static final int BEAM_AIM_TICKS = 8;
    private static final double BEAM_INFLATE = 0.14;
    private static final double BEAM_HIT_RADIUS = 1.3;
    private static final int JUMP_TICKS = 30;
    private static final int HOP_TICKS = 16;
    private static final double EYE_LASER_LENGTH = 8;

    private final JavaPlugin plugin;
    private final DonPollosModelBinder binder;
    private final DonPollosBossSettings settings;
    private final DonPollosBossMinions minions;
    private final DonPollosBossMeteors meteors = new DonPollosBossMeteors(player -> blame(player, DeathCause.METEOR));
    private final DonPollosBossShockwaves shockwaves = new DonPollosBossShockwaves(player -> blame(player, DeathCause.SHOCKWAVE));
    /** Ultimo ataque del boss (o de sus bichos) que recibio cada jugador, para el mensaje de muerte. */
    private final Map<UUID, Blame> lastHits = new HashMap<>();

    DonPollosBossAttacks(JavaPlugin plugin, DonPollosModelBinder binder, DonPollosBossSettings settings,
                         DonPollosBossMinions minions) {
        this.plugin = plugin;
        this.binder = binder;
        this.settings = settings;
        this.minions = minions;
    }

    /** Mueve los meteoritos y las ondas expansivas en vuelo. */
    void tickProjectiles() {
        meteors.tick();
        shockwaves.tick();
    }

    void clearProjectiles() {
        meteors.clear();
        shockwaves.clear();
    }

    /** Si el jugador murio por un ataque del boss hace poco, el mensaje de muerte lo dice. */
    void deathMessage(PlayerDeathEvent event) {
        Blame blame = lastHits.remove(event.getPlayer().getUniqueId());
        if (blame == null || Bukkit.getCurrentTick() - blame.tick() > BLAME_TICKS) {
            return;
        }
        event.deathMessage(Component.text()
            .append(event.getPlayer().displayName().colorIfAbsent(net.kyori.adventure.text.format.NamedTextColor.RED))
            .append(LEGACY.deserialize(blame.cause().message()))
            .build());
    }

    void setInflate(Ravager ravager, double amount) {
        AttributeInstance scale = ravager.getAttribute(Attribute.SCALE);
        if (scale == null) {
            return;
        }
        NamespacedKey key = new NamespacedKey(plugin, "boss_inflate");
        scale.removeModifier(key);
        if (amount > 0) {
            scale.addModifier(new org.bukkit.attribute.AttributeModifier(key, amount,
                org.bukkit.attribute.AttributeModifier.Operation.MULTIPLY_SCALAR_1,
                org.bukkit.inventory.EquipmentSlotGroup.ANY));
        }
    }

    /** Laseres violetas finitos de adorno que salen de los ojos (no pegan). */
    void eyeLasers(DonPollosBoss boss, long now) {
        Ravager ravager = boss.ravager;
        Vector forward = ravager.getLocation().getDirection().setY(0);
        if (forward.lengthSquared() < 1e-6) {
            return;
        }
        forward.normalize().rotateAroundY(Math.sin(now / 9.0) * 0.25).setY(-0.15).normalize();
        Particle.DustOptions purple = new Particle.DustOptions(Color.fromRGB(176, 64, 255), 0.45f);
        double length = EYE_LASER_LENGTH * scale(ravager) / settings.scale();
        for (int side : new int[]{1, -1}) {
            Location from = eyes(ravager, side);
            for (double d = 0.15; d <= length; d += 0.22) {
                ravager.getWorld().spawnParticle(Particle.DUST, from.clone().add(forward.clone().multiply(d)), 1,
                    0, 0, 0, 0, purple);
            }
        }
    }

    /** Se mueve a los saltos con el auto mientras te persigue. */
    void hop(DonPollosBoss boss, long now) {
        Ravager ravager = boss.ravager;
        if (now < boss.nextHop || !ravager.isOnGround() || !(ravager.getTarget() instanceof Player target)) {
            return;
        }
        Vector to = target.getLocation().toVector().subtract(ravager.getLocation().toVector()).setY(0);
        if (to.lengthSquared() < 9) {
            return;
        }
        ravager.setVelocity(to.normalize().multiply(0.38).setY(0.42));
        boss.nextHop = now + HOP_TICKS;
        World world = ravager.getWorld();
        world.playSound(ravager.getLocation(), Sound.ENTITY_RAVAGER_STEP, 1.6f, 0.7f);
        Block ground = ravager.getLocation().add(0, -0.5, 0).getBlock();
        if (ground.getType().isSolid()) {
            world.spawnParticle(Particle.BLOCK, ravager.getLocation(), 14, 0.8, 0.1, 0.8, 0, ground.getBlockData());
        }
    }

    /** Se infla y desinfla 2 veces mirandote; despues fija a donde estabas y tira el rayo ahi. */
    void startBeam(DonPollosBoss boss, long now) {
        Ravager ravager = boss.ravager;
        Player target = nearestPlayer(ravager.getLocation(), settings.phase4().beamRange());
        if (target == null) {
            boss.nextBeam = now + 20;
            return;
        }
        boss.beamTarget = target.getUniqueId();
        boss.beamStart = now;
        boss.beamPoint = null;
        ravager.setAI(false);
        ravager.getWorld().playSound(ravager.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 3f, 0.8f);
    }

    void tickBeam(DonPollosBoss boss, long now) {
        Ravager ravager = boss.ravager;
        long t = now - boss.beamStart;
        Player target = boss.beamTarget == null ? null : Bukkit.getPlayer(boss.beamTarget);
        if (t < BEAM_CHARGE_TICKS) {
            // dos infladas: |sen| tiene dos picos en 40 ticks
            setInflate(ravager, BEAM_INFLATE * Math.abs(Math.sin(Math.PI * t / 20.0)));
            if (target != null && target.getWorld() == ravager.getWorld()) {
                faceTowards(ravager, target.getLocation());
            }
            return;
        }
        setInflate(ravager, 0);
        if (boss.beamPoint == null) {
            if (target == null || target.getWorld() != ravager.getWorld()) {
                endBeam(boss, now);
                return;
            }
            // apunta a donde estas AHORA; despues ya no te sigue
            boss.beamPoint = target.getLocation().add(0, 1.0, 0);
            faceTowards(ravager, boss.beamPoint);
            ravager.getWorld().playSound(ravager.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 2f, 0.6f);
        }
        Location from = eyes(ravager, 0);
        Vector direction = boss.beamPoint.toVector().subtract(from.toVector());
        if (direction.lengthSquared() < 1e-6) {
            endBeam(boss, now);
            return;
        }
        direction.normalize();
        if (t < BEAM_CHARGE_TICKS + BEAM_AIM_TICKS) {
            Particle.DustOptions aim = new Particle.DustOptions(Color.fromRGB(40, 220, 220), 0.6f);
            for (double d = 0.5; d <= settings.phase4().beamRange(); d += 1.0) {
                ravager.getWorld().spawnParticle(Particle.DUST, from.clone().add(direction.clone().multiply(d)), 1,
                    0, 0, 0, 0, aim, true);
            }
            return;
        }
        fireBeam(boss, from, direction);
        endBeam(boss, now);
    }

    /** Rayo de warden en linea recta (atraviesa paredes, como el del warden). */
    void fireBeam(DonPollosBoss boss, Location from, Vector direction) {
        Ravager ravager = boss.ravager;
        World world = ravager.getWorld();
        double range = settings.phase4().beamRange();
        world.playSound(from, Sound.ENTITY_WARDEN_SONIC_BOOM, 3f, 0.9f);
        for (double d = 1; d <= range; d += 1.0) {
            world.spawnParticle(Particle.SONIC_BOOM, from.clone().add(direction.clone().multiply(d)), 1, 0, 0, 0, 0,
                null, true);
        }
        org.bukkit.damage.DamageSource source = org.bukkit.damage.DamageSource.builder(
                org.bukkit.damage.DamageType.SONIC_BOOM)
            .withCausingEntity(ravager).withDirectEntity(ravager).build();
        for (Player player : world.getPlayers()) {
            if (!attackable(player)) {
                continue;
            }
            Vector chest = player.getLocation().add(0, 1, 0).toVector().subtract(from.toVector());
            double along = chest.dot(direction);
            if (along < 0 || along > range) {
                continue;
            }
            double off = chest.clone().subtract(direction.clone().multiply(along)).length();
            if (off <= BEAM_HIT_RADIUS) {
                blame(player, DeathCause.BEAM);
                player.damage(boss.difficulty.damage(settings.phase4().beamDamage()), source);
                player.setVelocity(direction.clone().setY(0).multiply(2.0).setY(0.5));
            }
        }
    }

    void endBeam(DonPollosBoss boss, long now) {
        setInflate(boss.ravager, 0);
        boss.beamStart = 0;
        boss.beamPoint = null;
        boss.beamTarget = null;
        boss.ravager.setAI(true);
        boss.nextBeam = now + boss.difficulty.cooldownTicks(settings.phase4().beamCooldownSeconds() * 20);
        boss.nextJump = Math.max(boss.nextJump, now + 40);
    }

    /** Mega salto: cae a {@code jump-land-distance} bloques del jugador y arma la onda expansiva. */
    void startJump(DonPollosBoss boss, long now) {
        Ravager ravager = boss.ravager;
        Player target = nearestPlayer(ravager.getLocation(), CHASE_RANGE);
        if (target == null) {
            boss.nextJump = now + 20;
            return;
        }
        Location from = ravager.getLocation();
        Vector back = from.toVector().subtract(target.getLocation().toVector()).setY(0);
        if (back.lengthSquared() < 0.01) {
            back = new Vector(1, 0, 0).rotateAroundY(ThreadLocalRandom.current().nextDouble(Math.PI * 2));
        }
        Location land = target.getLocation().add(back.normalize().multiply(settings.phase4().jumpLandDistance()));
        land.setY(groundY(land, Math.max(from.getY(), target.getLocation().getY()) + 8, target.getLocation().getY()));
        boss.jumpFrom = from;
        boss.jumpTo = land;
        boss.jumpApex = Math.max(8, from.distance(land) / 2.5);
        boss.jumpStart = now;
        boss.jumpEnd = now + JUMP_TICKS;
        boss.jumpTarget = target.getUniqueId();
        ravager.setAI(false);
        ravager.setGravity(false);
        World world = ravager.getWorld();
        world.playSound(from, Sound.ENTITY_RAVAGER_ROAR, 3f, 0.6f);
        world.playSound(from, Sound.ENTITY_GOAT_LONG_JUMP, 3f, 0.5f);
        world.spawnParticle(Particle.EXPLOSION, from, 4, 1, 0.2, 1, 0, null, true);
    }

    void tickJump(DonPollosBoss boss, long now) {
        Ravager ravager = boss.ravager;
        double t = Math.min(1, (now - boss.jumpStart) / (double) JUMP_TICKS);
        Vector path = boss.jumpTo.toVector().subtract(boss.jumpFrom.toVector());
        Location at = boss.jumpFrom.clone().add(path.multiply(t)).add(0, boss.jumpApex * 4 * t * (1 - t), 0);
        Player target = boss.jumpTarget == null ? null : Bukkit.getPlayer(boss.jumpTarget);
        if (target != null && target.getWorld() == at.getWorld()) {
            Vector look = target.getLocation().toVector().subtract(at.toVector());
            at.setYaw((float) Math.toDegrees(Math.atan2(-look.getX(), look.getZ())));
        } else {
            at.setYaw(ravager.getLocation().getYaw());
        }
        at.setPitch(0);
        ravager.teleport(at);
        ravager.getWorld().spawnParticle(Particle.LARGE_SMOKE, at, 3, 0.5, 0.2, 0.5, 0.01, null, true);
        if (t >= 1) {
            boss.jumpEnd = 0;
            ravager.setGravity(true);
            ravager.setAI(true);
            ravager.setVelocity(new Vector());
            shockwaves.slam(ravager, boss.jumpTo, settings.phase4().shockwaveRadius(),
                boss.difficulty.damage(settings.phase4().shockwaveDamage()));
            boss.nextJump = now + boss.difficulty.cooldownTicks(settings.phase4().jumpCooldownSeconds() * 20);
            boss.nextBeam = Math.max(boss.nextBeam, now + 40);
        }
    }

    /** Invoca Sanders fase final (Coroneles mas rapidos, fuertes y con mas vida) al lado del boss. */
    void spawnFinalColonels(DonPollosBoss boss) {
        Ravager ravager = boss.ravager;
        DonPollosBossPhase4Settings phase4 = settings.phase4();
        long alive = minions.count(boss.minions, kind -> kind == DonPollosBossMinions.Kind.CORONEL_FINAL);
        long count = Math.min(boss.difficulty.count(phase4.colonelsPerWave()), boss.difficulty.count(phase4.maxColonels()) - alive);
        for (int i = 0; i < count; i++) {
            Location location = DonPollosBossMinions.around(ravager.getLocation(), 2 * scale(ravager));
            boss.minions.add(toughen(boss, minions.spawn(DonPollosBossMinions.Kind.CORONEL_FINAL, location,
                nearestPlayer(location, CHASE_RANGE), boss.difficulty.level(), colonel -> {
                    DonPollosBossMinions.setAttribute(colonel, Attribute.MOVEMENT_SPEED, ZOMBIE_SPEED * phase4.colonelSpeed());
                    DonPollosBossMinions.setAttribute(colonel, Attribute.ATTACK_DAMAGE, ZOMBIE_DAMAGE + phase4.colonelDamage());
                    DonPollosBossMinions.setAttribute(colonel, Attribute.MAX_HEALTH, phase4.colonelHealth());
                    colonel.setHealth(phase4.colonelHealth());
                })).getUniqueId());
        }
    }

    /** IA de bebe zombie: va derecho al jugador mas cercano y no lo suelta. */
    void chase(DonPollosBoss boss) {
        Ravager ravager = boss.ravager;
        if (ravager.getTarget() instanceof Player current && current.isValid() && !current.isDead()
            && current.getGameMode() != GameMode.CREATIVE && current.getGameMode() != GameMode.SPECTATOR
            && current.getWorld() == ravager.getWorld()
            && current.getLocation().distanceSquared(ravager.getLocation()) <= CHASE_RANGE * CHASE_RANGE) {
            return;
        }
        ravager.setTarget(nearestPlayer(ravager.getLocation(), CHASE_RANGE));
    }

    /**
     * FASE 2: no se acerca; camina para quedar a {@code keep-distance} bloques del jugador mas cercano,
     * moviendose de costado de a ratos para no quedarse quieto.
     */
    void keepAway(DonPollosBoss boss, long now) {
        Ravager ravager = boss.ravager;
        Player player = nearestPlayer(ravager.getLocation(), CHASE_RANGE);
        if (player == null) {
            return;
        }
        Vector away = ravager.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0);
        if (away.lengthSquared() < 0.01) {
            away = new Vector(1, 0, 0);
        }
        away.normalize().rotateAroundY(Math.sin(now / 50.0) * 0.6);
        Location goal = player.getLocation().add(away.multiply(settings.keepDistance()));
        goal.setY(ravager.getLocation().getY());
        ravager.getPathfinder().moveTo(goal, 1.15);
    }

    void launchMeteor(DonPollosBoss boss, long now) {
        Ravager ravager = boss.ravager;
        Player player = nearestPlayer(ravager.getLocation(), METEOR_RANGE);
        if (player == null) {
            boss.nextMeteor = now + 20;
            return;
        }
        faceTowards(ravager, player.getLocation());
        double scale = scale(ravager);
        Vector forward = player.getLocation().toVector().subtract(ravager.getLocation().toVector()).setY(0);
        if (forward.lengthSquared() > 1e-6) {
            forward.normalize();
        }
        Location from = ravager.getLocation().add(0, CAR_TOP * scale, 0).add(forward.multiply(CAR_FRONT * scale));
        Location target = player.getLocation().add(0, 0.5, 0);
        meteors.launch(ravager, from, target, boss.difficulty.damage(settings.meteorDamage()));
        boss.nextMeteor = now + boss.difficulty.cooldownTicks(settings.meteorCooldownSeconds() * 20);
    }

    /** FASE 2: gallinas WhatsApp al lado del boss. */
    void spawnChickens(DonPollosBoss boss) {
        Ravager ravager = boss.ravager;
        long alive = minions.count(boss.minions, kind -> kind == DonPollosBossMinions.Kind.GALLINA);
        long count = Math.min(boss.difficulty.count(settings.chickensPerWave()), boss.difficulty.count(settings.maxChickens()) - alive);
        for (int i = 0; i < count; i++) {
            Location location = DonPollosBossMinions.around(ravager.getLocation(), 1.6 * scale(ravager));
            boss.minions.add(toughen(boss, minions.spawn(DonPollosBossMinions.Kind.GALLINA, location,
                nearestPlayer(location, CHASE_RANGE), boss.difficulty.level())).getUniqueId());
        }
    }

    void startLaser(DonPollosBoss boss, long now) {
        Ravager ravager = boss.ravager;
        Player target = null;
        double best = settings.laserRange() * settings.laserRange();
        for (Player player : ravager.getWorld().getPlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE || player.isDead()) {
                continue;
            }
            double distance = player.getLocation().distanceSquared(ravager.getLocation());
            if (distance < best && ravager.hasLineOfSight(player)) {
                best = distance;
                target = player;
            }
        }
        if (target == null) {
            boss.nextLaser = now + 40;
            return;
        }
        boss.target = target.getUniqueId();
        boss.chargeEnd = now + LASER_CHARGE_TICKS;
        boss.firingUntil = now + LASER_CHARGE_TICKS + LASER_FIRE_TICKS;
        ravager.setAI(false);
        binder.playOnce(ravager.getUniqueId(), "laser", () -> { });
        ravager.getWorld().playSound(ravager.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2f, 1.6f);
    }

    void fireLaser(DonPollosBoss boss, long now) {
        Ravager ravager = boss.ravager;
        Player target = boss.target == null ? null : Bukkit.getPlayer(boss.target);
        if (now >= boss.firingUntil || target == null || !target.isValid() || target.getWorld() != ravager.getWorld()) {
            endLaser(boss, now);
            return;
        }
        faceTowards(ravager, target.getLocation());
        if (now < boss.chargeEnd) {
            Location eyes = eyes(ravager, 0);
            ravager.getWorld().spawnParticle(Particle.DUST, eyes, 4, 0.15, 0.05, 0.15, 0,
                new Particle.DustOptions(Color.RED, 1.2f));
            return;
        }
        if ((now - boss.chargeEnd) % 6 == 0) {
            ravager.getWorld().playSound(ravager.getLocation(), Sound.ENTITY_GUARDIAN_ATTACK, 1.5f, 1.8f);
        }
        Location aim = target.getLocation().add(0, 1.1, 0);
        List<Player> hitsPerEye = new ArrayList<>(2);
        for (int side : new int[]{1, -1}) {
            Location from = eyes(ravager, side);
            Vector direction = aim.toVector().subtract(from.toVector());
            double length = Math.min(settings.laserRange(), direction.length());
            direction.normalize();
            RayTraceResult wall = ravager.getWorld().rayTraceBlocks(from, direction, length,
                FluidCollisionMode.NEVER, true);
            double reach = wall == null ? length : wall.getHitPosition().distance(from.toVector());
            beam(from, direction, reach);
            // el rayo frena en la pared (reach) y en el primer jugador que cruza: ese es el que recibe el golpe
            RayTraceResult entity = ravager.getWorld().rayTraceEntities(from, direction, reach, 0.4,
                e -> e instanceof Player p && attackable(p));
            hitsPerEye.add(entity != null && entity.getHitEntity() instanceof Player p ? p : null);
            if (wall != null) {
                ravager.getWorld().spawnParticle(Particle.LAVA, wall.getHitPosition().toLocation(ravager.getWorld()), 1);
            }
        }
        if ((now - boss.chargeEnd) % 4 == 0) {
            for (Player hitPlayer : DonPollosLaser.victims(hitsPerEye)) {
                blame(hitPlayer, DeathCause.LASER);
                hitPlayer.damage(boss.difficulty.damage(settings.laserDamage()), ravager);
                hitPlayer.setFireTicks(Math.max(hitPlayer.getFireTicks(), 40));
            }
        }
    }

    void endLaser(DonPollosBoss boss, long now) {
        boss.firingUntil = 0;
        boss.target = null;
        boss.ravager.setAI(true);
        int jitter = ThreadLocalRandom.current().nextInt(-40, 41);
        boss.nextLaser = now + boss.difficulty.cooldownTicks(settings.laserCooldownSeconds() * 20.0) + jitter;
    }

    static void beam(Location from, Vector direction, double length) {
        World world = from.getWorld();
        Particle.DustOptions red = new Particle.DustOptions(Color.fromRGB(255, 30, 20), 1.1f);
        for (double d = 0.3; d <= length; d += 0.35) {
            world.spawnParticle(Particle.DUST, from.clone().add(direction.clone().multiply(d)), 1, 0, 0, 0, 0, red, true);
        }
    }

    enum DeathCause {
        METEOR(" §7fue aplastado por un §ameteorito de WhatsApp §7del §4Don Pollo Boss"),
        LASER(" §7fue derretido por los §claser de los ojos §7del §4Don Pollo Boss"),
        RAM(" §7fue atropellado por el auto del §4Don Pollo Boss"),
        CHICKEN(" §7fue picoteado hasta la muerte por una §aGallina WhatsApp"),
        LABUBU(" §7fue mordisqueado por los §dLabubus"),
        COLONEL(" §7fue frito por §fEl Coronel"),
        FINAL_COLONEL(" §7fue frito por un §4Sanders Fase Final"),
        BEAM(" §7fue pulverizado por el §3rayo sonico §7del §4Don Pollo Boss"),
        SHOCKWAVE(" §7salio volando por la §6onda expansiva §7del §4Don Pollo Boss");

        private final String message;

        DeathCause(String message) {
            this.message = message;
        }

        String message() {
            return message;
        }
    }

    /** Cuanto despues del golpe se le sigue echando la culpa (cubre el fuego del meteorito o del laser). */
    static final long BLAME_TICKS = 100;

    record Blame(DeathCause cause, long tick) {
    }

    void blame(Player player, DeathCause cause) {
        lastHits.put(player.getUniqueId(), new Blame(cause, Bukkit.getCurrentTick()));
    }
}
