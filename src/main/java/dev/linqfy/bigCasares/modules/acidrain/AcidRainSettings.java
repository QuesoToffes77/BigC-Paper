package dev.linqfy.bigCasares.modules.acidrain;

import dev.linqfy.bigCasares.modules.environment.EnvironmentalVisualSettings;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record AcidRainSettings(
    AcidRainLevel defaultLevel,
    Map<AcidRainLevel, AcidRainLevelSettings> levels,
    AcidRainDurationSettings duration,
    AcidRainWarningSettings warning,
    AcidRainEscalationSettings escalation,
    AcidRainDamageSettings damage,
    AcidRainWorldSettings worlds,
    AcidRainEnvironmentSettings environment,
    AcidRainProtectionSettings protection,
    AcidRainFeedbackSettings feedback,
    AcidRainAutomaticSettings automatic,
    AcidRainWaterSettings water,
    AcidRainMobSettings mobs
) {
    public AcidRainSettings {
        defaultLevel = defaultLevel == null ? AcidRainLevel.ACID : defaultLevel;
        EnumMap<AcidRainLevel, AcidRainLevelSettings> copiedLevels = new EnumMap<>(AcidRainLevel.class);
        copiedLevels.putAll(levels == null ? defaultLevelSettings() : levels);
        for (Map.Entry<AcidRainLevel, AcidRainLevelSettings> entry : defaultLevelSettings().entrySet()) {
            copiedLevels.putIfAbsent(entry.getKey(), entry.getValue());
        }
        levels = Map.copyOf(copiedLevels);
        duration = duration == null ? new AcidRainDurationSettings(300, 900, 20) : duration;
        warning = warning == null ? AcidRainWarningSettings.defaults() : warning;
        escalation = escalation == null ? AcidRainEscalationSettings.defaults() : escalation;
        damage = damage == null ? new AcidRainDamageSettings(true) : damage;
        worlds = worlds == null ? AcidRainWorldSettings.defaults() : worlds;
        environment = environment == null ? AcidRainEnvironmentSettings.defaults() : environment;
        protection = protection == null ? AcidRainProtectionSettings.defaults() : protection;
        feedback = feedback == null ? AcidRainFeedbackSettings.defaults() : feedback;
        automatic = automatic == null ? AcidRainAutomaticSettings.defaults() : automatic;
        water = water == null ? new AcidRainWaterSettings(false) : water;
        mobs = mobs == null ? AcidRainMobSettings.defaults() : mobs;
    }

    public static AcidRainSettings safeDefaults() {
        return new AcidRainSettings(
            AcidRainLevel.ACID,
            defaultLevelSettings(),
            new AcidRainDurationSettings(300, 900, 20),
            AcidRainWarningSettings.defaults(),
            AcidRainEscalationSettings.defaults(),
            new AcidRainDamageSettings(true),
            AcidRainWorldSettings.defaults(),
            AcidRainEnvironmentSettings.defaults(),
            AcidRainProtectionSettings.defaults(),
            AcidRainFeedbackSettings.defaults(),
            AcidRainAutomaticSettings.defaults(),
            new AcidRainWaterSettings(false),
            AcidRainMobSettings.defaults()
        );
    }

    public static AcidRainSettings safeDisabled() {
        AcidRainSettings defaults = safeDefaults();
        return defaults
            .withDamage(new AcidRainDamageSettings(false))
            .withEnvironment(defaults.environment().withDestruction(defaults.environment().destruction().withEnabled(false)))
            .withAutomatic(AcidRainAutomaticSettings.disabled())
            .withMobs(AcidRainMobSettings.disabled());
    }

    public AcidRainLevelSettings settingsFor(AcidRainLevel level) {
        return levels.getOrDefault(level, levels.get(AcidRainLevel.ACID));
    }

    public AcidRainSettings withDuration(AcidRainDurationSettings duration) {
        return new AcidRainSettings(defaultLevel, levels, duration, warning, escalation, damage, worlds, environment,
            protection, feedback, automatic, water, mobs);
    }

    public AcidRainSettings withWarning(AcidRainWarningSettings warning) {
        return new AcidRainSettings(defaultLevel, levels, duration, warning, escalation, damage, worlds, environment,
            protection, feedback, automatic, water, mobs);
    }

    public AcidRainSettings withLevels(Map<AcidRainLevel, AcidRainLevelSettings> levels) {
        return new AcidRainSettings(defaultLevel, levels, duration, warning, escalation, damage, worlds, environment,
            protection, feedback, automatic, water, mobs);
    }

    public AcidRainSettings withEscalation(AcidRainEscalationSettings escalation) {
        return new AcidRainSettings(defaultLevel, levels, duration, warning, escalation, damage, worlds, environment,
            protection, feedback, automatic, water, mobs);
    }

    public AcidRainSettings withDamage(AcidRainDamageSettings damage) {
        return new AcidRainSettings(defaultLevel, levels, duration, warning, escalation, damage, worlds, environment,
            protection, feedback, automatic, water, mobs);
    }

    public AcidRainSettings withWorlds(AcidRainWorldSettings worlds) {
        return new AcidRainSettings(defaultLevel, levels, duration, warning, escalation, damage, worlds, environment,
            protection, feedback, automatic, water, mobs);
    }

    public AcidRainSettings withEnvironment(AcidRainEnvironmentSettings environment) {
        return new AcidRainSettings(defaultLevel, levels, duration, warning, escalation, damage, worlds, environment,
            protection, feedback, automatic, water, mobs);
    }

    public AcidRainSettings withProtection(AcidRainProtectionSettings protection) {
        return new AcidRainSettings(defaultLevel, levels, duration, warning, escalation, damage, worlds, environment,
            protection, feedback, automatic, water, mobs);
    }

    public AcidRainSettings withFeedback(AcidRainFeedbackSettings feedback) {
        return new AcidRainSettings(defaultLevel, levels, duration, warning, escalation, damage, worlds, environment,
            protection, feedback, automatic, water, mobs);
    }

    public AcidRainSettings withAutomatic(AcidRainAutomaticSettings automatic) {
        return new AcidRainSettings(defaultLevel, levels, duration, warning, escalation, damage, worlds, environment,
            protection, feedback, automatic, water, mobs);
    }

    public AcidRainSettings withWater(AcidRainWaterSettings water) {
        return new AcidRainSettings(defaultLevel, levels, duration, warning, escalation, damage, worlds, environment,
            protection, feedback, automatic, water, mobs);
    }

    public AcidRainSettings withMobs(AcidRainMobSettings mobs) {
        return new AcidRainSettings(defaultLevel, levels, duration, warning, escalation, damage, worlds, environment,
            protection, feedback, automatic, water, mobs);
    }

    public AcidRainSettings withDefaultLevel(AcidRainLevel level) {
        return new AcidRainSettings(level, levels, duration, warning, escalation, damage, worlds, environment,
            protection, feedback, automatic, water, mobs);
    }

    private static Map<AcidRainLevel, AcidRainLevelSettings> defaultLevelSettings() {
        EnumMap<AcidRainLevel, AcidRainLevelSettings> values = new EnumMap<>(AcidRainLevel.class);
        values.put(AcidRainLevel.ACID, new AcidRainLevelSettings(1.0, 5, 1.0, List.of(),
            new AcidRainLevelDestructionSettings(40, 30)));
        values.put(AcidRainLevel.TOXIC, new AcidRainLevelSettings(2.0, 3, 2.0,
            List.of("POISON"), new AcidRainLevelDestructionSettings(20, 60)));
        values.put(AcidRainLevel.CHEMICAL, new AcidRainLevelSettings(3.0, 2, 3.0,
            List.of("POISON", "WEAKNESS"), new AcidRainLevelDestructionSettings(10, 90)));
        return values;
    }

    static Set<Material> defaultWhitelist() {
        return Set.of(
            Material.STONE,
            Material.COBBLESTONE,
            Material.DEEPSLATE,
            Material.OAK_LOG,
            Material.SPRUCE_LOG,
            Material.BIRCH_LOG,
            Material.JUNGLE_LOG,
            Material.ACACIA_LOG,
            Material.DARK_OAK_LOG,
            Material.MANGROVE_LOG,
            Material.CHERRY_LOG,
            Material.OAK_PLANKS,
            Material.SPRUCE_PLANKS,
            Material.BIRCH_PLANKS,
            Material.JUNGLE_PLANKS,
            Material.ACACIA_PLANKS,
            Material.DARK_OAK_PLANKS,
            Material.MANGROVE_PLANKS,
            Material.CHERRY_PLANKS
        );
    }

    static Set<Material> linkedSet(Material... materials) {
        LinkedHashSet<Material> set = new LinkedHashSet<>();
        java.util.Collections.addAll(set, materials);
        return Set.copyOf(set);
    }
}

