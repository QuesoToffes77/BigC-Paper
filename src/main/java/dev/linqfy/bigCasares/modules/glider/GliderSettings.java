package dev.linqfy.bigCasares.modules.glider;

import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/** Validated, immutable tuning for the Glider module. */
public record GliderSettings(
    boolean enabled,
    GliderActivationMode activationMode,
    double minimumFallSpeed,
    boolean allowSurvival,
    boolean allowAdventure,
    boolean allowCreative,
    int tickRate,
    int maxSessionTicks,
    double maxAltitudeGain,
    GliderVisualSettings visuals,
    GliderParticles particles,
    GliderSounds sounds,
    Map<GliderTier, GliderTierStats> tiers
) {

    public GliderSettings {
        activationMode = activationMode == null ? GliderActivationMode.SNEAK : activationMode;
        minimumFallSpeed = clamp(minimumFallSpeed, -1.0, -0.001);
        tickRate = clamp(tickRate, 1, 10);
        maxSessionTicks = clamp(maxSessionTicks, 20, 72_000);
        maxAltitudeGain = clamp(maxAltitudeGain, 0.0, 8.0);
        visuals = visuals == null ? GliderVisualSettings.defaults() : visuals;
        particles = particles == null ? GliderParticles.defaults() : particles;
        sounds = sounds == null ? GliderSounds.defaults() : sounds;
        tiers = normalizeTiers(tiers);
    }

    public static GliderSettings defaults() {
        return new GliderSettings(true, GliderActivationMode.SNEAK, -0.05,
            true, true, true, 1, 12_000, 2.0,
            GliderVisualSettings.defaults(),
            GliderParticles.defaults(), GliderSounds.defaults(), defaultTiers());
    }

    public static GliderSettings load(FileConfiguration config, Consumer<String> warnings) {
        Consumer<String> sink = warnings == null ? ignored -> { } : warnings;
        ConfigurationSection root = config == null ? null : config.getConfigurationSection("glider");
        if (root == null) {
            return defaults();
        }

        EnumMap<GliderTier, GliderTierStats> loadedTiers = new EnumMap<>(GliderTier.class);
        for (GliderTier tier : GliderTier.values()) {
            String path = "tiers." + tier.number();
            GliderTierStats fallback = GliderTierStats.defaults(tier);
            boolean boostEnabled = root.getBoolean(path + ".boost.enabled", fallback.boostEnabled());
            if (tier.number() < 3) {
                boostEnabled = false;
            }
            loadedTiers.put(tier, new GliderTierStats(
                root.getDouble(path + ".forward-speed", fallback.forwardSpeed()),
                root.getDouble(path + ".max-fall-speed", fallback.maxFallSpeed()),
                root.getDouble(path + ".steering", fallback.steering()),
                root.getDouble(path + ".lift", fallback.lift()),
                boostEnabled,
                root.getDouble(path + ".boost.power", fallback.boostPower()),
                root.getInt(path + ".boost.cooldown", fallback.boostCooldownTicks()),
                root.getDouble(path + ".fall-damage-reduction", fallback.fallDamageReduction())
            ));
        }
        if (!validProgression(loadedTiers)) {
            sink.accept("[Glider] La progresion de tiers es invalida; se usan valores seguros por defecto.");
            loadedTiers.clear();
            loadedTiers.putAll(defaultTiers());
        }

        return new GliderSettings(
            root.getBoolean("enabled", true),
            parsedActivationMode(root.getString("activation.mode", "SNEAK"), sink),
            root.getDouble("activation.minimum-fall-speed", -0.05),
            root.getBoolean("activation.allow-survival", true),
            root.getBoolean("activation.allow-adventure", true),
            root.getBoolean("activation.allow-creative", true),
            root.getInt("physics.tick-rate", 1),
            root.getInt("physics.max-session-ticks", 12_000),
            root.getDouble("physics.max-altitude-gain", 2.0),
            new GliderVisualSettings(
                root.getBoolean("visual.enabled", true),
                root.getDouble("visual.vertical-offset", 2.55),
                root.getDouble("visual.scale", 1.55),
                root.getDouble("visual.yaw-offset-degrees", 0.0),
                root.getInt("visual.interpolation-ticks", 3),
                root.getInt("visual.teleport-duration-ticks", 2),
                (float) root.getDouble("visual.view-range", 1.5)
            ),
            new GliderParticles(
                root.getBoolean("particles.enabled", true),
                root.getInt("particles.interval", 3)
            ),
            new GliderSounds(
                root.getBoolean("sounds.enabled", true),
                root.getString("sounds.start", "ITEM_ARMOR_EQUIP_ELYTRA"),
                root.getString("sounds.boost", "ENTITY_FIREWORK_ROCKET_LAUNCH"),
                root.getString("sounds.landing", "BLOCK_WOOL_FALL")
            ),
            loadedTiers
        );
    }

    public GliderTierStats stats(GliderTier tier) {
        return tiers.getOrDefault(tier, GliderTierStats.defaults(tier));
    }

    public boolean allows(GameMode mode) {
        return switch (mode) {
            case SURVIVAL -> allowSurvival;
            case ADVENTURE -> allowAdventure;
            case CREATIVE -> allowCreative;
            case SPECTATOR -> false;
        };
    }

    private static boolean validProgression(Map<GliderTier, GliderTierStats> tiers) {
        GliderTierStats previous = null;
        for (GliderTier tier : GliderTier.values()) {
            GliderTierStats current = tiers.get(tier);
            if (current == null || previous != null && !current.isStrictUpgradeFrom(previous)) {
                return false;
            }
            if (tier.number() < 3 && current.boostEnabled()) {
                return false;
            }
            if (tier.number() >= 3 && !current.boostEnabled()) {
                return false;
            }
            if (tier.number() >= 4
                && (current.boostPower() <= previous.boostPower()
                    || current.boostCooldownTicks() >= previous.boostCooldownTicks())) {
                return false;
            }
            previous = current;
        }
        return true;
    }

    private static Map<GliderTier, GliderTierStats> defaultTiers() {
        EnumMap<GliderTier, GliderTierStats> values = new EnumMap<>(GliderTier.class);
        for (GliderTier tier : GliderTier.values()) {
            values.put(tier, GliderTierStats.defaults(tier));
        }
        return Map.copyOf(values);
    }

    private static Map<GliderTier, GliderTierStats> normalizeTiers(Map<GliderTier, GliderTierStats> source) {
        EnumMap<GliderTier, GliderTierStats> normalized = new EnumMap<>(GliderTier.class);
        for (GliderTier tier : GliderTier.values()) {
            GliderTierStats value = source == null ? null : source.get(tier);
            normalized.put(tier, value == null ? GliderTierStats.defaults(tier) : value);
        }
        return Map.copyOf(normalized);
    }

    private static GliderActivationMode parsedActivationMode(String value, Consumer<String> warnings) {
        return GliderActivationMode.parse(value).orElseGet(() -> {
            warnings.accept("[Glider] activation.mode invalido; se usa SNEAK.");
            return GliderActivationMode.SNEAK;
        });
    }

    static double clamp(double value, double minimum, double maximum) {
        if (!Double.isFinite(value)) {
            return minimum;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }

    static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}

record GliderVisualSettings(
    boolean enabled,
    double verticalOffset,
    double scale,
    double yawOffsetDegrees,
    int interpolationTicks,
    int teleportDurationTicks,
    float viewRange
) {
    GliderVisualSettings {
        verticalOffset = GliderSettings.clamp(verticalOffset, 1.5, 4.0);
        scale = GliderSettings.clamp(scale, 0.5, 4.0);
        yawOffsetDegrees = GliderSettings.clamp(yawOffsetDegrees, -360.0, 360.0);
        interpolationTicks = GliderSettings.clamp(interpolationTicks, 0, 20);
        teleportDurationTicks = GliderSettings.clamp(teleportDurationTicks, 0, 59);
        viewRange = (float) GliderSettings.clamp(viewRange, 0.25, 8.0);
    }

    static GliderVisualSettings defaults() {
        return new GliderVisualSettings(true, 2.55, 1.55, 0.0, 3, 2, 1.5F);
    }
}

record GliderParticles(boolean enabled, int intervalTicks) {
    GliderParticles {
        intervalTicks = GliderSettings.clamp(intervalTicks, 1, 100);
    }

    static GliderParticles defaults() {
        return new GliderParticles(true, 3);
    }
}

record GliderSounds(boolean enabled, String start, String boost, String landing) {
    GliderSounds {
        start = normalize(start, "ITEM_ARMOR_EQUIP_ELYTRA");
        boost = normalize(boost, "ENTITY_FIREWORK_ROCKET_LAUNCH");
        landing = normalize(landing, "BLOCK_WOOL_FALL");
    }

    static GliderSounds defaults() {
        return new GliderSounds(true, "ITEM_ARMOR_EQUIP_ELYTRA", "ENTITY_FIREWORK_ROCKET_LAUNCH",
            "BLOCK_WOOL_FALL");
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().toUpperCase(Locale.ROOT);
    }
}
