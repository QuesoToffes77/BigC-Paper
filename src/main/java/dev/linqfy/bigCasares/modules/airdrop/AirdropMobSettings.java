package dev.linqfy.bigCasares.modules.airdrop;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Defender mob tuning for a generated AirDrop. Every knob is bounded by the
 * compact constructors so no accidental config can create hundreds of
 * entities: zombie amounts and radii are capped, rider slots are small, and
 * {@code maxTotal} is a hard ceiling enforced before spawning.
 */
public record AirdropMobSettings(
        boolean enabled,
        int maxTotal,
        AirdropZombieSettings zombies,
        AirdropHorseRiderSettings horseRiders,
        AirdropPhantomCreeperSettings phantomCreepers
) {
    public AirdropMobSettings {
        maxTotal = Math.max(0, Math.min(100, maxTotal));
        zombies = zombies == null ? AirdropZombieSettings.defaults() : zombies;
        horseRiders = horseRiders == null ? AirdropHorseRiderSettings.defaults() : horseRiders;
        phantomCreepers = phantomCreepers == null
            ? AirdropPhantomCreeperSettings.defaults()
            : phantomCreepers;
    }

    public AirdropMobSettings(
        boolean enabled,
        int maxTotal,
        AirdropZombieSettings zombies,
        AirdropHorseRiderSettings horseRiders
    ) {
        this(enabled, maxTotal, zombies, horseRiders, AirdropPhantomCreeperSettings.defaults());
    }

    static AirdropMobSettings defaults() {
        return new AirdropMobSettings(true, 40,
                AirdropZombieSettings.defaults(), AirdropHorseRiderSettings.defaults(),
                AirdropPhantomCreeperSettings.defaults());
    }

    static AirdropMobSettings disabled() {
        return new AirdropMobSettings(false, 0,
                AirdropZombieSettings.defaults(), AirdropHorseRiderSettings.defaults(),
                AirdropPhantomCreeperSettings.defaults());
    }

    static AirdropMobSettings fromConfig(FileConfiguration config, String prefix) {
        return new AirdropMobSettings(
                config.getBoolean(prefix + "enabled", true),
                Math.max(0, Math.min(100, config.getInt(prefix + "max-total", 40))),
                new AirdropZombieSettings(
                        Math.max(0, Math.min(64, config.getInt(prefix + "zombies.amount", 20))),
                        Math.max(0, Math.min(64, config.getInt(prefix + "zombies.radius", 12))),
                        Math.max(0, Math.min(100, config.getInt(prefix + "zombies.gear-chance", 40)))
                ),
                new AirdropHorseRiderSettings(
                        config.getBoolean(prefix + "horse-riders.enabled", true),
                        Math.max(0, Math.min(16, config.getInt(prefix + "horse-riders.amount", 2))),
                        Math.max(0, Math.min(100, config.getInt(prefix + "horse-riders.chance", 20)))
                ),
                new AirdropPhantomCreeperSettings(
                        config.getBoolean(prefix + "phantom-creepers.enabled", true),
                        config.getInt(prefix + "phantom-creepers.epic-amount", 1),
                        config.getInt(prefix + "phantom-creepers.legendary-amount", 2),
                        config.getInt(prefix + "phantom-creepers.ghistic-amount", 3),
                        config.getInt(prefix + "phantom-creepers.fire-resistance-minutes", 30),
                        config.getInt(prefix + "phantom-creepers.spawn-height", 12)
                )
        );
    }
}

/**
 * The common zombie bodyguard: how many spawn around the chest, within what
 * radius, and the percentage of them that carry armor/weapons. The zombies
 * are regular zombies with vanilla AI - never mini-bosses.
 */
record AirdropZombieSettings(int amount, int radius, int gearChance) {
    AirdropZombieSettings {
        amount = Math.max(0, Math.min(64, amount));
        radius = Math.max(1, Math.min(64, radius));
        gearChance = Math.max(0, Math.min(100, gearChance));
    }

    static AirdropZombieSettings defaults() {
        return new AirdropZombieSettings(20, 12, 40);
    }
}

/**
 * The rare mounted threat: a zombie riding a horse and wielding a trident.
 * {@code amount} is the maximum number of rider attempts and
 * {@code chancePercent} the chance each attempt actually spawns, so they stay
 * an occasional danger instead of a guaranteed army.
 */
record AirdropHorseRiderSettings(boolean enabled, int amount, int chancePercent) {
    AirdropHorseRiderSettings {
        amount = Math.max(0, Math.min(16, amount));
        chancePercent = Math.max(0, Math.min(100, chancePercent));
    }

    static AirdropHorseRiderSettings defaults() {
        return new AirdropHorseRiderSettings(true, 2, 20);
    }
}

/** A flying Phantom carrying a Creeper, enabled only for EPIC and higher drops. */
record AirdropPhantomCreeperSettings(
    boolean enabled,
    int epicAmount,
    int legendaryAmount,
    int ghisticAmount,
    int fireResistanceMinutes,
    int spawnHeight
) {
    AirdropPhantomCreeperSettings {
        epicAmount = clampAmount(epicAmount);
        legendaryAmount = Math.max(epicAmount, clampAmount(legendaryAmount));
        ghisticAmount = Math.max(legendaryAmount, clampAmount(ghisticAmount));
        fireResistanceMinutes = Math.max(0, Math.min(120, fireResistanceMinutes));
        spawnHeight = Math.max(6, Math.min(48, spawnHeight));
    }

    static AirdropPhantomCreeperSettings defaults() {
        return new AirdropPhantomCreeperSettings(true, 1, 2, 3, 30, 12);
    }

    int amountFor(AirdropQuality quality) {
        if (!enabled || quality == null) {
            return 0;
        }
        return switch (quality) {
            case COMMON, RARE -> 0;
            case EPIC -> epicAmount;
            case LEGENDARY -> legendaryAmount;
            case GHISTIC -> ghisticAmount;
        };
    }

    int fireResistanceTicks() {
        return fireResistanceMinutes * 60 * 20;
    }

    private static int clampAmount(int value) {
        return Math.max(0, Math.min(16, value));
    }
}