record AcidRainLevelSettings(
    double damage,
    int intervalSeconds,
    double contaminationIntensity,
    List<String> effects,
    AcidRainLevelDestructionSettings destruction
) {
    AcidRainLevelSettings(double damage, int intervalSeconds, double contaminationIntensity, List<String> effects) {
        this(damage, intervalSeconds, contaminationIntensity, effects, null);
    }

    AcidRainLevelSettings {
        effects = effects == null ? List.of() : List.copyOf(effects);
        destruction = destruction == null
            ? new AcidRainLevelDestructionSettings(20, 45)
            : destruction;
        // Defensive numeric sanitation: a NaN/Infinity/negative value must never
        // reach damage or contamination math.
        damage = finiteOrZero(damage);
        contaminationIntensity = finiteOrZero(contaminationIntensity);
        intervalSeconds = Math.max(1, intervalSeconds);
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }
}

/**
 * Per-level environmental destruction tuning. The global
 * {@code environment.destruction.*} values remain the hard safety caps;
 * these values control how aggressively each level harvests and breaks blocks
 * within those caps.
 */
record AcidRainLevelDestructionSettings(int intervalTicks, int candidatesPerCycle) {
    AcidRainLevelDestructionSettings {
        intervalTicks = Math.max(1, intervalTicks);
        candidatesPerCycle = Math.max(0, candidatesPerCycle);
    }
}

