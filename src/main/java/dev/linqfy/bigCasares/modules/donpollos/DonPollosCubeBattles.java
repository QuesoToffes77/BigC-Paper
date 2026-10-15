package dev.linqfy.bigCasares.modules.donpollos;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Ravager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.UUID;

import static dev.linqfy.bigCasares.modules.donpollos.DonPollosBossSupport.*;

/**
 * FASE 3 del Don Pollo Boss: se eleva, vacia el chunk de cada jugador y arrancan las Batallas del Cubo; flota adentro
 * de una esfera (invulnerable) hasta que se ganan o se abandonan.
 */
final class DonPollosCubeBattles {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final JavaPlugin plugin;
    private final DonPollosBossSettings settings;
    private final DonPollosBossMinions minions;
    private final DonPollosBossTerrain terrain;

    DonPollosCubeBattles(JavaPlugin plugin, DonPollosBossSettings settings, DonPollosBossMinions minions,
                         DonPollosBossTerrain terrain) {
        this.plugin = plugin;
        this.settings = settings;
        this.minions = minions;
        this.terrain = terrain;
    }

    /** Se eleva {@code levitate-height} bloques en {@code levitate-seconds} y le vacia el piso a cada jugador. */
    void startLevitation(DonPollosBoss boss, long now) {
        Ravager ravager = boss.ravager;
        ravager.setTarget(null);
        ravager.setAI(false);
        ravager.setGravity(false);
        ravager.setVelocity(new Vector());
        boss.levitateFrom = ravager.getLocation();
        boss.levitateStart = now;
        boss.levitateEnd = now + Math.round(settings.phase3().levitateSeconds() * 20);
        ravager.getWorld().playSound(ravager.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 2f, 0.6f);
        for (UUID viewer : boss.viewers) {
            Player player = Bukkit.getPlayer(viewer);
            if (player != null && attackable(player) && !DonPollosBossTerrain.inAnyPit(boss, player.getLocation())) {
                DonPollosBossPit pit = terrain.digPit(boss, player, settings.phase3().pitDepth());
                if (pit != null) {
                    boss.pits.add(pit);
                }
            }
        }
        if (boss.phase.summons() && boss.cube == null && !boss.pits.isEmpty()) {
            startCube(boss, now);
        }
    }

    void levitate(DonPollosBoss boss, long now) {
        Ravager ravager = boss.ravager;
        double total = boss.levitateEnd - boss.levitateStart;
        double t = Math.min(1, (now - boss.levitateStart) / total);
        double eased = t * t * (3 - 2 * t);
        Location at = boss.levitateFrom.clone().add(0, settings.phase3().levitateHeight() * eased, 0);
        at.setYaw(ravager.getLocation().getYaw() + 3);  // gira despacito mientras sube
        at.setPitch(0);
        ravager.teleport(at);
        World world = ravager.getWorld();
        world.spawnParticle(Particle.REVERSE_PORTAL, at.clone().add(0, 0.5, 0), 12, 0.8, 0.3, 0.8, 0.02, null, true);
        world.spawnParticle(Particle.FLAME, at, 4, 0.6, 0.1, 0.6, 0.01, null, true);
        if (now % 20 == 0) {
            world.playSound(at, Sound.BLOCK_BEACON_AMBIENT, 2f, 0.6f);
        }
        if (now % 3 == 0) {
            shieldSphere(ravager, now);
        }
        if (now >= boss.levitateEnd) {
            if (boss.cube != null) {
                // se queda arriba, protegido, hasta que se ganen las Batallas del Cubo
                boss.levitateEnd = 0;
                boss.hoverAt = at;
            } else {
                finishLevitation(boss);
            }
        }
    }

    /** Flota arriba (sube y baja apenas) mirando al jugador mas cercano, dentro de la esfera. */
    void hover(DonPollosBoss boss, long now) {
        Ravager ravager = boss.ravager;
        Location at = boss.hoverAt.clone().add(0, Math.sin(now / 20.0) * 0.3, 0);
        Player player = nearestPlayer(ravager.getLocation(), BAR_RADIUS * 2);
        if (player != null && player.getWorld() == at.getWorld()) {
            Vector look = player.getLocation().toVector().subtract(at.toVector());
            at.setYaw((float) Math.toDegrees(Math.atan2(-look.getX(), look.getZ())));
        } else {
            at.setYaw(ravager.getLocation().getYaw());
        }
        at.setPitch(0);
        ravager.teleport(at);
        if (now % 3 == 0) {
            shieldSphere(ravager, now);
        }
        if (now % 40 == 0) {
            ravager.getWorld().playSound(at, Sound.BLOCK_BEACON_AMBIENT, 1.5f, 0.8f);
        }
    }

