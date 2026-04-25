package dev.linqfy.bigCasares.modules.customcrossbow;

import org.bukkit.configuration.file.FileConfiguration;

public record CustomCrossbowSettings(
    double sonicRange,
    double sonicRadius,
    double sonicDamage,
    int echoShardCooldownTicks,
    int maxEchoChargedCrossbows,
    int wardenDropMin,
    int wardenDropMax
) {

    public static CustomCrossbowSettings load(FileConfiguration config) {
        double sonicRange = config.getDouble("custom-crossbow.sonic.range", 32.0);
        double sonicRadius = config.getDouble("custom-crossbow.sonic.radius", 1.0);
        double sonicDamage = config.getDouble("custom-crossbow.sonic.damage", CustomCrossbowRules.ECHO_SHARD_DAMAGE);
        int echoShardCooldownTicks = Math.max(0, config.getInt("custom-crossbow.echo-shard.cooldown-ticks", CustomCrossbowRules.defaultEchoShardCooldownTicks()));
        int maxEchoChargedCrossbows = Math.max(0, config.getInt("custom-crossbow.echo-charged-crossbows.max", 3));
        int dropMin = Math.max(1, config.getInt("custom-crossbow.warden-echo-shards.min", 1));
        int dropMax = Math.max(dropMin, config.getInt("custom-crossbow.warden-echo-shards.max", 3));
        return new CustomCrossbowSettings(sonicRange, sonicRadius, sonicDamage, echoShardCooldownTicks, maxEchoChargedCrossbows, dropMin, dropMax);
    }
}