record AcidRainDurationSettings(int minimumSeconds, int maximumSeconds, int endingSeconds) {
}

record AcidRainWarningSettings(
    int durationSeconds,
    int messageIntervalSeconds,
    List<String> messages,
    AcidRainSoundSettings sound,
    boolean bossBar
) {
    AcidRainWarningSettings {
        messages = messages == null ? List.of() : List.copyOf(messages);
        sound = sound == null ? AcidRainSoundSettings.disabled() : sound;
    }

    static AcidRainWarningSettings defaults() {
        return new AcidRainWarningSettings(
            60,
            20,
            List.of(
                "&e&lALERTA AMBIENTAL",
                "&7Se detecto contaminacion atmosferica.",
                "&cLa lluvia acida comenzara pronto."
            ),
            new AcidRainSoundSettings(true, "BLOCK_NOTE_BLOCK_BELL", 1.0f, 1.0f),
            true
        );
    }
}

record AcidRainEscalationSettings(boolean enabled, List<AcidRainEscalationStep> steps) {
    AcidRainEscalationSettings {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }

    static AcidRainEscalationSettings defaults() {
        return new AcidRainEscalationSettings(true, List.of(
            new AcidRainEscalationStep(AcidRainLevel.TOXIC, 180),
            new AcidRainEscalationStep(AcidRainLevel.CHEMICAL, 420)
        ));
    }
}

record AcidRainEscalationStep(AcidRainLevel level, int afterSeconds) {
}

record AcidRainDamageSettings(boolean enabled) {
}

record AcidRainWorldSettings(Set<String> enabled, Set<String> excluded) {
    AcidRainWorldSettings {
        enabled = normalize(enabled == null ? Set.of() : enabled);
        excluded = normalize(excluded == null ? Set.of() : excluded);
    }

    static AcidRainWorldSettings defaults() {
        return new AcidRainWorldSettings(Set.of("world"), Set.of());
    }

    boolean isAffected(String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return false;
        }
        String normalized = worldName.trim().toLowerCase(java.util.Locale.ROOT);
        return !enabled.isEmpty() && enabled.contains(normalized) && !excluded.contains(normalized);
    }

    private static Set<String> normalize(Set<String> values) {
        return values.stream()
            .filter(value -> value != null && !value.isBlank())
            .map(value -> value.trim().toLowerCase(java.util.Locale.ROOT))
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}

record AcidRainEnvironmentSettings(AcidRainDestructionSettings destruction, boolean waterEnabled) {
    AcidRainEnvironmentSettings {
        destruction = destruction == null ? AcidRainDestructionSettings.defaults() : destruction;
    }