    /**
     * Esfera de particulas azules alrededor del boss: pocos puntos repartidos parejo (espiral de Fibonacci)
     * que van girando, asi se lo ve adentro pero se nota la forma.
     */
    void shieldSphere(Ravager ravager, long now) {
        double scale = scale(ravager);
        Location center = ravager.getLocation().add(0, 1.55 * scale, 0);
        double radius = 2.4 * scale;
        int points = 110;
        double golden = Math.PI * (3 - Math.sqrt(5));
        double spin = now * 0.02;
        Particle.DustOptions blue = new Particle.DustOptions(Color.fromRGB(60, 150, 255), 1.1f);
        World world = ravager.getWorld();
        for (int i = 0; i < points; i++) {
            double y = 1 - 2 * (i + 0.5) / points;
            double ring = Math.sqrt(1 - y * y);
            double angle = golden * i + spin;
            world.spawnParticle(Particle.DUST, center.clone().add(Math.cos(angle) * ring * radius, y * radius,
                Math.sin(angle) * ring * radius), 1, 0, 0, 0, 0, blue, true);
        }
    }

    /** Termina de subir (o de flotar): baja planeando (no se lastima al caer) y sigue peleando. */
    void finishLevitation(DonPollosBoss boss) {
        boss.levitateEnd = 0;
        boss.hoverAt = null;
        boss.ravager.setGravity(true);
        boss.ravager.setAI(true);
        boss.ravager.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 20, 0, false, false));
    }

    static Component cubeTitle(DonPollosCubeBattle cube) {
        return LEGACY.deserialize("§6§lBatallas del Cubo §8| §eRonda " + cube.round() + "/" + cube.rounds());
    }

    void startCube(DonPollosBoss boss, long now) {
        DonPollosBossPhase3Settings phase3 = settings.phase3();
        // la primera ronda arranca justo cuando termina de elevarse (la pausa inicial dura hasta ahi)
        boss.cube = new DonPollosCubeBattle(phase3.rounds().stream().map(boss.difficulty::round).toList(),
            Math.min(boss.difficulty.count(phase3.maxAlive()), phase3.hardMaxLabubus()), phase3.roundPauseTicks(),
            Math.max(now, boss.levitateEnd) - phase3.roundPauseTicks());
        boss.cubeBar = BossBar.bossBar(cubeTitle(boss.cube), 1f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
        plugin.getLogger().info("Batallas del Cubo: " + boss.pits.size() + " cubo(s), " + phase3.rounds().size() + " rondas");
    }

    void tickCube(DonPollosBoss boss, long now) {
        if (abandonCubeIfEmpty(boss, now)) {
            return;
        }
        DonPollosCubeBattle cube = boss.cube;
        int labubus = (int) minions.count(boss.minions, DonPollosBossMinions.Kind::isLabubu);
        int colonels = (int) minions.count(boss.minions, kind -> kind == DonPollosBossMinions.Kind.CORONEL);
        if (now % 10 == 0) {
            digNewcomers(boss);
        }
        Player player = nextCubeFighter(boss);
        DonPollosCubeBattle.Step step = player == null
            ? DonPollosCubeBattle.Step.NONE : cube.tick(now, labubus, colonels);
        if (step.spawnColonel() || step.spawnLabubu()) {
            boss.fighterTurn++;
        }
        if (step.roundStarted() > 0) {
            announceRound(boss, step.roundStarted());
        }
        if (step.spawnColonel()) {
            DonPollosCubeRound round = cube.current();
            boss.minions.add(toughen(boss, minions.spawn(DonPollosBossMinions.Kind.CORONEL,
                DonPollosBossMinions.around(player.getLocation(), 4), player, boss.difficulty.level(), colonel -> {
                    // cada ronda mas rapido y mas fuerte
                    DonPollosBossMinions.setAttribute(colonel, Attribute.MOVEMENT_SPEED, ZOMBIE_SPEED * round.colonelSpeed());
                    DonPollosBossMinions.setAttribute(colonel, Attribute.ATTACK_DAMAGE, ZOMBIE_DAMAGE + round.colonelDamage());
                })).getUniqueId());
            colonels++;
        }
        if (step.spawnLabubu()) {
            DonPollosBossMinions.Kind kind = DonPollosBossMinions.Kind.LABUBUS[(cube.labubuIndex() - 1) % 4];
            boss.minions.add(toughen(boss, minions.spawn(kind, DonPollosBossMinions.around(player.getLocation(), 3), player,
                boss.difficulty.level())).getUniqueId());
            labubus++;
        }
        boss.cubeBar.name(cubeTitle(cube));
        boss.cubeBar.progress((float) cube.remaining(labubus, colonels));
        for (UUID viewer : boss.viewers) {
            Player p = Bukkit.getPlayer(viewer);
            if (p != null && boss.cubeViewers.add(viewer)) {
                p.showBossBar(boss.cubeBar);
            }
        }
        if (step.finished()) {
            for (UUID viewer : boss.viewers) {
                Player p = Bukkit.getPlayer(viewer);
                if (p != null) {
                    p.showTitle(net.kyori.adventure.title.Title.title(
                        LEGACY.deserialize("§a§l¡Ganaste las Batallas del Cubo!"), LEGACY.deserialize("§7Volves a la superficie"),
                        net.kyori.adventure.title.Title.Times.times(java.time.Duration.ofMillis(300),
                            java.time.Duration.ofMillis(2500), java.time.Duration.ofMillis(700))));
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                }
            }
            endCube(boss);
        }
    }

    /**
     * Si nadie queda adentro de ningun cubo (se desconectaron o murieron) durante un rato, se terminan las Batallas
     * del Cubo: se rellenan los pozos y Don Pollo baja a pelear. Asi el encuentro nunca queda trabado.
     */
    boolean abandonCubeIfEmpty(DonPollosBoss boss, long now) {
        boolean anyoneInside = !DonPollosBossTerrain.occupants(boss).isEmpty();
        if (anyoneInside) {
            boss.cubeEmptySince = -1;
            return false;
        }
        if (boss.cubeEmptySince < 0) {
            boss.cubeEmptySince = now;
        }
        if (!DonPollosCubeEscape.abandoned(boss.cubeEmptySince, now)) {
            return false;
        }
        plugin.getLogger().info("Batallas del Cubo abandonadas: se rellenan los cubos y Don Pollo vuelve a pelear.");
        for (UUID viewer : boss.viewers) {
            Player p = Bukkit.getPlayer(viewer);
            if (p != null) {
                p.sendMessage(LEGACY.deserialize("§eNo quedo nadie en el cubo: §6Don Pollo baja a pelear."));
            }
        }
        boss.cubeEmptySince = -1;
        endCube(boss);
        return true;
    }

    /** Quien entra al rango de la barra durante las Batallas del Cubo tambien cae en su propio cubo. */
    void digNewcomers(DonPollosBoss boss) {
        for (UUID viewer : boss.viewers) {
            Player player = Bukkit.getPlayer(viewer);
            if (player == null || !attackable(player) || DonPollosBossTerrain.inAnyPit(boss, player.getLocation())) {
                continue;
            }
            Location at = player.getLocation();
            boolean chunkTaken = boss.pits.stream().anyMatch(pit -> pit.world() == at.getWorld()
                && pit.chunkX() == at.getBlockX() >> 4 && pit.chunkZ() == at.getBlockZ() >> 4);
            if (chunkTaken) {
                continue;  // ese chunk ya es un cubo (esta arriba del borde: se cae adentro)
            }
            DonPollosBossPit pit = terrain.digPit(boss, player, settings.phase3().pitDepth());
            if (pit != null) {
                boss.pits.add(pit);
                player.showTitle(net.kyori.adventure.title.Title.title(
                    LEGACY.deserialize("§6§lBatallas del Cubo"), LEGACY.deserialize("§eNo hay escape..."),
                    net.kyori.adventure.title.Title.Times.times(java.time.Duration.ofMillis(200),
                        java.time.Duration.ofMillis(1500), java.time.Duration.ofMillis(500))));
            }
        }
    }

    /** A quien le salen los bichos: va rotando entre todos los que estan en un cubo (si no hay, el mas cercano). */
    Player nextCubeFighter(DonPollosBoss boss) {
        // los ocupantes reales de los cubos, sin importar a cuanto esta el Ravager (un cubo reubicado puede quedar lejos)
        Player fighter = DonPollosBossTerrain.pickFighter(DonPollosBossTerrain.occupants(boss), boss.fighterTurn);
        return fighter != null ? fighter : nearestPlayer(boss.ravager.getLocation(), BAR_RADIUS * 2);
    }

    void announceRound(DonPollosBoss boss, int round) {
        for (UUID viewer : boss.viewers) {
            Player p = Bukkit.getPlayer(viewer);
            if (p != null) {
                p.showTitle(net.kyori.adventure.title.Title.title(
                    LEGACY.deserialize("§6§lRonda " + round), LEGACY.deserialize("§eBatallas del Cubo"),
                    net.kyori.adventure.title.Title.Times.times(java.time.Duration.ofMillis(200),
                        java.time.Duration.ofMillis(1500), java.time.Duration.ofMillis(500))));
                p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 1.5f, 1f);
            }
        }
    }

    /** Termina las Batallas del Cubo: saca la barra, rellena cada cubo y sube a quien haya quedado adentro. */
    void endCube(DonPollosBoss boss) {
        if (boss.cubeBar != null) {
            for (UUID viewer : boss.cubeViewers) {
                Player p = Bukkit.getPlayer(viewer);
                if (p != null) {
                    p.hideBossBar(boss.cubeBar);
                }
            }
        }
        boss.cubeViewers.clear();
        boss.cube = null;
        boss.cubeBar = null;
        terrain.restore(boss);
        if (boss.hoverAt != null) {
            finishLevitation(boss);
        }
    }
}
