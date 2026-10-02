package dev.linqfy.bigCasares.modules.jeremy;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class JeremySettingsLoader {
    private JeremySettingsLoader() {
    }

    public static JeremyConfigLoadResult load(ConfigurationSection config) {
        ConfigurationSection root = config == null ? null : config.getConfigurationSection("jeremy");
        if (root == null) {
            return new JeremyConfigLoadResult(JeremySettings.defaults(), List.of(),
                List.of("jeremy section missing; using production-safe defaults"));
        }
        JeremySettings defaults = JeremySettings.defaults();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (!config.isSet("jeremy")) {
            warnings.add("jeremy section missing from the installed config; using production-safe defaults");
        }

        int huntSeconds = rangedInt(root, "timing.hunt-seconds", 10, 86_400,
            (int) (defaults.timing().huntMillis() / 1_000L), errors);
        int restSeconds = rangedInt(root, "timing.rest-seconds", 1, 604_800,
            (int) (defaults.timing().restMillis() / 1_000L), errors);
        int retrySeconds = rangedInt(root, "timing.no-player-retry-seconds", 1, 3_600,
            (int) (defaults.timing().noPlayerRetryMillis() / 1_000L), errors);
        int killCreditSeconds = rangedInt(root, "timing.kill-credit-seconds", 1, 30,
            (int) (defaults.timing().killCreditMillis() / 1_000L), errors);
        int celebrationTicks = rangedInt(root, "celebration.duration-ticks", 0, 400,
            defaults.celebration().durationTicks(), errors);
        JeremyTimingSettings timing = new JeremyTimingSettings(
            huntSeconds * 1_000L,
            restSeconds * 1_000L,
            retrySeconds * 1_000L,
            killCreditSeconds * 1_000L,
            celebrationTicks * 50L
        );

        Set<String> enabledWorlds = strings(root, "targeting.worlds.enabled", defaults.targeting().enabledWorlds());
        Set<String> excludedWorlds = strings(root, "targeting.worlds.excluded", defaults.targeting().excludedWorlds());
        if (enabledWorlds.isEmpty()) {
            warnings.add("jeremy.targeting.worlds.enabled is empty; all non-excluded worlds are eligible");
        }
        JeremyTargetingSettings targeting = new JeremyTargetingSettings(
            root.getBoolean("targeting.survival", defaults.targeting().survival()),
            root.getBoolean("targeting.adventure", defaults.targeting().adventure()),
            root.getBoolean("targeting.creative", defaults.targeting().creative()),
            root.getBoolean("targeting.spectator", defaults.targeting().spectator()),
            root.getBoolean("targeting.avoid-last-target", defaults.targeting().avoidLastTarget()),
            enabledWorlds,
            excludedWorlds
        );

        int minDistance = rangedInt(root, "spawn.min-distance", 8, 128,
            defaults.spawn().minDistance(), errors);
        int maxDistance = rangedInt(root, "spawn.max-distance", minDistance, 192,
            defaults.spawn().maxDistance(), errors);
        int attempts = rangedInt(root, "spawn.attempts", 1, 64, defaults.spawn().attempts(), errors);
        JeremySpawnSettings spawn = new JeremySpawnSettings(minDistance, maxDistance, attempts);

        double movementSpeed = rangedDouble(root, "movement.speed", 0.05, 1.0,
            defaults.movement().configuredSpeed(), errors);
        JeremyMovementSettings movement = new JeremyMovementSettings(
            root.getBoolean("movement.use-baby-zombie-speed", defaults.movement().useBabyZombieSpeed()),
            movementSpeed
        );

        double baseHealth = rangedDouble(root, "health.base", 1.0, 2_048.0, defaults.health().base(), errors);
        double healthPowerMultiplier = rangedDouble(root, "health.maximum-power-multiplier", 1.0, 3.0,
            defaults.health().maximumPowerMultiplier(), errors);
        JeremyHealthSettings health = new JeremyHealthSettings(baseHealth, healthPowerMultiplier);

        double minimumDamage = rangedDouble(root, "damage.minimum", 0.0, 100.0,
            defaults.damage().minimum(), errors);
        double maximumDamage = rangedDouble(root, "damage.maximum", minimumDamage, 100.0,
            defaults.damage().maximum(), errors);
        int recalculation = rangedInt(root, "damage.power-recalculation-ticks", 20, 1_200,
            defaults.damage().powerRecalculationTicks(), errors);
        List<Double> thresholds = doubles(root, "damage.score-thresholds", defaults.damage().scoreThresholds());
        List<Double> tierDamage = doubles(root, "damage.tier-values", defaults.damage().tierDamage());
        if (thresholds.size() != 4 || tierDamage.size() != 5 || !strictlyIncreasing(thresholds)) {
            errors.add("jeremy.damage score-thresholds/tier-values must contain 4 increasing thresholds and 5 values");
            thresholds = defaults.damage().scoreThresholds();
            tierDamage = defaults.damage().tierDamage();
        }
        if (tierDamage.stream().anyMatch(value -> !finiteBetween(value, minimumDamage, maximumDamage))) {
            errors.add("jeremy.damage.tier-values must stay between minimum and maximum");
            tierDamage = defaults.damage().tierDamage();
        }
        JeremyDamageSettings damage = new JeremyDamageSettings(
            minimumDamage, maximumDamage, recalculation, thresholds, tierDamage);

        JeremyGoldenHelmetSettings goldenHelmet = new JeremyGoldenHelmetSettings(
            rangedDouble(root, "golden-helmet.damage-multiplier", 1.0, 5.0,
                defaults.goldenHelmet().damageMultiplier(), errors));

        int stuckSeconds = rangedInt(root, "pathfinding.stuck-detection-seconds", 1, 30,
            defaults.pathfinding().stuckDetectionTicks() / 20, errors);
        double minimumProgress = rangedDouble(root, "pathfinding.minimum-progress", 0.1, 20.0,
            defaults.pathfinding().minimumProgress(), errors);
        int sampleInterval = rangedInt(root, "pathfinding.sample-interval-ticks", 5, 100,
            defaults.pathfinding().sampleIntervalTicks(), errors);
        JeremyPathfindingSettings pathfinding = new JeremyPathfindingSettings(
            stuckSeconds * 20, minimumProgress, sampleInterval);

        JeremyWallPenetrationSettings walls = new JeremyWallPenetrationSettings(
            root.getBoolean("ultrasound.wall-penetration.enabled", defaults.ultrasound().wallPenetration().enabled()),
            rangedInt(root, "ultrasound.wall-penetration.max-blocks", 0, 8,
                defaults.ultrasound().wallPenetration().maxBlocks(), errors)
        );
        JeremyUltrasoundSettings ultrasound = new JeremyUltrasoundSettings(
            root.getBoolean("ultrasound.enabled", defaults.ultrasound().enabled()),
            rangedDouble(root, "ultrasound.range", 4.0, 128.0, defaults.ultrasound().range(), errors),
            rangedInt(root, "ultrasound.charge-ticks", 1, 200, defaults.ultrasound().chargeTicks(), errors),
            rangedInt(root, "ultrasound.cooldown-ticks", 1, 1_200, defaults.ultrasound().cooldownTicks(), errors),
            rangedDouble(root, "ultrasound.damage-multiplier-vs-melee", 0.0, 2.0,
                defaults.ultrasound().damageMultiplierVsMelee(), errors),
            rangedDouble(root, "ultrasound.knockback", 0.0, 2.0,
                defaults.ultrasound().knockback(), errors),
            walls
        );

        JeremyCelebrationSettings celebration = new JeremyCelebrationSettings(
            root.getBoolean("celebration.enabled", defaults.celebration().enabled()),
            celebrationTicks,
            (float) rangedDouble(root, "celebration.rotation-degrees-per-tick", 1.0, 90.0,
                defaults.celebration().rotationDegreesPerTick(), errors)
        );
        JeremyRepositionSettings reposition = new JeremyRepositionSettings(
            rangedDouble(root, "reposition.distance", 64.0, 1_024.0,
                defaults.reposition().distance(), errors),
            rangedInt(root, "reposition.delay-ticks", 0, 1_200,
                defaults.reposition().delayTicks(), errors)
        );
        int minimumEmeralds = rangedInt(
            root, "loot.emeralds.minimum", 0, 64, defaults.loot().minimumEmeralds(), errors);
        int maximumEmeralds = rangedInt(
            root, "loot.emeralds.maximum", 0, 64, defaults.loot().maximumEmeralds(), errors);
        int minimumGold = rangedInt(
            root, "loot.gold-ingots.minimum", 0, 64, defaults.loot().minimumGoldIngots(), errors);
        int maximumGold = rangedInt(
            root, "loot.gold-ingots.maximum", 0, 64, defaults.loot().maximumGoldIngots(), errors);
        if (maximumEmeralds < minimumEmeralds || maximumGold < minimumGold) {
            errors.add("jeremy.loot maximum values must be greater than or equal to minimum values");
            minimumEmeralds = defaults.loot().minimumEmeralds();
            maximumEmeralds = defaults.loot().maximumEmeralds();
            minimumGold = defaults.loot().minimumGoldIngots();
            maximumGold = defaults.loot().maximumGoldIngots();
        }
        JeremyLootSettings loot = new JeremyLootSettings(
            root.getBoolean("loot.enabled", defaults.loot().enabled()),
            rangedInt(root, "loot.experience", 0, 10_000, defaults.loot().experience(), errors),
            minimumEmeralds,
            maximumEmeralds,
            minimumGold,
            maximumGold,
            rangedDouble(root, "loot.diamond.chance", 0.0, 1.0, defaults.loot().diamondChance(), errors),
            rangedInt(root, "loot.diamond.amount", 0, 8, defaults.loot().diamondAmount(), errors)
        );
        JeremySoundSettings sounds = new JeremySoundSettings(
            root.getBoolean("sounds.enabled", defaults.sounds().enabled()),
            root.getString("sounds.spawn", defaults.sounds().spawn()),
            root.getString("sounds.ultrasound-charge", defaults.sounds().ultrasoundCharge()),
            root.getString("sounds.ultrasound-fire", defaults.sounds().ultrasoundFire()),
            root.getString("sounds.celebration", defaults.sounds().celebration()),
            (float) rangedDouble(root, "sounds.volume", 0.0, 2.0, defaults.sounds().volume(), errors),
            (float) rangedDouble(root, "sounds.pitch", 0.0, 2.0, defaults.sounds().pitch(), errors)
        );

        JeremySettings loaded = new JeremySettings(
            root.getBoolean("enabled", defaults.enabled()),
            root.getString("display-name", defaults.displayName()),
            timing, targeting, spawn, movement, health, damage, goldenHelmet, pathfinding,
            ultrasound, celebration, reposition, loot, sounds,
            root.getBoolean("debug", defaults.debug())
        );
        if (!errors.isEmpty()) {
            return new JeremyConfigLoadResult(JeremySettings.disabled(), List.copyOf(errors), List.copyOf(warnings));
        }
        return new JeremyConfigLoadResult(loaded, List.of(), List.copyOf(warnings));
    }

    private static int rangedInt(
        ConfigurationSection root, String path, int minimum, int maximum, int fallback, List<String> errors
    ) {
        int value = root.getInt(path, fallback);
        if (value < minimum || value > maximum) {
            errors.add("jeremy." + path + " must be between " + minimum + " and " + maximum);
            return fallback;
        }
        return value;
    }

    private static double rangedDouble(
        ConfigurationSection root, String path, double minimum, double maximum, double fallback, List<String> errors
    ) {
        double value = root.getDouble(path, fallback);
        if (!finiteBetween(value, minimum, maximum)) {
            errors.add("jeremy." + path + " must be between " + minimum + " and " + maximum);
            return fallback;
        }
        return value;
    }

    private static boolean finiteBetween(double value, double minimum, double maximum) {
        return Double.isFinite(value) && value >= minimum && value <= maximum;
    }

    private static Set<String> strings(ConfigurationSection root, String path, Set<String> fallback) {
        if (!root.isSet(path) || !root.isList(path)) {
            return fallback;
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String raw : root.getStringList(path)) {
            if (raw != null && !raw.isBlank()) {
                values.add(raw.trim());
            }
        }
        return Set.copyOf(values);
    }

    private static List<Double> doubles(ConfigurationSection root, String path, List<Double> fallback) {
        if (!root.isSet(path)) {
            return fallback;
        }
        List<?> configured = root.getList(path);
        if (configured == null) {
            return List.of();
        }
        List<Double> values = new ArrayList<>();
        for (Object raw : configured) {
            if (raw instanceof Number number) {
                values.add(number.doubleValue());
            } else {
                return fallback;
            }
        }
        return List.copyOf(values);
    }

    private static boolean strictlyIncreasing(List<Double> values) {
        for (int index = 0; index < values.size(); index++) {
            if (!Double.isFinite(values.get(index)) || values.get(index) < 0.0
                || index > 0 && values.get(index) <= values.get(index - 1)) {
                return false;
            }
        }
        return true;
    }
}

record JeremyConfigLoadResult(JeremySettings settings, List<String> errors, List<String> warnings) {
    JeremyConfigLoadResult {
        settings = settings == null ? JeremySettings.disabled() : settings;
        errors = errors == null ? List.of() : List.copyOf(errors);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    boolean valid() {
        return errors.isEmpty();
    }
}
