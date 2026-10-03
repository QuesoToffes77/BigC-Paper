package dev.linqfy.bigCasares.modules.bloodmoon;

import org.bukkit.Material;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

final class BloodMoonLootPolicy {

    private final BloodMoonLootSettings settings;
    private final Random random;

    BloodMoonLootPolicy(BloodMoonLootSettings settings) {
        this(settings, new Random());
    }

    BloodMoonLootPolicy(BloodMoonLootSettings settings, Random random) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.random = Objects.requireNonNull(random, "random");
    }

    Optional<BloodMoonLootRoll> roll(boolean eligible) {
        if (!eligible || !settings.enabled() || settings.materials().isEmpty()
            || random.nextDouble() >= settings.bonusChance()) {
            return Optional.empty();
        }
        Material material = settings.materials().get(random.nextInt(settings.materials().size()));
        int amount = randomBetween(settings.minimumAmount(), settings.maximumAmount());
        if (material == Material.EMERALD) {
            amount = Math.min(amount, 1);
        }
        return Optional.of(new BloodMoonLootRoll(
            material,
            amount,
            randomBetween(settings.minimumExperience(), settings.maximumExperience())
        ));
    }

    private int randomBetween(int minimum, int maximum) {
        return minimum + random.nextInt(maximum - minimum + 1);
    }
}

record BloodMoonLootSettings(
    boolean enabled,
    double bonusChance,
    int minimumAmount,
    int maximumAmount,
    int minimumExperience,
    int maximumExperience,
    List<Material> materials
) {
    BloodMoonLootSettings {
        bonusChance = Double.isFinite(bonusChance) ? Math.max(0.0, Math.min(1.0, bonusChance)) : 0.0;
        minimumAmount = Math.max(0, Math.min(64, minimumAmount));
        maximumAmount = Math.max(minimumAmount, Math.min(64, maximumAmount));
        minimumExperience = Math.max(0, minimumExperience);
        maximumExperience = Math.max(minimumExperience, maximumExperience);
        materials = materials == null ? List.of() : List.copyOf(materials);
    }

    static BloodMoonLootSettings defaults() {
        return new BloodMoonLootSettings(
            true, 0.15, 1, 3, 2, 6,
            List.of(Material.IRON_NUGGET, Material.GOLD_NUGGET, Material.REDSTONE, Material.EMERALD)
        );
    }

    static BloodMoonLootSettings disabled() {
        BloodMoonLootSettings defaults = defaults();
        return new BloodMoonLootSettings(false, 0.0, 0, 0, 0, 0, defaults.materials());
    }
}

record BloodMoonLootRoll(Material material, int amount, int experience) {
}
