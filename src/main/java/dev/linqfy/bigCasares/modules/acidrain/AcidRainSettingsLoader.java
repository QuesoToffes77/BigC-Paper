package dev.linqfy.bigCasares.modules.acidrain;

import dev.linqfy.bigCasares.modules.environment.EnvironmentalVisualQuality;
import dev.linqfy.bigCasares.modules.environment.EnvironmentalVisualSettings;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AcidRainSettingsLoader {
    private static final int MAX_REASONABLE_DURATION_SECONDS = 7200;
    private static final int MAX_DESTRUCTION_RADIUS = 64;
    private static final int MAX_CANDIDATES_PER_CYCLE = 512;
    private static final int MAX_BLOCKS_PER_SECOND = 120;
    private static final int MAX_BLOCKS_PER_EVENT = 10000;
    private static final int MAX_VERTICAL_SCAN_DEPTH = 128;
    private static final int MAX_MOB_SPAWN_INTERVAL_TICKS = 600;
    private static final int MAX_MOB_ATTEMPTS_PER_CYCLE = 32;
    private static final int MAX_MOB_MAX_ACTIVE = 200;
    private static final int MAX_MOB_RADIUS = 64;

    private AcidRainSettingsLoader() {
    }

    public static AcidRainConfigLoadResult load(ConfigurationSection config) {
        return loadSection(resolveAcidRainSection(config));
    }

    public static AcidRainConfigLoadResult loadSection(ConfigurationSection root) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        AcidRainSettings defaults = AcidRainSettings.safeDefaults();
        if (root == null) {
            return new AcidRainConfigLoadResult(defaults, List.of(), List.of("acid-rain section missing; defaults used"));
        }

        AcidRainLevel defaultLevel = AcidRainLevel.parse(root.getString("default-level", defaults.defaultLevel().name()))
            .orElseGet(() -> {
                errors.add("acid-rain.default-level is invalid");
                return AcidRainLevel.ACID;
            });
        AcidRainDurationSettings duration = loadDuration(root, defaults.duration(), errors, warnings);
        Map<AcidRainLevel, AcidRainLevelSettings> levels = loadLevels(root, defaults.levels(), errors);
        AcidRainWarningSettings warning = loadWarning(root, defaults.warning());
        AcidRainEscalationSettings escalation = loadEscalation(root, defaults.escalation(), warnings);
        AcidRainDamageSettings damage = new AcidRainDamageSettings(root.getBoolean("damage.enabled", defaults.damage().enabled()));
        AcidRainWorldSettings worlds = loadWorlds(root, defaults.worlds());
        AcidRainEnvironmentSettings environment = loadEnvironment(root, defaults.environment(), errors, warnings);
        AcidRainProtectionSettings protection = loadProtection(root, defaults.protection(), errors, warnings);
        AcidRainFeedbackSettings feedback = loadFeedback(root, defaults.feedback());
        AcidRainAutomaticSettings automatic = loadAutomatic(root, defaults.automatic(), errors);
        AcidRainWaterSettings water = new AcidRainWaterSettings(root.getBoolean("water.enabled", defaults.water().enabled()));
        AcidRainMobSettings mobs = loadMobs(root, defaults.mobs(), errors, warnings);

        AcidRainSettings loaded = new AcidRainSettings(defaultLevel, levels, duration, warning, escalation, damage,
            worlds, environment, protection, feedback, automatic, water, mobs);
        if (!errors.isEmpty()) {
            AcidRainSettings safe = AcidRainSettings.safeDisabled()
                .withDefaultLevel(defaultLevel)
                .withWorlds(worlds)
                .withWarning(warning)
                .withFeedback(feedback);
            return new AcidRainConfigLoadResult(safe, List.copyOf(errors), List.copyOf(warnings));
        }
        return new AcidRainConfigLoadResult(loaded, List.of(), List.copyOf(warnings));
    }

    private static AcidRainDurationSettings loadDuration(
        ConfigurationSection root,
        AcidRainDurationSettings defaults,
        List<String> errors,
        List<String> warnings
    ) {
        int minimum = root.getInt("duration.minimum-seconds", defaults.minimumSeconds());
        int maximum = root.getInt("duration.maximum-seconds", defaults.maximumSeconds());
        int ending = root.getInt("duration.ending-seconds", defaults.endingSeconds());
        if (minimum < 0 || maximum < minimum || ending < 0) {
            errors.add("acid-rain.duration contains invalid negative or inverted values");
            return defaults;
        }
        if (maximum > MAX_REASONABLE_DURATION_SECONDS) {
            warnings.add("acid-rain.duration.maximum-seconds capped at " + MAX_REASONABLE_DURATION_SECONDS);
            maximum = MAX_REASONABLE_DURATION_SECONDS;
        }
        return new AcidRainDurationSettings(minimum, maximum, ending);
    }

    private static Map<AcidRainLevel, AcidRainLevelSettings> loadLevels(
        ConfigurationSection root,
        Map<AcidRainLevel, AcidRainLevelSettings> defaults,
        List<String> errors
    ) {
        EnumMap<AcidRainLevel, AcidRainLevelSettings> loaded = new EnumMap<>(AcidRainLevel.class);
        int globalCandidates = root.getInt(
            "environment.destruction.candidates-per-cycle",
            AcidRainSettings.safeDefaults().environment().destruction().candidatesPerCycle()
        );
        for (AcidRainLevel level : AcidRainLevel.values()) {
            String path = "levels." + level.name().toLowerCase(java.util.Locale.ROOT);
            AcidRainLevelSettings defaultSettings = defaults.get(level);
            double damage = root.getDouble(path + ".damage", defaultSettings.damage());
            int interval = root.getInt(path + ".interval-seconds", defaultSettings.intervalSeconds());
            double contamination = root.getDouble(path + ".contamination-intensity", defaultSettings.contaminationIntensity());
            List<String> effects = root.getStringList(path + ".effects");
            if (!root.isList(path + ".effects")) {
                effects = defaultSettings.effects();
            }
            int intervalTicks = root.getInt(path + ".destruction.interval-ticks",
                defaultSettings.destruction().intervalTicks());
            int candidates = root.getInt(path + ".destruction.candidates-per-cycle", globalCandidates);
            if (candidates < 0) {
                errors.add("acid-rain." + path + ".destruction.candidates-per-cycle is negative");
                candidates = defaultSettings.destruction().candidatesPerCycle();
            }
            if (!Double.isFinite(damage) || !Double.isFinite(contamination)
                || damage < 0.0 || interval <= 0 || contamination < 0.0 || intervalTicks < 1) {
                errors.add("acid-rain." + path + " has invalid damage, interval, contamination, or destruction tuning");
                loaded.put(level, defaultSettings);
            } else {
                loaded.put(level, new AcidRainLevelSettings(
                    damage, interval, contamination, effects,
                    new AcidRainLevelDestructionSettings(intervalTicks, candidates)
                ));
            }
        }
        return loaded;
    }

    private static AcidRainWarningSettings loadWarning(ConfigurationSection root, AcidRainWarningSettings defaults) {
        return new AcidRainWarningSettings(
            Math.max(0, root.getInt("warning.duration-seconds", defaults.durationSeconds())),
            Math.max(1, root.getInt("warning.message-interval-seconds", defaults.messageIntervalSeconds())),
            root.isList("warning.messages") ? root.getStringList("warning.messages") : defaults.messages(),
            loadSound(root, "sounds.warning", defaults.sound()),
            root.getBoolean("warning.bossbar", defaults.bossBar())
        );
    }

    private static AcidRainEscalationSettings loadEscalation(
        ConfigurationSection root,
        AcidRainEscalationSettings defaults,
        List<String> warnings
    ) {
        boolean enabled = root.getBoolean("escalation.enabled", defaults.enabled());
        List<AcidRainEscalationStep> steps = new ArrayList<>();
        if (root.isList("escalation.steps")) {
            for (Map<?, ?> raw : root.getMapList("escalation.steps")) {
                AcidRainLevel level = AcidRainLevel.parse(String.valueOf(raw.get("level"))).orElse(null);
                int after = parseInt(raw.get("after-seconds"), -1);
                if (level == null || after < 0) {
                    warnings.add("invalid acid-rain escalation step ignored");
                    continue;
                }
                steps.add(new AcidRainEscalationStep(level, after));
            }
        } else {
            steps.addAll(defaults.steps());
        }
        return new AcidRainEscalationSettings(enabled, steps);
    }

    private static AcidRainWorldSettings loadWorlds(ConfigurationSection root, AcidRainWorldSettings defaults) {
        Set<String> enabled = root.isList("worlds.enabled")
            ? new LinkedHashSet<>(root.getStringList("worlds.enabled"))
            : defaults.enabled();
        Set<String> excluded = root.isList("worlds.excluded")
            ? new LinkedHashSet<>(root.getStringList("worlds.excluded"))
            : defaults.excluded();
        return new AcidRainWorldSettings(enabled, excluded);
    }

    private static AcidRainEnvironmentSettings loadEnvironment(
        ConfigurationSection root,
        AcidRainEnvironmentSettings defaults,
        List<String> errors,
        List<String> warnings
    ) {
        AcidRainDestructionSettings base = defaults.destruction();
        boolean enabled = root.getBoolean("environment.destruction.enabled", base.enabled());
        int radius = clampInt(root.getInt("environment.destruction.radius", base.radius()),
            0, MAX_DESTRUCTION_RADIUS, "environment.destruction.radius", warnings);
        int maxPerEvent = clampInt(root.getInt("environment.destruction.max-blocks-per-event", base.maxBlocksPerEvent()),
            0, MAX_BLOCKS_PER_EVENT, "environment.destruction.max-blocks-per-event", warnings);
        int maxPerSecond = clampInt(root.getInt("environment.destruction.max-blocks-per-second", base.maxBlocksPerSecond()),
            0, MAX_BLOCKS_PER_SECOND, "environment.destruction.max-blocks-per-second", warnings);
        int candidates = clampInt(root.getInt("environment.destruction.candidates-per-cycle", base.candidatesPerCycle()),
            0, MAX_CANDIDATES_PER_CYCLE, "environment.destruction.candidates-per-cycle", warnings);
        int verticalScanDepth = clampInt(
            root.getInt("environment.destruction.vertical-scan-depth", base.verticalScanDepth()),
            0, MAX_VERTICAL_SCAN_DEPTH, "environment.destruction.vertical-scan-depth", warnings);
        boolean crops = root.getBoolean("environment.destruction.crops.enabled", base.cropsEnabled());
        if (radius < 0 || maxPerEvent < 0 || maxPerSecond < 0 || candidates < 0 || verticalScanDepth < 0) {
            errors.add("acid-rain.environment.destruction contains negative values");
            return defaults.withDestruction(base.withEnabled(false));
        }
        Set<Material> whitelist = root.isList("environment.destruction.whitelist")
            ? parseMaterials(root.getStringList("environment.destruction.whitelist"), warnings)
            : base.whitelist();
        AcidRainDestructionSettings destruction = new AcidRainDestructionSettings(
            enabled, radius, maxPerEvent, maxPerSecond, candidates, verticalScanDepth, whitelist, crops
        );
        return new AcidRainEnvironmentSettings(destruction, root.getBoolean("water.enabled", defaults.waterEnabled()));
    }

    private static int clampInt(int value, int minimum, int maximum, String path, List<String> warnings) {
        if (value < minimum) {
            return value;
        }
        if (value > maximum) {
            warnings.add("acid-rain." + path + " capped at " + maximum);
            return maximum;
        }
        return value;
    }

    private static AcidRainProtectionSettings loadProtection(
        ConfigurationSection root,
        AcidRainProtectionSettings defaults,
        List<String> errors,
        List<String> warnings
    ) {
        ConfigurationSection section = root.getConfigurationSection("protective-items");
        if (section == null) {
            return defaults;
        }
        List<AcidRainProtectionGroup> groups = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            String path = "protective-items." + key;
            double protection = root.getDouble(path + ".protection", 0.0);
            if (!Double.isFinite(protection) || protection < 0.0) {
                errors.add("acid-rain." + path + ".protection is negative or not finite");
                continue;
            }
            protection = Math.min(100.0, protection);
            List<Material> armor = parseMaterials(root.getStringList(path + ".required-armor"), warnings).stream().toList();
            Set<Material> carried = parseMaterials(root.getStringList(path + ".carried-materials"), warnings);
            groups.add(new AcidRainProtectionGroup(key, protection, armor, carried));
        }
        return new AcidRainProtectionSettings(groups);
    }

    private static AcidRainFeedbackSettings loadFeedback(ConfigurationSection root, AcidRainFeedbackSettings defaults) {
        AcidRainBossBarSettings bossBar = new AcidRainBossBarSettings(
            root.getBoolean("bossbar.enabled", defaults.bossBar().enabled()),
            root.getString("bossbar.color", defaults.bossBar().color()),
            root.getString("bossbar.style", defaults.bossBar().style()),
            root.getString("bossbar.colors.warning", defaults.bossBar().warningColor()),
            root.getString("bossbar.colors.active", defaults.bossBar().activeColor()),
            root.getString("bossbar.colors.ending", defaults.bossBar().endingColor())
        );
        AcidRainActionBarSettings actionBar = new AcidRainActionBarSettings(
            root.getBoolean("actionbar.enabled", defaults.actionBar().enabled()),
            Math.max(1, root.getInt("actionbar.interval-ticks", defaults.actionBar().intervalTicks()))
        );
        EnumMap<AcidRainLevel, AcidRainLevelParticleSettings> levelParticles = new EnumMap<>(AcidRainLevel.class);
        for (AcidRainLevel level : AcidRainLevel.values()) {
            String path = "particles.levels." + level.name().toLowerCase(java.util.Locale.ROOT);
            AcidRainLevelParticleSettings levelDefault = defaults.particles().forLevel(level);
            String particle = root.getString(path + ".particle", levelDefault.particle());
            int count = root.getInt(path + ".count", levelDefault.count());
            levelParticles.put(level, new AcidRainLevelParticleSettings(particle, count));
        }
        AcidRainParticleSettings particles = new AcidRainParticleSettings(
            root.getBoolean("particles.enabled", defaults.particles().enabled()),
            root.getString("particles.particle", defaults.particles().particle()),
            Math.max(0, root.getInt("particles.count", defaults.particles().count())),
            Math.max(1, root.getInt("particles.interval-ticks", defaults.particles().intervalTicks())),
            levelParticles
        );
        AcidRainImpactSettings impacts = new AcidRainImpactSettings(
            root.getBoolean("impacts.enabled", defaults.impacts().enabled()),
            Math.max(0, Math.min(16, root.getInt("impacts.count", defaults.impacts().count())))
        );
        EnvironmentalVisualSettings visuals = new EnvironmentalVisualSettings(
            root.getBoolean("visuals.enabled", defaults.visuals().enabled()),
            EnvironmentalVisualQuality.parse(root.getString("visuals.quality", defaults.visuals().quality().name()))
                .orElse(defaults.visuals().quality()),
            root.getDouble("visuals.horizontal-radius", defaults.visuals().horizontalRadius()),
            root.getDouble("visuals.vertical-radius", defaults.visuals().verticalRadius()),
            root.getInt("visuals.interval-ticks", defaults.visuals().intervalTicks())
        );
        return new AcidRainFeedbackSettings(
            bossBar,
            actionBar,
            particles,
            loadSound(root, "sounds.start", defaults.startSound()),
            loadSound(root, "sounds.stop", defaults.stopSound()),
            impacts,
            visuals
        );
    }

    private static AcidRainAutomaticSettings loadAutomatic(
        ConfigurationSection root,
        AcidRainAutomaticSettings defaults,
        List<String> errors
    ) {
        boolean enabled = root.getBoolean("automatic.enabled", defaults.enabled());
        int chance = root.getInt("automatic.chance", defaults.chancePercent());
        int minimum = root.getInt("automatic.minimum-interval-minutes", defaults.minimumIntervalMinutes());
        int maximum = root.getInt("automatic.maximum-interval-minutes", defaults.maximumIntervalMinutes());
        if (chance < 0 || chance > 100 || minimum < 0 || maximum < minimum) {
            errors.add("acid-rain.automatic contains invalid chance or interval values");
            return AcidRainAutomaticSettings.disabled();
        }
        return new AcidRainAutomaticSettings(enabled, chance, minimum, maximum);
    }

    private static AcidRainMobSettings loadMobs(
        ConfigurationSection root,
        AcidRainMobSettings defaults,
        List<String> errors,
        List<String> warnings
    ) {
        boolean enabled = root.getBoolean("mobs.enabled", defaults.enabled());
        int interval = clampInt(root.getInt("mobs.spawn.interval-ticks", defaults.spawnIntervalTicks()),
            1, MAX_MOB_SPAWN_INTERVAL_TICKS, "mobs.spawn.interval-ticks", warnings);
        int attempts = clampInt(root.getInt("mobs.spawn.attempts-per-cycle", defaults.attemptsPerCycle()),
            0, MAX_MOB_ATTEMPTS_PER_CYCLE, "mobs.spawn.attempts-per-cycle", warnings);
        int maxActive = clampInt(root.getInt("mobs.spawn.max-active", defaults.maxActive()),
            0, MAX_MOB_MAX_ACTIVE, "mobs.spawn.max-active", warnings);
        int radius = clampInt(root.getInt("mobs.spawn.radius", defaults.radius()),
            0, MAX_MOB_RADIUS, "mobs.spawn.radius", warnings);
        if (interval < 1 || attempts < 0 || maxActive < 0 || radius < 0) {
            errors.add("acid-rain.mobs contains negative or invalid spawn values");
            return AcidRainMobSettings.disabled();
        }
        EnumMap<AcidRainMobType, Boolean> types = new EnumMap<>(AcidRainMobType.class);
        for (AcidRainMobType type : AcidRainMobType.values()) {
            types.put(type, root.getBoolean(
                "mobs.types." + type.name().toLowerCase(java.util.Locale.ROOT),
                defaults.isTypeEnabled(type)));
        }
        AcidRainMobSpawnEffectSettings spawnEffect = new AcidRainMobSpawnEffectSettings(
            root.getBoolean("mobs.spawn-effect.enabled", defaults.spawnEffect().enabled()),
            Math.max(0, Math.min(32, root.getInt("mobs.spawn-effect.particles", defaults.spawnEffect().particles()))),
            loadSound(root, "mobs.spawn-effect.sound", defaults.spawnEffect().sound())
        );
        AcidRainMobAmbientSettings ambient = new AcidRainMobAmbientSettings(
            root.getBoolean("mobs.ambient-particles.enabled", defaults.ambient().enabled()),
            Math.max(1, Math.min(200,
                root.getInt("mobs.ambient-particles.interval-ticks", defaults.ambient().intervalTicks()))),
            Math.max(0, Math.min(16, root.getInt("mobs.ambient-particles.count", defaults.ambient().count()))),
            root.getString("mobs.ambient-particles.particle", defaults.ambient().particle())
        );
        AcidRainMobDropSettings drops = new AcidRainMobDropSettings(
            root.getBoolean("mobs.drops.enabled", defaults.drops().enabled()),
            clampInt(root.getInt("mobs.drops.chance-percent", defaults.drops().chancePercent()),
                0, 100, "mobs.drops.chance-percent", warnings),
            Math.max(0, root.getInt("mobs.drops.min-amount", defaults.drops().minAmount())),
            Math.max(0, root.getInt("mobs.drops.max-amount", defaults.drops().maxAmount()))
        );
        return new AcidRainMobSettings(enabled, interval, attempts, maxActive, radius, types, spawnEffect, ambient, drops);
    }

    private static AcidRainSoundSettings loadSound(
        ConfigurationSection root,
        String path,
        AcidRainSoundSettings defaults
    ) {
        float volume = (float) root.getDouble(path + ".volume", defaults.volume());
        float pitch = (float) root.getDouble(path + ".pitch", defaults.pitch());
        if (!Float.isFinite(volume)) {
            volume = defaults.volume();
        } else {
            volume = Math.max(0.0f, Math.min(2.0f, volume));
        }
        if (!Float.isFinite(pitch)) {
            pitch = defaults.pitch();
        } else {
            pitch = Math.max(0.0f, Math.min(2.0f, pitch));
        }
        return new AcidRainSoundSettings(
            root.getBoolean(path + ".enabled", defaults.enabled()),
            root.getString(path + ".sound", defaults.sound()),
            volume,
            pitch
        );
    }

    private static Set<Material> parseMaterials(List<String> raw, List<String> warnings) {
        LinkedHashSet<Material> materials = new LinkedHashSet<>();
        for (String value : raw) {
            Material material = Material.matchMaterial(value == null ? "" : value.trim());
            if (material == null) {
                warnings.add("invalid material ignored: " + value);
                continue;
            }
            materials.add(material);
        }
        return Set.copyOf(materials);
    }

    private static int parseInt(Object raw, int fallback) {
        if (raw instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(raw));
        } catch (RuntimeException failure) {
            return fallback;
        }
    }

    private static ConfigurationSection resolveAcidRainSection(ConfigurationSection config) {
        if (config == null) {
            return null;
        }
        if (config.isConfigurationSection("acid-rain")) {
            return config.getConfigurationSection("acid-rain");
        }
        if ("acid-rain".equalsIgnoreCase(config.getName())
            || config.contains("default-level")
            || config.contains("levels")
            || config.contains("duration")) {
            return config;
        }
        return config.getConfigurationSection("acid-rain");
    }
}

record AcidRainConfigLoadResult(AcidRainSettings settings, List<String> errors, List<String> warnings) {
    AcidRainConfigLoadResult {
        errors = errors == null ? List.of() : List.copyOf(errors);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    boolean valid() {
        return errors.isEmpty();
    }
}
