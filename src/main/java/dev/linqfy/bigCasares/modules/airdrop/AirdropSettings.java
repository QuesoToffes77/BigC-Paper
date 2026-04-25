package dev.linqfy.bigCasares.modules.airdrop;

import org.bukkit.configuration.file.FileConfiguration;

public record AirdropSettings(
        int intervalMinutes,
        int radius,
        int dropHeight
) {
    private static final String PREFIX = "airdrop-system.";

    public static AirdropSettings fromConfig(FileConfiguration config) {
        int intervalMinutes = requirePositive(PREFIX + "interval-minutes",
                config.getInt(PREFIX + "interval-minutes", 30));
        int radius = requirePositive(PREFIX + "radius",
                config.getInt(PREFIX + "radius", 500));
        int dropHeight = requirePositive(PREFIX + "drop-height",
                config.getInt(PREFIX + "drop-height", 30));

        return new AirdropSettings(intervalMinutes, radius, dropHeight);
    }

    private static int requirePositive(String key, int value) {
        if (value <= 0) {
            throw new IllegalArgumentException(key + " must be greater than 0, but was " + value);
        }
        return value;
    }
}
