package dev.linqfy.bigCasares.modules.airdrop;

import org.bukkit.configuration.file.FileConfiguration;

public record AirdropSettings(
        boolean automatic,
        int intervalMinutes,
        int radius,
        int dropHeight,
        AirdropMobSettings mobs,
        AirdropQualitySettings quality
) {
    private static final String PREFIX = "airdrop-system.";

    public AirdropSettings(boolean automatic, int intervalMinutes, int radius, int dropHeight) {
        this(automatic, intervalMinutes, radius, dropHeight,
            AirdropMobSettings.defaults(), AirdropQualitySettings.defaults());
    }

    public static AirdropSettings fromConfig(FileConfiguration config) {
        boolean automatic = config.getBoolean(PREFIX + "automatic", true);
        int intervalMinutes = requirePositive(PREFIX + "interval-minutes",
                config.getInt(PREFIX + "interval-minutes", 30));
        int radius = requirePositive(PREFIX + "radius",
                config.getInt(PREFIX + "radius", 10000));
        int dropHeight = requirePositive(PREFIX + "drop-height",
                config.getInt(PREFIX + "drop-height", 30));
        AirdropMobSettings mobs = AirdropMobSettings.fromConfig(config, PREFIX + "mobs.");
        AirdropQualitySettings quality = AirdropQualitySettings.fromConfig(config, PREFIX);

        return new AirdropSettings(automatic, intervalMinutes, radius, dropHeight, mobs, quality);
    }

    private static int requirePositive(String key, int value) {
        if (value <= 0) {
            throw new IllegalArgumentException(key + " must be greater than 0, but was " + value);
        }
        return value;
    }
}
