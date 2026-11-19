package dev.linqfy.bigCasares.modules.alexscaves.toxic;

import dev.linqfy.bigCasares.modules.alexscaves.core.effect.ModEffectService;
import dev.linqfy.bigCasares.modules.alexscaves.core.effect.ModEffectType;
import dev.linqfy.bigCasares.modules.alexscaves.core.mob.AnimatedMob;
import dev.linqfy.bigCasares.modules.alexscaves.core.mob.MobAnimator;
import dev.linqfy.bigCasares.modules.alexscaves.core.mob.MobDrop;
import dev.linqfy.bigCasares.modules.alexscaves.core.mob.ModMobBehaviour;
import dev.linqfy.bigCasares.modules.alexscaves.core.mob.ModMobCatalog;
import dev.linqfy.bigCasares.modules.alexscaves.core.mob.ModMobContext;
import dev.linqfy.bigCasares.modules.alexscaves.core.mob.ModMobType;
import dev.linqfy.bigCasares.modules.alexscaves.core.spawn.BiomeSpawnTable.SpawnCategory;
import dev.linqfy.bigCasares.modules.alexscaves.core.worldgen.CaveBiomeId;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Tameable;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mobs of the Toxic Caves with the stats and drops of the mod, each on a vanilla base with a
 * close AI: the Nucleeper is a creeper, the Brainiac a zombie, the Raycat a cat, the Radgill a
 * cod. The Gammaroach sits on a rabbit (a silverfish would hide in stone as infested blocks).
 * Abilities after the mod: the Nucleeper's long fuse and nuclear blast ({@link ToxicMobListener}), the
 * Brainiac's tongue and irradiating hits, the Gammaroach's radiation spray when hurt, and the Raycat
 * absorbing radiation from nearby mobs. The Brainiac's waste drums need a block the module does not have yet.
 */
public final class ToxicMobs {
    private static final CaveBiomeId BIOME = CaveBiomeId.TOXIC_CAVES;
    /** Mod {@code nucleeper_fuse_time}. */
    static final int NUCLEEPER_FUSE_TICKS = 300;

    public static final ModMobType NUCLEEPER = new ModMobType("nucleeper", BIOME, "creeper", SpawnCategory.MONSTER,
        Map.of("max_health", 40.0, "armor", 4.0, "movement_speed", 0.2),
        "alexscaves_nucleeper", List.of(MobDrop.of("ac_fissile_core", 0, 1), MobDrop.of("gunpowder", 0, 2)));
    public static final ModMobType BRAINIAC = new ModMobType("brainiac", BIOME, "zombie", SpawnCategory.MONSTER,
        Map.of("max_health", 40.0, "attack_damage", 5.0, "armor", 8.0, "movement_speed", 0.25, "follow_range", 32.0),
        "alexscaves_brainiac", List.of(MobDrop.of("rotten_flesh", 0, 2), MobDrop.of("ac_charred_remnant", 0, 1)));
    public static final ModMobType GAMMAROACH = new ModMobType("gammaroach", BIOME, "rabbit", SpawnCategory.AMBIENT,
        Map.of("max_health", 14.0, "attack_damage", 2.0, "movement_speed", 0.4),
        "alexscaves_gammaroach", List.of(MobDrop.of("ac_toxic_paste", 0, 3)));
    public static final ModMobType RAYCAT = new ModMobType("raycat", BIOME, "cat", SpawnCategory.CAVE_CREATURE,
        Map.of("max_health", 24.0, "attack_damage", 1.0, "movement_speed", 0.3),
        "alexscaves_raycat", List.of(MobDrop.of("bone", 0, 1)));
    public static final ModMobType RADGILL = new ModMobType("radgill", BIOME, "cod", SpawnCategory.WATER_AMBIENT,
        Map.of("max_health", 8.0, "movement_speed", 0.25),
        "alexscaves_radgill", List.of());

    public static final List<ModMobType> ALL = List.of(NUCLEEPER, BRAINIAC, GAMMAROACH, RAYCAT, RADGILL);

    private ToxicMobs() {
    }

