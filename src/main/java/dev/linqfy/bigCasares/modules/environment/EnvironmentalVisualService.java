package dev.linqfy.bigCasares.modules.environment;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Random;

/**
 * Sends bounded vanilla particle profiles directly to each observing player.
 * It never changes world blocks, biomes, weather textures, fog, or sky assets.
 */
public final class EnvironmentalVisualService {
    private static final Particle.DustOptions ACID_GREEN =
        new Particle.DustOptions(Color.fromRGB(154, 255, 35), 0.8f);
    private static final Particle.DustOptions ACID_YELLOW =
        new Particle.DustOptions(Color.fromRGB(216, 255, 64), 0.65f);
    private static final Particle.DustOptions FOG_GREEN =
        new Particle.DustOptions(Color.fromRGB(118, 181, 45), 1.45f);
    private static final Particle.DustOptions FOG_YELLOW =
        new Particle.DustOptions(Color.fromRGB(181, 201, 55), 1.2f);
    private static final Particle.DustOptions SPORE_GREEN =
        new Particle.DustOptions(Color.fromRGB(92, 222, 76), 0.55f);
    private static final Particle.DustOptions SPORE_GLOW =
        new Particle.DustOptions(Color.fromRGB(213, 255, 92), 0.45f);
    private static final Particle.DustOptions BLOOD_RED =
        new Particle.DustOptions(Color.fromRGB(178, 18, 32), 1.0f);
    private static final Particle.DustOptions BLOOD_DARK =
        new Particle.DustOptions(Color.fromRGB(92, 5, 16), 0.75f);

    private final Random random;
    private EnvironmentalVisualSettings settings;

    public EnvironmentalVisualService(EnvironmentalVisualSettings settings) {
        this(settings, new Random());
    }

    EnvironmentalVisualService(EnvironmentalVisualSettings settings, Random random) {
        this.settings = settings == null ? EnvironmentalVisualSettings.defaults() : settings;
        this.random = Objects.requireNonNull(random, "random");
    }

    public void updateSettings(EnvironmentalVisualSettings settings) {
        this.settings = settings == null ? EnvironmentalVisualSettings.disabled() : settings;
    }

    public int show(Player player, EnvironmentalVisualProfile profile) {
        if (player == null || profile == null || !settings.enabled() || !player.isOnline()) {
            return 0;
        }
        EnvironmentalVisualPlan plan = EnvironmentalVisualPlan.forProfile(profile, settings.quality());
        return switch (profile) {
            case ACID_RAIN -> acidRain(player, plan.particleBudget());
            case CHEMICAL_FOG -> chemicalFog(player, plan.particleBudget());
            case TOXIC_SPORES -> toxicSpores(player, plan.particleBudget());
            case BLOOD_MOON -> bloodMoon(player, plan.particleBudget());
        };
    }

    private int acidRain(Player player, int budget) {
        int drops = Math.max(2, budget / 2);
        for (int index = 0; index < drops; index++) {
            Location top = randomAround(player, 1.0, settings.verticalRadius());
            Particle.DustOptions color = index % 3 == 0 ? ACID_YELLOW : ACID_GREEN;
            player.spawnParticle(Particle.DUST, top, 1, 0.02, 0.45, 0.02, 0.02, color);
            if (index % 3 == 0) {
                player.spawnParticle(Particle.FALLING_WATER, top, 1, 0.0, 0.15, 0.0, 0.0);
            }
        }
        int remaining = Math.max(0, budget - drops);
        int splashes = Math.min(4, remaining / 2);
        int acidMist = Math.min(3, remaining - splashes);
        Location floor = player.getLocation().add(randomHorizontal(3.5), 0.15, randomHorizontal(3.5));
        if (splashes > 0) {
            player.spawnParticle(Particle.SPLASH, floor, splashes, 0.35, 0.08, 0.35, 0.0);
        }
        if (acidMist > 0) {
            player.spawnParticle(Particle.DUST, floor, acidMist, 0.3, 0.08, 0.3, 0.0, ACID_GREEN);
        }
        return drops + splashes + acidMist;
    }

    private int chemicalFog(Player player, int budget) {
        int dust = Math.max(3, budget * 2 / 3);
        Location center = player.getLocation().add(0.0, 1.2, 0.0);
        player.spawnParticle(Particle.DUST, center, dust / 2,
            settings.horizontalRadius() * 0.55, settings.verticalRadius() * 0.28,
            settings.horizontalRadius() * 0.55, 0.0, FOG_GREEN);
        player.spawnParticle(Particle.DUST, center, dust - dust / 2,
            settings.horizontalRadius() * 0.5, settings.verticalRadius() * 0.22,
            settings.horizontalRadius() * 0.5, 0.0, FOG_YELLOW);
        int smoke = Math.max(1, budget - dust);
        player.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, center, smoke,
            settings.horizontalRadius() * 0.35, settings.verticalRadius() * 0.16,
            settings.horizontalRadius() * 0.35, 0.005);
        return Math.min(budget, dust + smoke);
    }

    private int toxicSpores(Player player, int budget) {
        int vanilla = Math.max(2, budget / 3);
        Location center = player.getLocation().add(0.0, 1.5, 0.0);
        player.spawnParticle(Particle.SPORE_BLOSSOM_AIR, center, vanilla,
            settings.horizontalRadius() * 0.45, settings.verticalRadius() * 0.45,
            settings.horizontalRadius() * 0.45, 0.006);
        int colored = budget - vanilla;
        for (int index = 0; index < colored; index++) {
            Location spore = randomAround(player, -1.0, settings.verticalRadius() * 0.75);
            Particle.DustOptions color = index % 4 == 0 ? SPORE_GLOW : SPORE_GREEN;
            player.spawnParticle(Particle.DUST, spore, 1, 0.08, 0.12, 0.08, 0.01, color);
        }
        return budget;
    }

    private int bloodMoon(Player player, int budget) {
        Location center = player.getLocation().add(0.0, 1.0, 0.0);
        int red = Math.max(2, budget * 3 / 4);
        player.spawnParticle(Particle.DUST, center, red,
            settings.horizontalRadius() * 0.65, settings.verticalRadius() * 0.38,
            settings.horizontalRadius() * 0.65, 0.0, BLOOD_RED);
        int dark = budget - red;
        if (dark > 0) {
            player.spawnParticle(Particle.DUST, center, dark,
                settings.horizontalRadius() * 0.5, settings.verticalRadius() * 0.25,
                settings.horizontalRadius() * 0.5, 0.0, BLOOD_DARK);
        }
        return budget;
    }

    private Location randomAround(Player player, double minimumY, double maximumY) {
        double x = randomHorizontal(settings.horizontalRadius());
        double z = randomHorizontal(settings.horizontalRadius());
        double y = minimumY + random.nextDouble() * Math.max(0.1, maximumY - minimumY);
        return player.getLocation().add(x, y, z);
    }

    private double randomHorizontal(double radius) {
        return (random.nextDouble() * 2.0 - 1.0) * radius;
    }
}
