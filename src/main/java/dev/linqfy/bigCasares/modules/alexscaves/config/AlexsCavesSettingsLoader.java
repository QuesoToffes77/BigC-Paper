package dev.linqfy.bigCasares.modules.alexscaves.config;

import dev.linqfy.bigCasares.modules.alexscaves.config.AlexsCavesSettings.BlockSettings;
import dev.linqfy.bigCasares.modules.alexscaves.config.AlexsCavesSettings.EffectSettings;
import dev.linqfy.bigCasares.modules.alexscaves.config.AlexsCavesSettings.EnchantmentSettings;
import dev.linqfy.bigCasares.modules.alexscaves.config.AlexsCavesSettings.IntegrationSettings;
import dev.linqfy.bigCasares.modules.alexscaves.config.AlexsCavesSettings.MagnetismSettings;
import dev.linqfy.bigCasares.modules.alexscaves.config.AlexsCavesSettings.SpawningSettings;
import dev.linqfy.bigCasares.modules.alexscaves.config.AlexsCavesSettings.WorldgenSettings;
import dev.linqfy.bigCasares.modules.alexscaves.core.worldgen.CaveBiomeId;
import dev.linqfy.bigCasares.modules.alexscaves.integration.BossIntegrationRules;
import dev.linqfy.bigCasares.modules.alexscaves.core.worldgen.VoronoiRarity.RarityParameters;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reads {@code alexs-caves.yml}. An invalid value never throws: it is reported as
 * an error and only the section that contains it is switched off, so a typo in
 * this file cannot roll back every BigCasares module.
 */
public final class AlexsCavesSettingsLoader {
    static final double MIN_RADIUS = 32;
    static final double MAX_RADIUS = 4096;
    static final double MAX_SEPARATION = 100_000;
    static final int MAX_SPAWN_DISTANCE = 1_000_000;

    private AlexsCavesSettingsLoader() {
    }

