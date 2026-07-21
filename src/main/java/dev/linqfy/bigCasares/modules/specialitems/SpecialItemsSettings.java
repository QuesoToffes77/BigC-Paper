package dev.linqfy.bigCasares.modules.specialitems;

import org.bukkit.configuration.file.FileConfiguration;

public record SpecialItemsSettings(
    long trackingMillis,
    long cooldownMillis,
    long trackerUpdateTicks,
    double nukeHeight,
    int ringCount,
    int totalTnt,
    long ringIntervalTicks,
    double radiusStep
) {

    public SpecialItemsSettings {
        long minimumTnt = ringCount >= 31 ? Long.MAX_VALUE : 1L << ringCount;
        if (trackingMillis <= 0L || cooldownMillis <= 0L || trackerUpdateTicks <= 0L
            || nukeHeight <= 0.0 || ringCount <= 0 || ringCount > 30 || totalTnt < minimumTnt
            || ringIntervalTicks <= 0L || radiusStep <= 0.0) {
            throw new IllegalArgumentException("special-items configuration contains invalid values");
        }
    }

    public static SpecialItemsSettings load(FileConfiguration config) {
        return new SpecialItemsSettings(
            config.getLong("special-items.tracker-compass.tracking-seconds", 45L) * 1_000L,
            config.getLong("special-items.tracker-compass.cooldown-seconds", 60L) * 1_000L,
            config.getLong("special-items.tracker-compass.update-ticks", 20L),
            config.getDouble("special-items.nuke-shot.height", 50.0),
            config.getInt("special-items.nuke-shot.ring-count", 6),
            config.getInt("special-items.nuke-shot.total-tnt", 350),
            config.getLong("special-items.nuke-shot.ring-interval-ticks", 5L),
            config.getDouble("special-items.nuke-shot.radius-step", 3.0)
        );
    }
}
