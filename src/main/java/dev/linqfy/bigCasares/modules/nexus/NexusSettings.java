package dev.linqfy.bigCasares.modules.nexus;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

public record NexusSettings(
        double maximumHealth,
        NexusDamageMultipliers damageMultipliers,
        NexusDamagePolicy damagePolicy,
        NexusPlacementSettings placement
) {

    public NexusSettings {
        if (!Double.isFinite(maximumHealth) || maximumHealth <= 0.0) {
            throw new IllegalArgumentException("maximumHealth must be finite and positive");
        }
        Objects.requireNonNull(damageMultipliers, "damageMultipliers");
        Objects.requireNonNull(damagePolicy, "damagePolicy");
        Objects.requireNonNull(placement, "placement");
    }

    public static NexusSettings defaults() {
        return new NexusSettings(
                500.0,
                NexusDamageMultipliers.defaults(),
                NexusDamagePolicy.safeDefaults(),
                NexusPlacementSettings.defaults()
        );
    }

    public static NexusSettings fromConfig(ConfigurationSection root) {
        if (root == null) {
            return defaults();
        }
        NexusSettings defaults = defaults();
        ConfigurationSection section = root.getConfigurationSection("nexus-system");
        if (section == null) {
            section = root;
        }

        NexusDamageMultipliers damage = new NexusDamageMultipliers(
                section.getDouble("damage.multipliers.melee", defaults.damageMultipliers().melee()),
                section.getDouble("damage.multipliers.projectile", defaults.damageMultipliers().projectile()),
                section.getDouble("damage.multipliers.explosion", defaults.damageMultipliers().explosion()),
                section.getDouble("damage.multipliers.fire", defaults.damageMultipliers().fire()),
                section.getDouble("damage.multipliers.magic", defaults.damageMultipliers().magic()),
                section.getDouble("damage.multipliers.environment", defaults.damageMultipliers().environment()),
                section.getDouble("damage.multipliers.custom", defaults.damageMultipliers().custom())
        );
        NexusPlacementSettings placement = new NexusPlacementSettings(
                section.getInt(
                        "placement.container-clearance-horizontal",
                        defaults.placement().containerClearanceHorizontal()
                ),
                section.getInt(
                        "placement.container-clearance-vertical",
                        defaults.placement().containerClearanceVertical()
                ),
                section.getInt("placement.minimum-open-faces", defaults.placement().minimumOpenFaces()),
                section.getInt("placement.minimum-walkable-width", defaults.placement().minimumWalkableWidth()),
                section.getBoolean(
                        "placement.prevent-doorway-placement",
                        defaults.placement().preventDoorwayPlacement()
                )
        );
        return new NexusSettings(
                section.getDouble("maximum-health", defaults.maximumHealth()),
                damage,
                new NexusDamagePolicy(
                    section.getBoolean("damage.allow-natural-entities", false),
                    section.getBoolean("damage.allow-unowned-explosions", false)
                ),
                placement
        );
    }
}