    static AcidRainEnvironmentSettings defaults() {
        return new AcidRainEnvironmentSettings(AcidRainDestructionSettings.defaults(), false);
    }

    AcidRainEnvironmentSettings withDestruction(AcidRainDestructionSettings destruction) {
        return new AcidRainEnvironmentSettings(destruction, waterEnabled);
    }
}

record AcidRainDestructionSettings(
    boolean enabled,
    int radius,
    int maxBlocksPerEvent,
    int maxBlocksPerSecond,
    int candidatesPerCycle,
    int verticalScanDepth,
    Set<Material> whitelist,
    boolean cropsEnabled
) {
    AcidRainDestructionSettings {
        whitelist = whitelist == null ? Set.of() : Set.copyOf(whitelist);
    }

    static AcidRainDestructionSettings defaults() {
        return new AcidRainDestructionSettings(true, 18, 3000, 90, 90, 16, AcidRainSettings.defaultWhitelist(), false);
    }

    boolean canDestroyBlocks() {
        return enabled && maxBlocksPerEvent > 0 && maxBlocksPerSecond > 0 && !whitelist.isEmpty();
    }

    AcidRainDestructionSettings withEnabled(boolean enabled) {
        return new AcidRainDestructionSettings(enabled, radius, maxBlocksPerEvent, maxBlocksPerSecond,
            candidatesPerCycle, verticalScanDepth, whitelist, cropsEnabled);
    }

    AcidRainDestructionSettings withWhitelist(Set<Material> whitelist) {
        return new AcidRainDestructionSettings(enabled, radius, maxBlocksPerEvent, maxBlocksPerSecond,
            candidatesPerCycle, verticalScanDepth, whitelist, cropsEnabled);
    }

    AcidRainDestructionSettings withMaxBlocksPerEvent(int maxBlocksPerEvent) {
        return new AcidRainDestructionSettings(enabled, radius, maxBlocksPerEvent, maxBlocksPerSecond,
            candidatesPerCycle, verticalScanDepth, whitelist, cropsEnabled);
    }

    AcidRainDestructionSettings withMaxBlocksPerSecond(int maxBlocksPerSecond) {
        return new AcidRainDestructionSettings(enabled, radius, maxBlocksPerEvent, maxBlocksPerSecond,
            candidatesPerCycle, verticalScanDepth, whitelist, cropsEnabled);
    }
}

record AcidRainProtectionSettings(List<AcidRainProtectionGroup> groups) {
    AcidRainProtectionSettings {
        groups = groups == null ? List.of() : List.copyOf(groups);
    }

    static AcidRainProtectionSettings defaults() {
        return new AcidRainProtectionSettings(List.of(
            new AcidRainProtectionGroup("basic", 25.0, List.of(
                Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS
            ), Set.of()),
            new AcidRainProtectionGroup("advanced", 60.0, List.of(
                Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS
            ), Set.of()),
            new AcidRainProtectionGroup("chemical", 100.0, List.of(
                Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS
            ), Set.of())
        ));
    }
}

record AcidRainProtectionGroup(
    String id,
    double protectionPercent,
    List<Material> requiredArmor,
    Set<Material> carriedMaterials
) {
    AcidRainProtectionGroup {
        id = id == null || id.isBlank() ? "unnamed" : id.trim();
        requiredArmor = requiredArmor == null ? List.of() : List.copyOf(requiredArmor);
        carriedMaterials = carriedMaterials == null ? Set.of() : Set.copyOf(carriedMaterials);
    }
}

