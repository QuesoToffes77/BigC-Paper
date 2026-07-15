package dev.linqfy.bigCasares.modules.moderation;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.Map;

public record ModerationSettings(
    Duration scoreWindow,
    int warningThreshold,
    int criticalThreshold,
    Duration alertCooldown,
    Duration retention,
    Set<String> sensitiveCommands,
    Set<Material> rareItems,
    Map<String, Integer> signalPoints
) {

    public static ModerationSettings load(ConfigurationSection config) {
        String root = "moderation-system.";
        Set<String> sensitive = new LinkedHashSet<>(config.getStringList(root + "sensitive-command-roots"));
        if (sensitive.isEmpty()) {
            sensitive.addAll(Set.of("op", "deop", "stop", "restart", "reload", "ban", "ban-ip", "whitelist", "lp", "luckperms", "pex", "execute"));
        }
        sensitive = sensitive.stream().map(value -> value.toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.toUnmodifiableSet());
        Set<Material> rare = new LinkedHashSet<>();
        for (String value : config.getStringList(root + "rare-items")) {
            Material material = Material.matchMaterial(value);
            if (material != null) {
                rare.add(material);
            }
        }
        if (rare.isEmpty()) {
            rare.addAll(Set.of(Material.NETHERITE_INGOT, Material.ELYTRA, Material.TOTEM_OF_UNDYING, Material.ENCHANTED_GOLDEN_APPLE));
        }
        Map<String, Integer> points = new java.util.HashMap<>();
        Map<String, Integer> defaults = Map.ofEntries(
            Map.entry("chat-rate", 20), Map.entry("chat-duplicate", 25),
            Map.entry("command-rate", 20), Map.entry("command-duplicate", 25),
            Map.entry("sensitive-command", 35), Map.entry("blocked-end", 20),
            Map.entry("blocked-rocket", 20), Map.entry("reconnect-rate", 20),
            Map.entry("ip-join-rate", 35), Map.entry("ip-account-rate", 40),
            Map.entry("pack-cache-timing", 15), Map.entry("movement-speed", 25),
            Map.entry("upward-movement", 30), Map.entry("melee-reach", 30),
            Map.entry("melee-rate", 25), Map.entry("combat-logout", 20),
            Map.entry("illegal-stack", 60), Map.entry("inventory-click-rate", 15),
            Map.entry("rare-item-gain", 25), Map.entry("security-log", 40)
        );
        defaults.forEach((rule, fallback) -> points.put(
            rule, Math.max(1, config.getInt(root + "signals." + rule + ".points", fallback))
        ));
        return new ModerationSettings(
            Duration.ofSeconds(config.getLong(root + "score-window-seconds", 60)),
            config.getInt(root + "warning-threshold", 50),
            config.getInt(root + "critical-threshold", 90),
            Duration.ofSeconds(config.getLong(root + "alert-cooldown-seconds", 120)),
            Duration.ofDays(config.getLong(root + "observation-retention-days", 30)),
            sensitive,
            Set.copyOf(rare),
            Map.copyOf(points)
        );
    }
}
