package dev.linqfy.bigCasares.modules.donpollos;

import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Ondas expansivas del mega salto del Don Pollo Boss (FASE FINAL): los bloques pegados a la caida salen
 * volando y despues un anillo de particulas se abre; a quien toca le pega y lo tira para arriba, mas
 * fuerte cuanto mas cerca de la caida.
 */
final class DonPollosBossShockwaves {

    /** Bloques por tick que avanza el anillo. */
    static final double SPEED = 0.6;
    private static final double BAND = 0.9;
    private static final double FLYING_RADIUS = 2.6;
    private static final int MAX_FLYING_BLOCKS = 18;

    private final List<Wave> waves = new ArrayList<>();
    /** Cuanto se agranda la zona protegida para los bloques que salen volando (caen a varios bloques de donde estaban). */
    static final int LANDING_MARGIN = 8;

    private final BiConsumer<Player, Runnable> onHit;
    private final DonPollosProtection protection;
    /** Los bloques de un cubo de cualquier boss (paredes, piso e interior). */
    private final Predicate<Block> cubeBlock;

    /** Marca (en el FallingBlock) de los bloques que lanzo una onda: al aterrizar se revisa donde caen. */
    private final NamespacedKey flyingKey;

    DonPollosBossShockwaves(BiConsumer<Player, Runnable> onHit, DonPollosProtection protection, Predicate<Block> cubeBlock,
                            NamespacedKey flyingKey) {
        this.flyingKey = flyingKey;
        this.onHit = onHit;
        this.protection = protection;
        this.cubeBlock = cubeBlock;
    }

    /**
     * Si el megasalto no puede tocar este bloque: esta en una zona protegida (o a menos de {@link #LANDING_MARGIN}
     * bloques, porque lo que sale volando aterriza al lado) o es parte del cubo de algun boss.
     */
    static boolean untouchable(Block block, DonPollosProtection protection, Predicate<Block> cubeBlock) {
        return cubeBlock.test(block) || protection.isProtected(block.getWorld(),
            block.getX() - LANDING_MARGIN, block.getX() + LANDING_MARGIN, block.getY() - LANDING_MARGIN,
            block.getY() + LANDING_MARGIN, block.getZ() - LANDING_MARGIN, block.getZ() + LANDING_MARGIN);
    }

    /** Cuanto le pega y cuanto lo levanta a alguien a {@code distance} bloques: maximo al lado, un tercio en el borde. */
    static double damageAt(double distance, double radius, double maxDamage) {
        double closeness = Math.max(0, Math.min(1, 1 - distance / radius));
        return maxDamage * (1 + 2 * closeness) / 3;
    }

    static double liftAt(double distance, double radius) {
        double closeness = Math.max(0, Math.min(1, 1 - distance / radius));
        return 0.55 + 0.95 * closeness;
    }

