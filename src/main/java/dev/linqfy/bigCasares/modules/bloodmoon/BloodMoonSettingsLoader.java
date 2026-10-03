package dev.linqfy.bigCasares.modules.bloodmoon;

import dev.linqfy.bigCasares.modules.environment.EnvironmentalVisualQuality;
import dev.linqfy.bigCasares.modules.environment.EnvironmentalVisualSettings;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class BloodMoonSettingsLoader {
    private BloodMoonSettingsLoader() {
    }

    public static BloodMoonConfigLoadResult load(ConfigurationSection config) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        ConfigurationSection root = config == null ? null : config.getConfigurationSection("blood-moon");
        if (root == null) {
            return new BloodMoonConfigLoadResult(
                BloodMoonSettings.disabled(), List.of("blood-moon section missing"), List.of());
        }

        BloodMoonSettings defaults = BloodMoonSettings.defaults();
        BloodMoonSchedulingMode mode = BloodMoonSchedulingMode.parse(
            root.getString("scheduling.mode", defaults.scheduling().mode().name()))
            .orElseGet(() -> {
                errors.add("blood-moon.scheduling.mode is invalid");
                return BloodMoonSchedulingMode.CHANCE;
            });
        double chance = root.getDouble("scheduling.chance-per-night", defaults.scheduling().chancePerNight());
        int minimumNights = root.getInt("scheduling.minimum-normal-nights-between-events",
            defaults.scheduling().minimumNormalNightsBetweenEvents());
        int everyNights = root.getInt("scheduling.every-nights", defaults.scheduling().everyNights());
        if (!finiteBetween(chance, 0.0, 1.0) || minimumNights < 0 || everyNights < 1) {
            errors.add("blood-moon.scheduling contains invalid values");
        }
        BloodMoonSchedulingSettings scheduling = new BloodMoonSchedulingSettings(
            mode, safe(chance, defaults.scheduling().chancePerNight()),
            Math.max(0, minimumNights), Math.max(1, everyNights));

        double health = root.getDouble("mobs.health-multiplier", defaults.mobs().healthMultiplier());
        double speed = root.getDouble("mobs.movement-speed-multiplier", defaults.mobs().movementSpeedMultiplier());
        double damage = root.getDouble("mobs.damage-multiplier", defaults.mobs().damageMultiplier());
        if (!finiteBetween(health, 1.0, 4.0)
            || !finiteBetween(speed, 1.0, 3.0)
            || !finiteBetween(damage, 0.0, 4.0)) {
            errors.add("blood-moon.mobs contains invalid multipliers");
        }
        BloodMoonMobSettings mobs = new BloodMoonMobSettings(
            safeRange(health, 1.0, 4.0, defaults.mobs().healthMultiplier()),
            safeRange(speed, 1.0, 3.0, defaults.mobs().movementSpeedMultiplier()),
            safeRange(damage, 0.0, 4.0, defaults.mobs().damageMultiplier()),
            strings(root, "mobs.whitelist", defaults.mobs().whitelist()),
            strings(root, "mobs.blacklist", defaults.mobs().blacklist())
        );

        double multiplier = root.getDouble("spawning.multiplier", defaults.spawning().multiplier());
        int interval = root.getInt("spawning.interval-ticks", defaults.spawning().intervalTicks());
        int minimumDistance = root.getInt("spawning.min-distance-from-player",
            defaults.spawning().minDistanceFromPlayer());
        int maximumDistance = root.getInt("spawning.max-distance-from-player",
            defaults.spawning().maxDistanceFromPlayer());
        int perPlayer = root.getInt("spawning.max-extra-hostiles-per-player",
            defaults.spawning().maxExtraHostilesPerPlayer());
        int perWorld = root.getInt("spawning.max-extra-hostiles-per-world",
            defaults.spawning().maxExtraHostilesPerWorld());
        if (!finiteBetween(multiplier, 1.0, 4.0) || interval < 20 || interval > 1200
            || minimumDistance < 8 || maximumDistance < minimumDistance || maximumDistance > 128
            || perPlayer < 0 || perPlayer > 100 || perWorld < 0 || perWorld > 1000) {
            errors.add("blood-moon.spawning contains invalid or unsafe values");
        }
        List<String> types = root.isList("spawning.types")
            ? root.getStringList("spawning.types")
            : defaults.spawning().types();
        BloodMoonSpawningSettings spawning = new BloodMoonSpawningSettings(
            safeRange(multiplier, 1.0, 4.0, defaults.spawning().multiplier()),
            safeInt(interval, 20, 1200, defaults.spawning().intervalTicks()),
            safeInt(minimumDistance, 8, 128, defaults.spawning().minDistanceFromPlayer()),
            safeInt(maximumDistance, Math.max(8, minimumDistance), 128, defaults.spawning().maxDistanceFromPlayer()),
            safeInt(perPlayer, 0, 100, 0),
            safeInt(perWorld, 0, 1000, 0),
            types
        );

        EnvironmentalVisualQuality quality = EnvironmentalVisualQuality.parse(
            root.getString("visuals.quality", defaults.visuals().quality().name()))
            .orElseGet(() -> {
                errors.add("blood-moon.visuals.quality is invalid");
                return EnvironmentalVisualQuality.LOW;
            });
        double horizontal = root.getDouble("visuals.horizontal-radius", defaults.visuals().horizontalRadius());
        double vertical = root.getDouble("visuals.vertical-radius", defaults.visuals().verticalRadius());
        int visualInterval = root.getInt("visuals.interval-ticks", defaults.visuals().intervalTicks());
        if (!finiteBetween(horizontal, 2.0, 32.0) || !finiteBetween(vertical, 2.0, 20.0)
            || visualInterval < 2 || visualInterval > 100) {
            errors.add("blood-moon.visuals contains invalid values");
        }
        EnvironmentalVisualSettings visuals = new EnvironmentalVisualSettings(
            root.getBoolean("visuals.enabled", defaults.visuals().enabled()),
            quality,
            safeRange(horizontal, 2.0, 32.0, defaults.visuals().horizontalRadius()),
            safeRange(vertical, 2.0, 20.0, defaults.visuals().verticalRadius()),
            safeInt(visualInterval, 2, 100, defaults.visuals().intervalTicks())
        );

        double lootChance = root.getDouble("loot.bonus-chance", defaults.loot().bonusChance());
        int lootMinimum = root.getInt("loot.minimum-amount", defaults.loot().minimumAmount());
        int lootMaximum = root.getInt("loot.maximum-amount", defaults.loot().maximumAmount());
        int xpMinimum = root.getInt("loot.minimum-experience", defaults.loot().minimumExperience());
        int xpMaximum = root.getInt("loot.maximum-experience", defaults.loot().maximumExperience());
        if (!finiteBetween(lootChance, 0.0, 1.0) || lootMinimum < 0 || lootMaximum < lootMinimum
            || lootMaximum > 64 || xpMinimum < 0 || xpMaximum < xpMinimum || xpMaximum > 100) {
            errors.add("blood-moon.loot contains invalid values");
        }
        BloodMoonLootSettings loot = new BloodMoonLootSettings(
            root.getBoolean("loot.enabled", defaults.loot().enabled()),
            safeRange(lootChance, 0.0, 1.0, defaults.loot().bonusChance()),
            safeInt(lootMinimum, 0, 64, defaults.loot().minimumAmount()),
            safeInt(lootMaximum, Math.max(0, lootMinimum), 64, defaults.loot().maximumAmount()),
            safeInt(xpMinimum, 0, 100, defaults.loot().minimumExperience()),
            safeInt(xpMaximum, Math.max(0, xpMinimum), 100, defaults.loot().maximumExperience()),
            materials(root, "loot.materials", defaults.loot().materials(), warnings)
        );

        BloodMoonSoundSettings sounds = new BloodMoonSoundSettings(
            root.getBoolean("sounds.enabled", defaults.sounds().enabled()),
            root.getString("sounds.start", defaults.sounds().startSound()),
            root.getString("sounds.stop", defaults.sounds().stopSound()),
            (float) root.getDouble("sounds.volume", defaults.sounds().volume()),
            (float) root.getDouble("sounds.pitch", defaults.sounds().pitch())
        );
        Set<String> worlds = strings(root, "worlds", defaults.worlds());
        if (worlds.isEmpty()) {
            warnings.add("blood-moon.worlds is empty; automatic events cannot start");
        }

        BloodMoonSettings loaded = new BloodMoonSettings(
            root.getBoolean("enabled", defaults.enabled()), scheduling, worlds, mobs, spawning, visuals, loot, sounds,
            root.getBoolean("allow-with-other-events", defaults.allowWithOtherEvents()),
            root.getBoolean("debug", defaults.debug())
        );
        if (!errors.isEmpty()) {
            BloodMoonSettings disabled = BloodMoonSettings.disabled();
            return new BloodMoonConfigLoadResult(new BloodMoonSettings(
                false, scheduling, worlds, mobs, BloodMoonSpawningSettings.disabled(),
                EnvironmentalVisualSettings.disabled(), BloodMoonLootSettings.disabled(), sounds, false, loaded.debug()),
                List.copyOf(errors), List.copyOf(warnings));
        }
        return new BloodMoonConfigLoadResult(loaded, List.of(), List.copyOf(warnings));
    }

    private static Set<String> strings(ConfigurationSection root, String path, Set<String> defaults) {
        if (!root.isList(path)) {
            return defaults;
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String value : root.getStringList(path)) {
            if (value != null && !value.isBlank()) {
                values.add(value.trim());
            }
        }
        return Set.copyOf(values);
    }

    private static List<Material> materials(
        ConfigurationSection root,
        String path,
        List<Material> defaults,
        List<String> warnings
    ) {
        if (!root.isList(path)) {
            return defaults;
        }
        List<Material> materials = new ArrayList<>();
        for (String configured : root.getStringList(path)) {
            Material material = Material.matchMaterial(configured == null ? "" : configured);
            if (material == null || material == Material.AIR
                || material == Material.CAVE_AIR || material == Material.VOID_AIR) {
                warnings.add("blood-moon." + path + " ignores invalid material: " + configured);
            } else {
                materials.add(material);
            }
        }
        return List.copyOf(materials);
    }

    private static boolean finiteBetween(double value, double minimum, double maximum) {
        return Double.isFinite(value) && value >= minimum && value <= maximum;
    }

    private static double safe(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }

    private static double safeRange(double value, double minimum, double maximum, double fallback) {
        return finiteBetween(value, minimum, maximum) ? value : fallback;
    }

    private static int safeInt(int value, int minimum, int maximum, int fallback) {
        return value >= minimum && value <= maximum ? value : fallback;
    }
}

record BloodMoonConfigLoadResult(BloodMoonSettings settings, List<String> errors, List<String> warnings) {
    BloodMoonConfigLoadResult {
        settings = settings == null ? BloodMoonSettings.disabled() : settings;
        errors = errors == null ? List.of() : List.copyOf(errors);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    boolean valid() {
        return errors.isEmpty();
    }
}