record AcidRainFeedbackSettings(
    AcidRainBossBarSettings bossBar,
    AcidRainActionBarSettings actionBar,
    AcidRainParticleSettings particles,
    AcidRainSoundSettings startSound,
    AcidRainSoundSettings stopSound,
    AcidRainImpactSettings impacts,
    EnvironmentalVisualSettings visuals
) {
    AcidRainFeedbackSettings {
        bossBar = bossBar == null ? AcidRainBossBarSettings.defaults() : bossBar;
        actionBar = actionBar == null ? AcidRainActionBarSettings.defaults() : actionBar;
        particles = particles == null ? AcidRainParticleSettings.defaults() : particles;
        startSound = startSound == null ? new AcidRainSoundSettings(true, "ENTITY_LIGHTNING_BOLT_THUNDER", 1.0f, 1.0f) : startSound;
        stopSound = stopSound == null ? new AcidRainSoundSettings(true, "BLOCK_BEACON_DEACTIVATE", 0.7f, 1.2f) : stopSound;
        impacts = impacts == null ? AcidRainImpactSettings.defaults() : impacts;
        visuals = visuals == null ? EnvironmentalVisualSettings.defaults() : visuals;
    }

    AcidRainFeedbackSettings(
        AcidRainBossBarSettings bossBar,
        AcidRainActionBarSettings actionBar,
        AcidRainParticleSettings particles,
        AcidRainSoundSettings startSound,
        AcidRainSoundSettings stopSound
    ) {
        this(bossBar, actionBar, particles, startSound, stopSound,
            AcidRainImpactSettings.defaults(), EnvironmentalVisualSettings.defaults());
    }

    AcidRainFeedbackSettings(
        AcidRainBossBarSettings bossBar,
        AcidRainActionBarSettings actionBar,
        AcidRainParticleSettings particles,
        AcidRainSoundSettings startSound,
        AcidRainSoundSettings stopSound,
        AcidRainImpactSettings impacts
    ) {
        this(bossBar, actionBar, particles, startSound, stopSound, impacts, EnvironmentalVisualSettings.defaults());
    }

    static AcidRainFeedbackSettings defaults() {
        return new AcidRainFeedbackSettings(
            AcidRainBossBarSettings.defaults(),
            AcidRainActionBarSettings.defaults(),
            AcidRainParticleSettings.defaults(),
            new AcidRainSoundSettings(true, "ENTITY_LIGHTNING_BOLT_THUNDER", 1.0f, 1.0f),
            new AcidRainSoundSettings(true, "BLOCK_BEACON_DEACTIVATE", 0.7f, 1.2f),
            AcidRainImpactSettings.defaults(),
            EnvironmentalVisualSettings.defaults()
        );
    }
}

