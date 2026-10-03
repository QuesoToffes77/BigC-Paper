package dev.linqfy.bigCasares.modules.copperapple;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

public record CopperAppleSettings(
    boolean oxidationEnabled,
    int maxStackSize,
    long consumeCooldownMillis,
    long scanIntervalTicks,
    CopperAppleOxidationPolicy oxidationPolicy
) {
    private static final long DEFAULT_EXPOSED_SECONDS = 900L;
    private static final long DEFAULT_WEATHERED_SECONDS = 1_800L;
    private static final long DEFAULT_OXIDIZED_SECONDS = 3_600L;
    private static final long MAX_OXIDATION_SECONDS = 30L * 24L * 60L * 60L;

    public CopperAppleSettings {
        maxStackSize = maxStackSize < 1 || maxStackSize > 64 ? 64 : maxStackSize;
        consumeCooldownMillis = Math.max(0L, consumeCooldownMillis);
        scanIntervalTicks = Math.max(20L, Math.min(scanIntervalTicks, 1_200L));
        oxidationPolicy = Objects.requireNonNull(oxidationPolicy, "oxidationPolicy");
    }

    public static CopperAppleSettings load(ConfigurationSection config) {
        Objects.requireNonNull(config, "config");
        long exposed = safeSeconds(
            config.getLong("copper-apple.oxidation.exposed-after-seconds", DEFAULT_EXPOSED_SECONDS),
            DEFAULT_EXPOSED_SECONDS
        );
        long weathered = Math.max(exposed, safeSeconds(
            config.getLong("copper-apple.oxidation.weathered-after-seconds", DEFAULT_WEATHERED_SECONDS),
            DEFAULT_WEATHERED_SECONDS
        ));
        long oxidized = Math.max(weathered, safeSeconds(
            config.getLong("copper-apple.oxidation.oxidized-after-seconds", DEFAULT_OXIDIZED_SECONDS),
            DEFAULT_OXIDIZED_SECONDS
        ));
        return new CopperAppleSettings(
            config.getBoolean("copper-apple.oxidation.enabled", true),
            config.getInt("copper-apple.max-stack-size", 64),
            config.getLong("copper-apple.consume-cooldown-ms", 5_000L),
            config.getLong("copper-apple.oxidation.scan-interval-ticks", 100L),
            CopperAppleOxidationPolicy.fromSeconds(exposed, weathered, oxidized)
        );
    }

    private static long safeSeconds(long configured, long fallback) {
        if (configured < 0L) {
            return fallback;
        }
        return Math.min(configured, MAX_OXIDATION_SECONDS);
    }
}