    public static AlexsCavesConfigLoadResult load(ConfigurationSection root) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        AlexsCavesSettings defaults = AlexsCavesSettings.defaults();
        if (root == null) {
            warnings.add("alexs-caves.yml vacío o ilegible: se usan los valores por defecto");
            return new AlexsCavesConfigLoadResult(defaults, errors, warnings);
        }
        WorldgenSettings worldgen = loadWorldgen(root.getConfigurationSection("worldgen"), defaults.worldgen(), errors, warnings);
        SpawningSettings spawning = loadSpawning(root.getConfigurationSection("spawning"), defaults.spawning(), errors);
        EffectSettings effects = loadEffects(root.getConfigurationSection("effects"), defaults.effects());
        MagnetismSettings magnetism = loadMagnetism(root.getConfigurationSection("magnetism"), defaults.magnetism(), errors);
        EnchantmentSettings enchantments = loadEnchantments(root.getConfigurationSection("enchantments"), defaults.enchantments());
        BlockSettings blocks = new BlockSettings(
            bool(root.getConfigurationSection("blocks"), "custom-textures", defaults.blocks().customTextures()),
            bool(root.getConfigurationSection("blocks"), "packet-remap", defaults.blocks().packetRemap()));
        IntegrationSettings integration = loadIntegration(root.getConfigurationSection("integration"), defaults.integration(), errors, warnings);
        return new AlexsCavesConfigLoadResult(
            new AlexsCavesSettings(worldgen, spawning, effects, magnetism, enchantments, blocks, integration), errors, warnings);
    }

    private static WorldgenSettings loadWorldgen(
        ConfigurationSection section,
        WorldgenSettings defaults,
        List<String> errors,
        List<String> warnings
    ) {
        if (section == null) {
            return defaults;
        }
        int errorsBefore = errors.size();
        boolean enabled = section.getBoolean("enabled", defaults.enabled());
        boolean installDatapack = section.getBoolean("install-datapack", defaults.installDatapack());
        List<String> worlds = section.isList("worlds") ? section.getStringList("worlds") : defaults.worlds();
        if (worlds.stream().anyMatch(name -> name == null || name.isBlank())) {
            errors.add("worldgen.worlds contiene un nombre de mundo vacío");
        }
        double radius = section.getDouble("biome-radius", defaults.rarity().radius());
        double separation = section.getDouble("biome-separation", defaults.rarity().separation());
        double width = section.getDouble("width-randomness", defaults.rarity().widthRandomness());
        double spacing = section.getDouble("spacing-randomness", defaults.rarity().spacingRandomness());
        if (!(radius >= MIN_RADIUS && radius <= MAX_RADIUS)) {
            errors.add("worldgen.biome-radius debe estar entre " + (int) MIN_RADIUS + " y " + (int) MAX_RADIUS + " (valor: " + radius + ")");
        }
        if (!(separation >= 0 && separation <= MAX_SEPARATION)) {
            errors.add("worldgen.biome-separation debe estar entre 0 y " + (int) MAX_SEPARATION + " (valor: " + separation + ")");
        }
        if (!(width >= 0 && width <= 1)) {
            errors.add("worldgen.width-randomness debe estar entre 0 y 1 (valor: " + width + ")");
        }
        if (!(spacing >= 0 && spacing <= 1)) {
            errors.add("worldgen.spacing-randomness debe estar entre 0 y 1 (valor: " + spacing + ")");
        }

        Set<CaveBiomeId> enabledBiomes = EnumSet.noneOf(CaveBiomeId.class);
        EnumMap<CaveBiomeId, Integer> distances = new EnumMap<>(defaults.minSpawnDistance());
        ConfigurationSection biomes = section.getConfigurationSection("biomes");
        for (CaveBiomeId biome : CaveBiomeId.values()) {
            ConfigurationSection biomeSection = biomes == null ? null : biomes.getConfigurationSection(biome.configKey());
            boolean biomeEnabled = biomeSection == null || biomeSection.getBoolean("enabled", true);
            if (biomeEnabled) {
                enabledBiomes.add(biome);
            }
            if (biomeSection != null && biomeSection.isSet("min-spawn-distance")) {
                int distance = biomeSection.getInt("min-spawn-distance");
                if (distance < 0 || distance > MAX_SPAWN_DISTANCE) {
                    errors.add("worldgen.biomes." + biome.configKey() + ".min-spawn-distance debe estar entre 0 y "
                        + MAX_SPAWN_DISTANCE + " (valor: " + distance + ")");
                } else {
                    distances.put(biome, distance);
                }
            }
        }
        if (biomes != null) {
            for (String key : biomes.getKeys(false)) {
                if (CaveBiomeId.parse(key).isEmpty()) {
                    warnings.add("worldgen.biomes." + key + " no es un bioma conocido y se ignora");
                }
            }
        }
        int blocksPerTick = section.getInt("structure-blocks-per-tick", defaults.structureBlocksPerTick());
        if (blocksPerTick < 1 || blocksPerTick > 50_000) {
            errors.add("worldgen.structure-blocks-per-tick debe estar entre 1 y 50000 (valor: " + blocksPerTick + ")");
        }
        if (errors.size() > errorsBefore) {
            return defaults.disabled();
        }
        return new WorldgenSettings(enabled, installDatapack, worlds,
            new RarityParameters(radius, separation, width, spacing), enabledBiomes, distances, blocksPerTick);
    }

    private static SpawningSettings loadSpawning(ConfigurationSection section, SpawningSettings defaults, List<String> errors) {
        if (section == null) {
            return defaults;
        }
        int errorsBefore = errors.size();
        int interval = rangedInt(section, "interval-ticks", 1, 6000, defaults.intervalTicks(), errors, "spawning");
        int radius = rangedInt(section, "radius", 16, 128, defaults.radius(), errors, "spawning");
        int attempts = rangedInt(section, "attempts-per-player", 0, 64, defaults.attemptsPerPlayer(), errors, "spawning");
        double multiplier = rangedDouble(section, "cave-creature-multiplier", 0, 10, defaults.caveCreatureMultiplier(), errors, "spawning");
        double capScale = rangedDouble(section, "cap-scale", 0, 4, defaults.capScale(), errors, "spawning");
        if (errors.size() > errorsBefore) {
            return defaults.disabled();
        }
        return new SpawningSettings(section.getBoolean("enabled", defaults.enabled()), interval, radius, attempts, multiplier, capScale);
    }

    private static EffectSettings loadEffects(ConfigurationSection section, EffectSettings defaults) {
        if (section == null) {
            return defaults;
        }
        return new EffectSettings(
            section.getBoolean("irradiated", defaults.irradiatedEnabled()),
            section.getBoolean("sugar-rush-slows-nearby", defaults.sugarRushSlowsNearby()),
            section.getBoolean("hud", defaults.hud())
        );
    }

    private static MagnetismSettings loadMagnetism(ConfigurationSection section, MagnetismSettings defaults, List<String> errors) {
        if (section == null) {
            return defaults;
        }
        int errorsBefore = errors.size();
        int maxEntities = rangedInt(section, "max-entities-per-tick", 1, 5000, defaults.maxEntitiesPerTick(), errors, "magnetism");
        int scanRadius = rangedInt(section, "scan-radius", 1, 16, defaults.scanRadius(), errors, "magnetism");
        if (errors.size() > errorsBefore) {
            return defaults.disabled();
        }
        return new MagnetismSettings(section.getBoolean("enabled", defaults.enabled()), maxEntities, scanRadius);
    }

    private static EnchantmentSettings loadEnchantments(ConfigurationSection section, EnchantmentSettings defaults) {
        if (section == null) {
            return defaults;
        }
        return new EnchantmentSettings(section.getBoolean("enabled", defaults.enabled()),
            section.getBoolean("in-loot", defaults.inLoot()));
    }

    /** Bad economy amounts switch only the economy reward off; Danger placements are untouched. */
    private static IntegrationSettings loadIntegration(ConfigurationSection section, IntegrationSettings defaults,
                                                       List<String> errors, List<String> warnings) {
        if (section == null) {
            return defaults;
        }
        ConfigurationSection dangerSection = section.getConfigurationSection("danger");
        if (dangerSection != null && dangerSection.isSet("placements") && !dangerSection.isBoolean("placements")) {
            warnings.add("integration.danger.placements debe ser true o false: se usa " + defaults.dangerPlacements());
        }
        boolean danger = bool(dangerSection, "placements", defaults.dangerPlacements());
        double minDamage = defaults.minDamagePercent();
        if (section.isSet("min-damage-percent")) {
            double value = section.getDouble("min-damage-percent", Double.NaN);
            if (value >= 0 && value <= 100) {
                minDamage = value;
            } else {
                errors.add("integration.min-damage-percent debe estar entre 0 y 100 (valor: " + section.get("min-damage-percent") + ")");
            }
        }
        ConfigurationSection economy = section.getConfigurationSection("economy");
        if (economy == null) {
            return new IntegrationSettings(danger, defaults.economyEnabled(), minDamage, defaults.economyRewards());
        }
        int errorsBefore = errors.size();
        boolean economyEnabled = defaults.economyEnabled();
        if (economy.isSet("enabled")) {
            if (economy.isBoolean("enabled")) {
                economyEnabled = economy.getBoolean("enabled");
            } else {
                warnings.add("integration.economy.enabled debe ser true o false: la economía queda apagada");
            }
        }
        Map<String, List<Double>> rewards = new LinkedHashMap<>(defaults.economyRewards());
        ConfigurationSection bosses = economy.getConfigurationSection("rewards");
        if (bosses != null) {
            for (String boss : bosses.getKeys(false)) {
                String bossId = boss.toLowerCase(Locale.ROOT);
                if (!BossIntegrationRules.BOSS_IDS.contains(bossId)) {
                    warnings.add("integration.economy.rewards." + boss + " no es un jefe conocido ("
                        + String.join(", ", new java.util.TreeSet<>(BossIntegrationRules.BOSS_IDS)) + ") y se ignora");
                    continue;
                }
                List<?> raw = bosses.getList(boss);
                if (raw == null || raw.size() > 3) {
                    errors.add("integration.economy.rewards." + boss + " debe ser una lista de hasta 3 montos");
                    continue;
                }
                List<Double> amounts = new ArrayList<>();
                for (Object value : raw) {
                    double amount = value instanceof Number number ? number.doubleValue() : Double.NaN;
                    if (!(amount >= 0 && amount <= IntegrationSettings.MAX_REWARD)) {
                        errors.add("integration.economy.rewards." + boss + " tiene un monto inválido (" + value
                            + "): debe estar entre 0 y " + (long) IntegrationSettings.MAX_REWARD);
                        break;
                    }
                    amounts.add(amount);
                }
                rewards.put(bossId, amounts);
            }
        }
        IntegrationSettings result = new IntegrationSettings(danger, economyEnabled, minDamage, rewards);
        return errors.size() > errorsBefore ? result.withoutEconomy() : result;
    }

    private static boolean bool(ConfigurationSection section, String path, boolean fallback) {
        return section == null ? fallback : section.getBoolean(path, fallback);
    }

    private static int rangedInt(ConfigurationSection section, String path, int min, int max, int fallback,
                                 List<String> errors, String prefix) {
        int value = section.getInt(path, fallback);
        if (value < min || value > max) {
            errors.add(prefix + "." + path + " debe estar entre " + min + " y " + max + " (valor: " + value + ")");
            return fallback;
        }
        return value;
    }

    private static double rangedDouble(ConfigurationSection section, String path, double min, double max, double fallback,
                                       List<String> errors, String prefix) {
        double value = section.getDouble(path, fallback);
        if (!(value >= min && value <= max)) {
            errors.add(prefix + "." + path + " debe estar entre " + min + " y " + max + " (valor: " + value + ")");
            return fallback;
        }
        return value;
    }

    /** Spawn distances are part of the public map so tests and commands can show them. */
    public static Map<CaveBiomeId, Integer> defaultSpawnDistances() {
        return WorldgenSettings.defaults().minSpawnDistance();
    }
}