    public static void register(ModMobCatalog catalog) {
        catalog.register(NUCLEEPER, (entity, type) -> {
            if (entity instanceof Creeper creeper) {
                creeper.setMaxFuseTicks(NUCLEEPER_FUSE_TICKS);
            }
            return new Nucleeper(type);
        });
        catalog.register(BRAINIAC, (entity, type) -> new Brainiac(type));
        catalog.register(GAMMAROACH, (entity, type) -> new Gammaroach(type));
        catalog.register(RAYCAT, (entity, type) -> new Raycat(type));
        catalog.register(RADGILL, (entity, ignored) -> new AnimatedMob(ToxicMobs::stranded));
    }

    /** Mod tag {@code resists_radiation}. */
    static boolean resistsRadiation(LivingEntity entity, ModMobContext context) {
        return context.mobs().typeOf(entity).map(type -> NukeMath.RESISTS_RADIATION.contains("alexscaves:" + type.id()))
            .orElse(false);
    }

    /** Mod {@code landProgress}: a fish out of the water lies on its side ({@code land}). */
    static String stranded(LivingEntity entity) {
        return !entity.isInWater() && entity.isOnGround() ? "land" : null;
    }

    private static double maxHealth(LivingEntity entity, double fallback) {
        return entity.getAttribute(Attribute.MAX_HEALTH) == null ? fallback : entity.getAttribute(Attribute.MAX_HEALTH).getValue();
    }

    /** Siren and charge sounds while the fuse burns; a charged one slowly heals (mod: 1 HP every 150 ticks). */
    static final class Nucleeper implements ModMobBehaviour {
        private final ModMobType type;
        private final MobAnimator animation = new MobAnimator();
        private int sound;
        private int heal;

        /**
         * Mod {@code closeProgress}/{@code explodeProgress}: the shell closes ({@code close}) as the fuse burns and the
         * last 5 ticks (the mod's explode delay) it shakes ({@code explode}).
         */
        static String state(int fuseTicks, int maxFuseTicks) {
            if (fuseTicks <= 0) {
                return null;
            }
            return fuseTicks >= maxFuseTicks - 5 ? "explode" : "close";
        }

        Nucleeper(ModMobType type) {
            this.type = type;
        }

        @Override
        public void tick(LivingEntity entity, ModMobContext context, int ticks) {
            if (!(entity instanceof Creeper creeper)) {
                return;
            }
            animation.update(entity, context, ticks, state(creeper.getFuseTicks(), creeper.getMaxFuseTicks()));
            if (creeper.getFuseTicks() > 0) {
                // Mod: once triggered the countdown runs to the end; vanilla would let it fizzle when the target
                // steps away. An ignited creeper keeps swelling, like one lit with flint and steel.
                if (!creeper.isIgnited()) {
                    creeper.setIgnited(true);
                }
                sound -= ticks;
                if (sound <= 0) {
                    sound = 40;
                    context.mobs().playSound(entity, type, "charge");
                    entity.getWorld().playSound(entity.getLocation(), "alexscaves:nuclear_siren", SoundCategory.HOSTILE, 3.0f, 1.0f);
                }
            } else {
                sound = 0;
            }
            if (creeper.isPowered() && (heal += ticks) >= 150) {
                heal = 0;
                entity.setHealth(Math.min(maxHealth(entity, 40.0), entity.getHealth() + 1.0));
            }
        }
    }

    /**
     * Irradiates what it hits (Irradiated 400 ticks) and licks targets up to 20 blocks away with its tongue:
     * 4 damage, Irradiated and a pull (mod: 1 in 16 per tick, cooldown 15-29 ticks).
     */
    static final class Brainiac implements ModMobBehaviour {
        static final int IRRADIATED_TICKS = 400;
        private final ModMobType type;
        private final MobAnimator animation = new MobAnimator();
        private int cooldown;

        Brainiac(ModMobType type) {
            this.type = type;
        }

        @Override
        public void tick(LivingEntity entity, ModMobContext context, int ticks) {
            animation.update(entity, context, ticks, null);
            cooldown = Math.max(0, cooldown - ticks);
            LivingEntity target = entity instanceof Mob mob ? mob.getTarget() : null;
            ThreadLocalRandom random = ThreadLocalRandom.current();
            if (target == null || !target.isValid() || cooldown > 0 || target.getWorld() != entity.getWorld()
                || random.nextDouble() >= 1.0 - Math.pow(15.0 / 16.0, ticks)) {
                return;
            }
            double distance = entity.getLocation().distance(target.getLocation());
            if (distance < 20.0 && distance > 3.0 && entity.hasLineOfSight(target)) {
                cooldown = 15 + random.nextInt(15);
                context.mobs().playSound(entity, type, "lick");
                lick(entity, context, target);
            }
        }

