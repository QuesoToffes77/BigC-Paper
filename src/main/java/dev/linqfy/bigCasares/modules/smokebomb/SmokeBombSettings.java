package dev.linqfy.bigCasares.modules.smokebomb;

import org.bukkit.configuration.file.FileConfiguration;

public record SmokeBombSettings(
    long durationTicks,
    double width,
    double depth,
    double height,
    long lingerTicks,
    long smokeBurstIntervalTicks,
    int impactParticles,
    int cloudParticles,
    double particleViewDistance,
    long occupancyIntervalTicks
) {

    public static SmokeBombSettings load(FileConfiguration config) {
        return new SmokeBombSettings(
            config.getLong("smoke-bomb.duration-ticks", 20L * 30),
            config.getDouble("smoke-bomb.width", 12.0),
            config.getDouble("smoke-bomb.depth", 12.0),
            config.getDouble("smoke-bomb.height", 5.0),
            config.getLong("smoke-bomb.linger-ticks", 20L * 2),
            config.getLong("smoke-bomb.smoke-burst-interval-ticks", 20L),
            config.getInt("smoke-bomb.impact-particles", 200),
            config.getInt("smoke-bomb.cloud-particles", 60),
            config.getDouble("smoke-bomb.particle-view-distance", 32.0),
            config.getLong("smoke-bomb.occupancy-interval-ticks", 5L)
        );
    }

    public SmokeBombSettings {
        if (durationTicks <= 0L) {
            throw new IllegalArgumentException("duration-ticks debe ser mayor a cero.");
        }
        if (width <= 0.0 || depth <= 0.0 || height <= 0.0) {
            throw new IllegalArgumentException("Las dimensiones del smoke bomb deben ser positivas.");
        }
        if (lingerTicks < 0L) {
            throw new IllegalArgumentException("linger-ticks no puede ser negativo.");
        }
        if (smokeBurstIntervalTicks <= 0L) {
            throw new IllegalArgumentException("smoke-burst-interval-ticks debe ser mayor a cero.");
        }
        if (impactParticles < 0 || cloudParticles < 0 || particleViewDistance <= 0.0 || occupancyIntervalTicks <= 0L) {
            throw new IllegalArgumentException("La configuración de rendimiento del humo no es válida.");
        }
    }

    public double halfWidth() {
        return width / 2.0;
    }

    public double halfDepth() {
        return depth / 2.0;
    }

    public double halfHeight() {
        return height / 2.0;
    }
}