record AcidRainBossBarSettings(
    boolean enabled,
    String color,
    String style,
    String warningColor,
    String activeColor,
    String endingColor
) {
    AcidRainBossBarSettings {
        color = blankTo(color, "GREEN");
        style = blankTo(style, "SEGMENTED_10");
        warningColor = blankTo(warningColor, "YELLOW");
        activeColor = blankTo(activeColor, "RED");
        endingColor = blankTo(endingColor, "GREEN");
    }

    AcidRainBossBarSettings(boolean enabled, String color, String style) {
        this(enabled, color, style, "YELLOW", "RED", "GREEN");
    }

    static AcidRainBossBarSettings defaults() {
        return new AcidRainBossBarSettings(true, "GREEN", "SEGMENTED_10", "YELLOW", "RED", "GREEN");
    }

    String colorFor(AcidRainState state) {
        if (state == null) {
            return color;
        }
        return switch (state) {
            case WARNING -> warningColor;
            case ACTIVE -> activeColor;
            case ENDING -> endingColor;
            case INACTIVE -> color;
        };
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}

record AcidRainActionBarSettings(boolean enabled, int intervalTicks) {
    static AcidRainActionBarSettings defaults() {
        return new AcidRainActionBarSettings(true, 20);
    }
}

record AcidRainParticleSettings(
    boolean enabled,
    String particle,
    int count,
    int intervalTicks,
    Map<AcidRainLevel, AcidRainLevelParticleSettings> levels
) {
    AcidRainParticleSettings {
        particle = particle == null || particle.isBlank() ? "FALLING_WATER" : particle;
        count = Math.max(0, count);
        intervalTicks = Math.max(1, intervalTicks);
        levels = levels == null ? Map.of() : Map.copyOf(levels);
    }

    AcidRainParticleSettings(boolean enabled, String particle, int count, int intervalTicks) {
        this(enabled, particle, count, intervalTicks, Map.of());
    }

    static AcidRainParticleSettings defaults() {
        return new AcidRainParticleSettings(true, "FALLING_WATER", 4, 20, defaultLevels());
    }

    /**
     * Per-level visual tuning. Each level can override the particle type and
     * count; a null/blank particle or a negative count falls back to the base
     * values, so every level is always fully resolved.
     */
    AcidRainLevelParticleSettings forLevel(AcidRainLevel level) {
        AcidRainLevelParticleSettings override = levels.get(level);
        if (override == null) {
            return new AcidRainLevelParticleSettings(particle, count);
        }
        String resolvedParticle = override.particle() == null || override.particle().isBlank()
            ? particle
            : override.particle();
        int resolvedCount = override.count() < 0 ? count : override.count();
        return new AcidRainLevelParticleSettings(resolvedParticle, resolvedCount);
    }

    private static Map<AcidRainLevel, AcidRainLevelParticleSettings> defaultLevels() {
        EnumMap<AcidRainLevel, AcidRainLevelParticleSettings> values = new EnumMap<>(AcidRainLevel.class);
        values.put(AcidRainLevel.ACID, new AcidRainLevelParticleSettings("FALLING_WATER", 4));
        values.put(AcidRainLevel.TOXIC, new AcidRainLevelParticleSettings("SPORE_BLOSSOM_AIR", 10));
        values.put(AcidRainLevel.CHEMICAL, new AcidRainLevelParticleSettings("DRAGON_BREATH", 16));
        return Map.copyOf(values);
    }
}

record AcidRainLevelParticleSettings(String particle, int count) {
    AcidRainLevelParticleSettings {
        count = Math.max(-1, count);
    }
}

/**
 * Small splash bursts where rain drops hit the ground. Throttled by the
 * {@code feedback.particles.interval-ticks} cycle; never per-tick.
 */
record AcidRainImpactSettings(boolean enabled, int count) {
    AcidRainImpactSettings {
        count = Math.max(0, Math.min(16, count));
    }

    static AcidRainImpactSettings defaults() {
        return new AcidRainImpactSettings(true, 3);
    }
}

record AcidRainSoundSettings(boolean enabled, String sound, float volume, float pitch) {
    static AcidRainSoundSettings disabled() {
        return new AcidRainSoundSettings(false, "", 0.0f, 1.0f);
    }
}

record AcidRainAutomaticSettings(
    boolean enabled,
    int chancePercent,
    int minimumIntervalMinutes,
    int maximumIntervalMinutes
) {
    static AcidRainAutomaticSettings defaults() {
        return new AcidRainAutomaticSettings(true, 10, 60, 180);
    }

    static AcidRainAutomaticSettings disabled() {
        return new AcidRainAutomaticSettings(false, 0, 60, 180);
    }
}

record AcidRainWaterSettings(boolean enabled) {
}

/**
 * Acid Rain mob spawning tuning. The spawn gate is exactly
 * {@code ACTIVE} state; every knob below is bounded by the loader, and the
 * record normalizes values defensively so no calculation can propagate
 * negatives or non-finite numbers.
 */
record AcidRainMobSettings(
    boolean enabled,
    int spawnIntervalTicks,
    int attemptsPerCycle,
    int maxActive,
    int radius,
    Map<AcidRainMobType, Boolean> types,
    AcidRainMobSpawnEffectSettings spawnEffect,
    AcidRainMobAmbientSettings ambient,
    AcidRainMobDropSettings drops
) {
    AcidRainMobSettings {
        spawnIntervalTicks = Math.max(1, spawnIntervalTicks);
        attemptsPerCycle = Math.max(0, attemptsPerCycle);
        maxActive = Math.max(0, maxActive);
        radius = Math.max(0, radius);
        types = types == null || types.isEmpty() ? allEnabled() : Map.copyOf(types);
        spawnEffect = spawnEffect == null ? AcidRainMobSpawnEffectSettings.defaults() : spawnEffect;
        ambient = ambient == null ? AcidRainMobAmbientSettings.defaults() : ambient;
        drops = drops == null ? AcidRainMobDropSettings.defaults() : drops;
    }

    AcidRainMobSettings(
        boolean enabled,
        int spawnIntervalTicks,
        int attemptsPerCycle,
        int maxActive,
        int radius,
        Map<AcidRainMobType, Boolean> types
    ) {
        this(enabled, spawnIntervalTicks, attemptsPerCycle, maxActive, radius, types,
            AcidRainMobSpawnEffectSettings.defaults(), AcidRainMobAmbientSettings.defaults(),
            AcidRainMobDropSettings.defaults());
    }

    static AcidRainMobSettings defaults() {
        return new AcidRainMobSettings(true, 60, 2, 20, 24, allEnabled(),
            AcidRainMobSpawnEffectSettings.defaults(), AcidRainMobAmbientSettings.defaults(),
            AcidRainMobDropSettings.defaults());
    }

    static AcidRainMobSettings disabled() {
        return new AcidRainMobSettings(false, 60, 2, 20, 24, allEnabled(),
            AcidRainMobSpawnEffectSettings.defaults(), AcidRainMobAmbientSettings.defaults(),
            AcidRainMobDropSettings.disabled());
    }

    boolean canSpawn() {
        return enabled && spawnIntervalTicks > 0 && attemptsPerCycle > 0
            && maxActive > 0 && radius > 0 && !enabledTypes().isEmpty();
    }

    List<AcidRainMobType> enabledTypes() {
        return java.util.Arrays.stream(AcidRainMobType.values())
            .filter(type -> types.getOrDefault(type, true))
            .toList();
    }

    boolean isTypeEnabled(AcidRainMobType type) {
        return types.getOrDefault(type, true);
    }

    AcidRainMobSettings withEnabled(boolean enabled) {
        return new AcidRainMobSettings(enabled, spawnIntervalTicks, attemptsPerCycle, maxActive, radius, types,
            spawnEffect, ambient, drops);
    }

    AcidRainMobSettings withSpawnIntervalTicks(int spawnIntervalTicks) {
        return new AcidRainMobSettings(enabled, spawnIntervalTicks, attemptsPerCycle, maxActive, radius, types,
            spawnEffect, ambient, drops);
    }

    private static Map<AcidRainMobType, Boolean> allEnabled() {
        EnumMap<AcidRainMobType, Boolean> values = new EnumMap<>(AcidRainMobType.class);
        for (AcidRainMobType type : AcidRainMobType.values()) {
            values.put(type, true);
        }
        return Map.copyOf(values);
    }
}

/**
 * Acid Rain mob loot: how often they drop the catalog {@code nitric_acid}
 * item and in what quantity. Chance is a percentage; amount is rolled
 * uniformly between {@code minAmount} and {@code maxAmount}. The record
 * normalizes defensively so no calculation can propagate out-of-range values.
 */
record AcidRainMobDropSettings(boolean enabled, int chancePercent, int minAmount, int maxAmount) {
    AcidRainMobDropSettings {
        chancePercent = Math.max(0, Math.min(100, chancePercent));
        minAmount = Math.max(0, minAmount);
        maxAmount = Math.max(minAmount, maxAmount);
    }

    static AcidRainMobDropSettings defaults() {
        return new AcidRainMobDropSettings(true, 100, 1, 2);
    }

    static AcidRainMobDropSettings disabled() {
        return new AcidRainMobDropSettings(false, 0, 0, 0);
    }
}

/**
 * Audio/visual burst emitted when an Acid Rain mob spawns (configurable;
 * vanilla mobs keep their own attack/death sounds).
 */
record AcidRainMobSpawnEffectSettings(boolean enabled, int particles, AcidRainSoundSettings sound) {
    AcidRainMobSpawnEffectSettings {
        particles = Math.max(0, Math.min(32, particles));
        sound = sound == null ? AcidRainSoundSettings.disabled() : sound;
    }

    static AcidRainMobSpawnEffectSettings defaults() {
        return new AcidRainMobSpawnEffectSettings(
            true, 8, new AcidRainSoundSettings(true, "ENTITY_WITCH_AMBIENT", 0.7f, 0.9f));
    }
}

/**
 * Periodic spore-like particles around living Acid Rain mobs, bounded by
 * {@code max-active} and throttled to avoid per-tick spawning.
 */
record AcidRainMobAmbientSettings(boolean enabled, int intervalTicks, int count, String particle) {
    AcidRainMobAmbientSettings {
        intervalTicks = Math.max(1, Math.min(200, intervalTicks));
        count = Math.max(0, Math.min(16, count));
        particle = particle == null || particle.isBlank() ? "SPORE_BLOSSOM_AIR" : particle;
    }

    static AcidRainMobAmbientSettings defaults() {
        return new AcidRainMobAmbientSettings(true, 20, 2, "SPORE_BLOSSOM_AIR");
    }
}