        private void lick(LivingEntity entity, ModMobContext context, LivingEntity target) {
            Location from = entity.getEyeLocation();
            Vector toward = target.getEyeLocation().toVector().subtract(from.toVector());
            double length = toward.length();
            for (double d = 0; d < length; d += 0.5) {
                entity.getWorld().spawnParticle(Particle.DUST, from.clone().add(toward.clone().multiply(d / length)), 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(0xD8, 0x6B, 0x8A), 1.0f));
            }
            target.damage(4.0, entity);
            irradiate(target, context);
            target.setVelocity(target.getVelocity().add(toward.normalize().multiply(-0.3)));
        }

        @Override
        public void onAttack(LivingEntity entity, ModMobContext context, LivingEntity target) {
            irradiate(target, context);
            // Mod BrainiacMeleeGoal: smash or bite, half and half (the barrel animations need the waste drum block).
            if (ThreadLocalRandom.current().nextBoolean()) {
                animation.action(entity, context, "smash");
            } else {
                animation.action(entity, context, "bite");
            }
        }

        static void irradiate(LivingEntity target, ModMobContext context) {
            if (!resistsRadiation(target, context)) {
                context.effects().apply(target, ModEffectType.IRRADIATED, 0, IRRADIATED_TICKS);
            }
        }
    }

    /**
     * When something not irradiated hurts it, sprays a radioactive cloud (mod: radius 2.3 shrinking to 0 over
     * 200 ticks, Irradiated 2000 ticks), then waits 10000-34000 ticks before it can spray again.
     */
    static final class Gammaroach implements ModMobBehaviour {
        static final int CLOUD_TICKS = 200;
        static final float CLOUD_RADIUS = 2.3f;
        private final ModMobType type;
        private final MobAnimator animation = new MobAnimator();
        private long readyAt;

        Gammaroach(ModMobType type) {
            this.type = type;
        }

        @Override
        public void tick(LivingEntity entity, ModMobContext context, int ticks) {
            animation.update(entity, context, ticks, null);
        }

        @Override
        public void onAttack(LivingEntity entity, ModMobContext context, LivingEntity target) {
            animation.action(entity, context, "ram");
        }

        @Override
        public void onDamaged(LivingEntity entity, ModMobContext context, EntityDamageEvent event) {
            if (!(event instanceof EntityDamageByEntityEvent byEntity)) {
                return;
            }
            Entity damager = byEntity.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter
                ? shooter : byEntity.getDamager();
            long now = entity.getWorld().getGameTime();
            if (!(damager instanceof LivingEntity attacker) || context.effects().has(attacker, ModEffectType.IRRADIATED) || now < readyAt) {
                return;
            }
            readyAt = now + 10000 + ThreadLocalRandom.current().nextInt(24000);
            context.mobs().playSound(entity, type, "spray");
            animation.action(entity, context, "spray");
            spray(entity.getLocation().add(0, 0.2, 0), context);
        }

        private static void spray(Location at, ModMobContext context) {
            new BukkitRunnable() {
                private int age;

                @Override
                public void run() {
                    if (age >= CLOUD_TICKS || !at.isChunkLoaded()) {
                        cancel();
                        return;
                    }
                    double radius = CLOUD_RADIUS * (1.0 - (double) age / CLOUD_TICKS);
                    at.getWorld().spawnParticle(Particle.DUST, at, 12, radius * 0.6, 0.2, radius * 0.6, 0,
                        new Particle.DustOptions(Color.fromRGB(0x77, 0xD6, 0x0E), 1.4f));
                    for (Entity near : at.getWorld().getNearbyEntities(at, radius, 0.5 + radius * 0.25, radius)) {
                        if (near instanceof LivingEntity living && !resistsRadiation(living, context)) {
                            context.effects().apply(living, ModEffectType.IRRADIATED, 0, 2000);
                        }
                    }
                    age += 10;
                }
            }.runTaskTimer(context.plugin(), 0L, 10L);
        }
    }

    /**
     * Every 300-600 ticks looks for the nearest irradiated mob within 9 blocks (its owner first, within 20, when
     * tamed; players only when tamed), takes one level of Irradiated from it over a second and heals 10. While
     * irradiated it heals 1 every 10 ticks (mod {@code RaycatEntity}).
     */
    static final class Raycat implements ModMobBehaviour {
        private static final int ABSORB_TICKS = 20;
        private final ModMobType type;
        private final MobAnimator animation = new MobAnimator();
        private int cooldown = 300 + ThreadLocalRandom.current().nextInt(300);
        private LivingEntity absorbing;
        private int absorbTicks;
        private int heal;

        Raycat(ModMobType type) {
            this.type = type;
        }

        @Override
        public void tick(LivingEntity entity, ModMobContext context, int ticks) {
            ModEffectService effects = context.effects();
            animation.update(entity, context, ticks, state(entity));
            if (effects.has(entity, ModEffectType.IRRADIATED) && (heal += ticks) >= 10) {
                heal = 0;
                entity.setHealth(Math.min(maxHealth(entity, 24.0), entity.getHealth() + 1.0));
            }
            if (absorbing != null) {
                absorb(entity, context, ticks);
                return;
            }
            cooldown -= ticks;
            if (cooldown > 0) {
                return;
            }
            cooldown = 300 + ThreadLocalRandom.current().nextInt(300);
            absorbing = findTarget(entity, context, effects);
            if (absorbing != null) {
                absorbTicks = ABSORB_TICKS;
                context.mobs().playSound(entity, type, "absorb");
            }
        }

        /** Mod {@code layProgress} / {@code sitProgress}: lying down, or sitting when tamed and told to. */
        static String state(LivingEntity entity) {
            if (entity instanceof Cat cat && cat.isLyingDown()) {
                return "lay";
            }
            return entity instanceof Sittable sittable && sittable.isSitting() ? "sit" : null;
        }

        private void absorb(LivingEntity entity, ModMobContext context, int ticks) {
            if (!absorbing.isValid() || absorbing.getWorld() != entity.getWorld()) {
                absorbing = null;
                return;
            }
            Location from = absorbing.getLocation().add(0, absorbing.getHeight() * 0.5, 0);
            Vector toward = entity.getLocation().add(0, 0.4, 0).toVector().subtract(from.toVector());
            entity.getWorld().spawnParticle(Particle.DUST, from.add(toward.multiply(1.0 - (double) absorbTicks / ABSORB_TICKS)),
                3, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(Color.fromRGB(0x7F, 0xD1, 0x1B), 1.2f));
            absorbTicks -= ticks;
            if (absorbTicks > 0) {
                return;
            }
            ModEffectService effects = context.effects();
            int level = effects.get(entity, ModEffectType.IRRADIATED).map(effect -> effect.amplifier() + 1).orElse(0);
            entity.setHealth(Math.min(maxHealth(entity, 24.0), entity.getHealth() + 10.0));
            effects.remove(entity, ModEffectType.IRRADIATED);
            effects.apply(entity, ModEffectType.IRRADIATED, level, 200);
            LivingEntity target = absorbing;
            effects.get(target, ModEffectType.IRRADIATED).ifPresent(effect -> {
                effects.remove(target, ModEffectType.IRRADIATED);
                if (effect.amplifier() > 0) {
                    effects.apply(target, ModEffectType.IRRADIATED, effect.amplifier() - 1, effect.remainingTicks());
                }
            });
            absorbing = null;
        }

        private LivingEntity findTarget(LivingEntity entity, ModMobContext context, ModEffectService effects) {
            boolean tamed = entity instanceof Tameable tameable && tameable.isTamed();
            if (entity instanceof Tameable tameable && tameable.getOwner() instanceof LivingEntity owner
                && owner.getWorld() == entity.getWorld() && owner.getLocation().distance(entity.getLocation()) < 20.0
                && effects.has(owner, ModEffectType.IRRADIATED)) {
                return owner;
            }
            LivingEntity closest = null;
            double best = Double.MAX_VALUE;
            for (Entity near : entity.getNearbyEntities(9.0, 9.0, 9.0)) {
                if (!(near instanceof LivingEntity living) || !effects.has(living, ModEffectType.IRRADIATED)
                    || context.mobs().typeOf(living).filter(found -> found == RAYCAT).isPresent()
                    || !tamed && living instanceof Player) {
                    continue;
                }
                double distance = living.getLocation().distanceSquared(entity.getLocation());
                if (distance < best) {
                    best = distance;
                    closest = living;
                }
            }
            return closest;
        }
    }
}
