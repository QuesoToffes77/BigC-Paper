package dev.linqfy.bigCasares.modules.donpollos;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Ravager;
import org.bukkit.util.Vector;

/** Ayudas y constantes que comparten las partes del Don Pollo Boss. */
final class DonPollosBossSupport {

    static final double BAR_RADIUS = 48;
    static final double CHASE_RANGE = 40;
    static final double EYE_HEIGHT = 45.5 / 16;   // altura de los ojos del modelo, en bloques
    static final double EYE_SPREAD = 2.5 / 16;    // separacion de cada ojo al centro
    /** Zombie comun: el Coronel arranca de aca y en cada ronda de las Batallas del Cubo se hace mas rapido y fuerte. */
    static final double ZOMBIE_SPEED = 0.23;
    static final double ZOMBIE_DAMAGE = 3;

    private DonPollosBossSupport() {
    }

    static boolean attackable(Player player) {
        return !player.isDead() && player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR;
    }

    /** Jugador vivo mas cercano en supervivencia o aventura (a los de creativo y espectador no los ve). */
    static Player nearestPlayer(Location from, double range) {
        Player nearest = null;
        double best = range * range;
        for (Player player : from.getWorld().getPlayers()) {
            if (!attackable(player)) {
                continue;
            }
            double distance = player.getLocation().distanceSquared(from);
            if (distance < best) {
                best = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    static double scale(LivingEntity entity) {
        AttributeInstance scale = entity.getAttribute(Attribute.SCALE);
        return scale == null ? 1.0 : scale.getValue();
    }

    static Location eyes(Ravager ravager, int side) {
        double scale = scale(ravager);
        Location base = ravager.getLocation();
        Vector forward = base.getDirection().setY(0);
        if (forward.lengthSquared() < 1e-6) {
            forward = new Vector(0, 0, 1);
        }
        forward.normalize();
        Vector right = new Vector(-forward.getZ(), 0, forward.getX());
        return base.clone().add(0, EYE_HEIGHT * scale, 0)
            .add(forward.clone().multiply(0.15 * scale))
            .add(right.multiply(EYE_SPREAD * scale * side));
    }

    static void faceTowards(Ravager ravager, Location target) {
        Vector to = target.toVector().subtract(ravager.getLocation().toVector());
        float yaw = (float) Math.toDegrees(Math.atan2(-to.getX(), to.getZ()));
        ravager.setRotation(yaw, 0);
    }

    static void setAttribute(LivingEntity entity, Attribute attribute, double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    /** Primer piso solido bajando desde {@code fromY} (si no hay, la altura de respaldo). */
    static double groundY(Location at, double fromY, double fallback) {
        Location probe = at.clone();
        for (int y = (int) Math.floor(fromY); y > fromY - 48 && y > at.getWorld().getMinHeight(); y--) {
            probe.setY(y);
            Block block = probe.getBlock();
            if (block.getType().isSolid() && block.getRelative(0, 1, 0).isPassable()
                && block.getRelative(0, 2, 0).isPassable()) {
                return y + 1;
            }
        }
        return fallback;
    }

    /** Bichos del boss mas rapidos y con mas dano segun la dificultad. */
    static org.bukkit.entity.Zombie toughen(DonPollosBoss boss, org.bukkit.entity.Zombie minion) {
        AttributeInstance speed = minion.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(boss.difficulty.speed(speed.getBaseValue()));
        }
        AttributeInstance damage = minion.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damage != null) {
            damage.setBaseValue(boss.difficulty.damage(damage.getBaseValue()));
        }
        return minion;
    }
}