    void slam(LivingEntity source, Location center, double radius, double maxDamage) {
        World world = center.getWorld();
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.5f, 0.6f);
        world.playSound(center, Sound.ENTITY_WARDEN_ATTACK_IMPACT, 2.5f, 0.6f);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1, 0, 0, 0, 0, null, true);
        throwBlocks(center, protection, cubeBlock, flyingKey);
        waves.add(new Wave(source, center.clone(), radius, maxDamage));
    }

    /** Los bloques de arriba alrededor de la caida salen volando (nunca bedrock, liquidos ni cofres/hornos). */
    private static void throwBlocks(Location center, DonPollosProtection protection, Predicate<Block> cubeBlock,
                                    NamespacedKey flyingKey) {
        World world = center.getWorld();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<Block> candidates = candidates(center, protection, cubeBlock, DonPollosBossShockwaves::isSolid);
        java.util.Collections.shuffle(candidates);
        for (Block block : candidates.subList(0, Math.min(MAX_FLYING_BLOCKS, candidates.size()))) {
            BlockData data = block.getBlockData();
            block.setType(Material.AIR, false);
            FallingBlock flying = world.spawnFallingBlock(block.getLocation().add(0.5, 0.2, 0.5), data);
            flying.getPersistentDataContainer().set(flyingKey, PersistentDataType.BYTE, (byte) 1);
            flying.setDropItem(false);
            flying.setHurtEntities(false);
            Vector out = block.getLocation().add(0.5, 0, 0.5).toVector().subtract(center.toVector()).setY(0);
            if (out.lengthSquared() < 0.01) {
                out = new Vector(random.nextDouble(-1, 1), 0, random.nextDouble(-1, 1));
            }
            out.normalize().multiply(random.nextDouble(0.15, 0.45));
            flying.setVelocity(out.setY(random.nextDouble(0.55, 1.05)));
        }
    }

    /** Solido de verdad (no faroles, cadenas, varas ni andamios): se inyecta en los tests porque necesita el registro. */
    private static boolean isSolid(Block block) {
        return block.getType().isSolid();
    }

    /** Los bloques que pueden salir volando alrededor de la caida: sin bedrock, cofres/hornos, zonas protegidas ni cubos. */
    static List<Block> candidates(Location center, DonPollosProtection protection, Predicate<Block> cubeBlock,
                                  Predicate<Block> solid) {
        List<Block> candidates = new ArrayList<>();
        int r = (int) Math.ceil(FLYING_RADIUS);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > FLYING_RADIUS * FLYING_RADIUS) {
                    continue;
                }
                for (int dy = -1; dy >= -2; dy--) {
                    Block block = center.clone().add(dx, dy, dz).getBlock();
                    if (solid.test(block) && !block.isPassable() && block.getType() != Material.BEDROCK && !(block.getState() instanceof TileState)
                        && block.getRelative(0, 1, 0).isPassable() && !untouchable(block, protection, cubeBlock)) {
                        candidates.add(block);
                    }
                }
            }
        }
        return candidates;
    }

    void tick() {
        Iterator<Wave> iterator = waves.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().tick()) {
                iterator.remove();
            }
        }
    }

    void clear() {
        waves.clear();
    }

    private final class Wave {
        private final LivingEntity source;
        private final Location center;
        private final double radius;
        private final double maxDamage;
        private final Set<UUID> hit = new HashSet<>();
        private double current;

        private Wave(LivingEntity source, Location center, double radius, double maxDamage) {
            this.source = source;
            this.center = center;
            this.radius = radius;
            this.maxDamage = maxDamage;
        }

        /** Abre el anillo un paso; devuelve true cuando ya llego al borde. */
        private boolean tick() {
            current += SPEED;
            World world = center.getWorld();
            int points = Math.max(12, (int) (current * 2 * Math.PI / 0.6));
            Block ground = center.clone().add(0, -1, 0).getBlock();
            BlockData dust = ground.getType().isSolid() ? ground.getBlockData() : Material.DIRT.createBlockData();
            for (int i = 0; i < points; i++) {
                double angle = 2 * Math.PI * i / points;
                Location at = center.clone().add(Math.cos(angle) * current, 0.2, Math.sin(angle) * current);
                world.spawnParticle(Particle.CLOUD, at, 1, 0.05, 0.05, 0.05, 0.01, null, true);
                if (i % 2 == 0) {
                    world.spawnParticle(Particle.BLOCK, at, 2, 0.1, 0.1, 0.1, 0, dust, true);
                }
                if (i % 6 == 0) {
                    world.spawnParticle(Particle.SWEEP_ATTACK, at.clone().add(0, 0.4, 0), 1, 0, 0, 0, 0, null, true);
                }
            }
            for (Player player : world.getPlayers()) {
                if (player.isDead() || player.getGameMode() == GameMode.CREATIVE
                    || player.getGameMode() == GameMode.SPECTATOR || hit.contains(player.getUniqueId())) {
                    continue;
                }
                Location feet = player.getLocation();
                double dy = Math.abs(feet.getY() - center.getY());
                double distance = Math.hypot(feet.getX() - center.getX(), feet.getZ() - center.getZ());
                if (dy > 3 || distance > current + BAND || distance < current - BAND - SPEED) {
                    continue;
                }
                hit.add(player.getUniqueId());
                double hurt = damageAt(distance, radius, maxDamage);
                onHit.accept(player, () -> player.damage(hurt, source));
                Vector push = feet.toVector().subtract(center.toVector()).setY(0);
                if (push.lengthSquared() > 1e-4) {
                    push.normalize().multiply(0.6);
                }
                player.setVelocity(push.setY(liftAt(distance, radius)));
            }
            return current >= radius;
        }
    }
}
