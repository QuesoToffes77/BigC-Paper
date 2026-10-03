package dev.linqfy.bigCasares.modules.airdrop;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.Map;

public record AirdropQualitySettings(
    Map<AirdropQuality, Double> chances,
    Map<AirdropQuality, Double> promotionChances,
    Map<AirdropQuality, AirdropQualityProfile> profiles,
    boolean allowCurses,
    boolean debug
) {
    private static final double SUM_TOLERANCE = 0.000_001;

    public AirdropQualitySettings {
        chances = immutableCompleteMap(chances, "quality chances");
        promotionChances = immutableCompleteMap(promotionChances, "promotion chances");
        profiles = immutableCompleteMap(profiles, "quality profiles");
        validateProgression(profiles);
        if (Math.abs(chances.values().stream().mapToDouble(Double::doubleValue).sum() - 1.0) > SUM_TOLERANCE) {
            throw new IllegalArgumentException("airdrop quality chances must sum to 1.0");
        }
        chances.forEach(AirdropQualitySettings::validateChance);
        promotionChances.forEach(AirdropQualitySettings::validateChance);
        if (promotionChances.get(AirdropQuality.GHISTIC) != 0.0) {
            throw new IllegalArgumentException("GHISTIC promotion chance must be zero");
        }
    }

    public static AirdropQualitySettings defaults() {
        EnumMap<AirdropQuality, Double> chances = new EnumMap<>(AirdropQuality.class);
        chances.put(AirdropQuality.COMMON, 0.39);
        chances.put(AirdropQuality.RARE, 0.30);
        chances.put(AirdropQuality.EPIC, 0.20);
        chances.put(AirdropQuality.LEGENDARY, 0.10);
        chances.put(AirdropQuality.GHISTIC, 0.01);

        EnumMap<AirdropQuality, Double> promotions = new EnumMap<>(AirdropQuality.class);
        promotions.put(AirdropQuality.COMMON, 0.12);
        promotions.put(AirdropQuality.RARE, 0.10);
        promotions.put(AirdropQuality.EPIC, 0.07);
        promotions.put(AirdropQuality.LEGENDARY, 0.03);
        promotions.put(AirdropQuality.GHISTIC, 0.0);

        EnumMap<AirdropQuality, AirdropQualityProfile> profiles = new EnumMap<>(AirdropQuality.class);
        profiles.put(AirdropQuality.COMMON, new AirdropQualityProfile(4, 1.0, 1.0, 1, 4, 0.20, 0.05, false));
        profiles.put(AirdropQuality.RARE, new AirdropQualityProfile(6, 1.15, 1.10, 2, 5, 0.35, 0.03, false));
        profiles.put(AirdropQuality.EPIC, new AirdropQualityProfile(8, 1.30, 1.20, 3, 6, 0.50, 0.02, true));
        profiles.put(AirdropQuality.LEGENDARY, new AirdropQualityProfile(11, 1.50, 1.35, 4, 8, 0.70, 0.01, true));
        profiles.put(AirdropQuality.GHISTIC, new AirdropQualityProfile(15, 1.75, 1.50, 5, 10, 0.90, 0.0, true));
        return new AirdropQualitySettings(chances, promotions, profiles, false, false);
    }

    public static AirdropQualitySettings fromConfig(FileConfiguration config, String prefix) {
        AirdropQualitySettings defaults = defaults();
        EnumMap<AirdropQuality, Double> chances = readChances(config, prefix + "quality-chances.", defaults.chances());
        if (Math.abs(chances.values().stream().mapToDouble(Double::doubleValue).sum() - 1.0) > SUM_TOLERANCE) {
            chances = new EnumMap<>(defaults.chances());
        }
        EnumMap<AirdropQuality, Double> promotions = readChances(
            config, prefix + "loot-quality-promotion.next-quality-chance.", defaults.promotionChances());
        if (!config.getBoolean(prefix + "loot-quality-promotion.enabled", true)) {
            promotions.replaceAll((ignored, value) -> 0.0);
        }
        promotions.put(AirdropQuality.GHISTIC, 0.0);

        EnumMap<AirdropQuality, AirdropQualityProfile> profiles = new EnumMap<>(AirdropQuality.class);
        for (AirdropQuality quality : AirdropQuality.values()) {
            AirdropQualityProfile fallback = defaults.profile(quality);
            String path = prefix + "qualities." + quality.name() + ".";
            profiles.put(quality, new AirdropQualityProfile(
                bounded(config.getInt(path + "guard-count", fallback.guardCount()), 0, 100),
                bounded(config.getDouble(path + "health-multiplier", fallback.healthMultiplier()), 1.0, 10.0),
                bounded(config.getDouble(path + "damage-multiplier", fallback.damageMultiplier()), 1.0, 10.0),
                bounded(config.getInt(path + "equipment-level", fallback.equipmentLevel()), 1, 5),
                bounded(config.getInt(path + "loot-rolls", fallback.lootRolls()), 1, 27),
                bounded(config.getDouble(path + "custom-item-chance", fallback.customItemChance()), 0.0, 1.0),
                bounded(config.getDouble(path + "equipment-drop-chance", fallback.equipmentDropChance()), 0.0, 1.0),
                config.getBoolean(path + "announce-globally", fallback.announceGlobally())
            ));
        }
        if (!isProgressive(profiles)) {
            profiles = new EnumMap<>(defaults.profiles());
        }
        return new AirdropQualitySettings(
            chances,
            promotions,
            profiles,
            config.getBoolean(prefix + "enchantments.allow-curses", false),
            config.getBoolean(prefix + "debug", false)
        );
    }

    public AirdropQualityProfile profile(AirdropQuality quality) {
        return profiles.get(quality);
    }

    public double totalChance() {
        return chances.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    private static EnumMap<AirdropQuality, Double> readChances(
        FileConfiguration config,
        String prefix,
        Map<AirdropQuality, Double> fallbacks
    ) {
        EnumMap<AirdropQuality, Double> values = new EnumMap<>(AirdropQuality.class);
        for (AirdropQuality quality : AirdropQuality.values()) {
            double fallback = fallbacks.get(quality);
            values.put(quality, bounded(config.getDouble(prefix + quality.name(), fallback), 0.0, 1.0));
        }
        return values;
    }

    private static <V> Map<AirdropQuality, V> immutableCompleteMap(
        Map<AirdropQuality, V> source,
        String name
    ) {
        if (source == null || source.size() != AirdropQuality.values().length) {
            throw new IllegalArgumentException(name + " must define every quality");
        }
        EnumMap<AirdropQuality, V> copy = new EnumMap<>(AirdropQuality.class);
        for (AirdropQuality quality : AirdropQuality.values()) {
            V value = source.get(quality);
            if (value == null) {
                throw new IllegalArgumentException(name + " is missing " + quality);
            }
            copy.put(quality, value);
        }
        return Map.copyOf(copy);
    }

    private static void validateChance(AirdropQuality quality, double value) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException("invalid chance for " + quality + ": " + value);
        }
    }

    private static void validateProgression(Map<AirdropQuality, AirdropQualityProfile> profiles) {
        if (!isProgressive(profiles)) {
            throw new IllegalArgumentException("airdrop quality profiles must become progressively harder");
        }
    }

    private static boolean isProgressive(Map<AirdropQuality, AirdropQualityProfile> profiles) {
        AirdropQualityProfile previous = null;
        for (AirdropQuality quality : AirdropQuality.values()) {
            AirdropQualityProfile current = profiles.get(quality);
            if (previous != null && (current.guardCount() <= previous.guardCount()
                || current.healthMultiplier() <= previous.healthMultiplier()
                || current.damageMultiplier() <= previous.damageMultiplier()
                || current.equipmentLevel() <= previous.equipmentLevel()
                || current.lootRolls() <= previous.lootRolls())) {
                return false;
            }
            previous = current;
        }
        return true;
    }

    private static int bounded(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static double bounded(double value, double minimum, double maximum) {
        return Double.isFinite(value) ? Math.max(minimum, Math.min(maximum, value)) : minimum;
    }
}
