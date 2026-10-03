package dev.linqfy.bigCasares.modules.bloodmoon;

import dev.linqfy.bigCasares.modules.environment.EnvironmentalVisualSettings;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public record BloodMoonSettings(
    boolean enabled,
    BloodMoonSchedulingSettings scheduling,
    Set<String> worlds,
    BloodMoonMobSettings mobs,
    BloodMoonSpawningSettings spawning,
    EnvironmentalVisualSettings visuals,
    BloodMoonLootSettings loot,
    BloodMoonSoundSettings sounds,
    boolean allowWithOtherEvents,
    boolean debug
) {
    public BloodMoonSettings {
        scheduling = scheduling == null ? BloodMoonSchedulingSettings.defaults() : scheduling;
        worlds = worlds == null ? Set.of("world") : Set.copyOf(worlds);
        mobs = mobs == null ? BloodMoonMobSettings.defaults() : mobs;
        spawning = spawning == null ? BloodMoonSpawningSettings.defaults() : spawning;
        visuals = visuals == null ? EnvironmentalVisualSettings.defaults() : visuals;
        loot = loot == null ? BloodMoonLootSettings.defaults() : loot;
        sounds = sounds == null ? BloodMoonSoundSettings.defaults() : sounds;
    }

    static BloodMoonSettings defaults() {
        return new BloodMoonSettings(
            true,
            BloodMoonSchedulingSettings.defaults(),
            Set.of("world"),
            BloodMoonMobSettings.defaults(),
            BloodMoonSpawningSettings.defaults(),
            EnvironmentalVisualSettings.defaults(),
            BloodMoonLootSettings.defaults(),
            BloodMoonSoundSettings.defaults(),
            false,
            false
        );
    }

    static BloodMoonSettings disabled() {
        BloodMoonSettings defaults = defaults();
        return new BloodMoonSettings(
            false,
            defaults.scheduling(),
            defaults.worlds(),
            defaults.mobs(),
            BloodMoonSpawningSettings.disabled(),
            EnvironmentalVisualSettings.disabled(),
            BloodMoonLootSettings.disabled(),
            defaults.sounds(),
            false,
            defaults.debug()
        );
    }

    boolean affectsWorld(String worldName) {
        return worldName != null && worlds.stream().anyMatch(worldName::equalsIgnoreCase);
    }
}

enum BloodMoonSchedulingMode {
    CHANCE,
    INTERVAL;

    static Optional<BloodMoonSchedulingMode> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(raw.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}

record BloodMoonSchedulingSettings(
    BloodMoonSchedulingMode mode,
    double chancePerNight,
    int minimumNormalNightsBetweenEvents,
    int everyNights
) {
    BloodMoonSchedulingSettings {
        mode = mode == null ? BloodMoonSchedulingMode.CHANCE : mode;
        chancePerNight = Math.max(0.0, Math.min(1.0, chancePerNight));
        minimumNormalNightsBetweenEvents = Math.max(0, minimumNormalNightsBetweenEvents);
        everyNights = Math.max(1, everyNights);
    }

    static BloodMoonSchedulingSettings defaults() {
        return new BloodMoonSchedulingSettings(BloodMoonSchedulingMode.CHANCE, 0.12, 2, 5);
    }
}

record BloodMoonMobSettings(
    double healthMultiplier,
    double movementSpeedMultiplier,
    double damageMultiplier,
    Set<String> whitelist,
    Set<String> blacklist
) {
    BloodMoonMobSettings {
        whitelist = whitelist == null ? Set.of() : Set.copyOf(whitelist);
        blacklist = blacklist == null ? Set.of() : Set.copyOf(blacklist);
    }

    static BloodMoonMobSettings defaults() {
        return new BloodMoonMobSettings(1.5, 1.25, 1.0, Set.of(), Set.of());
    }
}

record BloodMoonSpawningSettings(
    double multiplier,
    int intervalTicks,
    int minDistanceFromPlayer,
    int maxDistanceFromPlayer,
    int maxExtraHostilesPerPlayer,
    int maxExtraHostilesPerWorld,
    List<String> types
) {
    BloodMoonSpawningSettings {
        types = types == null ? List.of() : List.copyOf(types);
    }

    static BloodMoonSpawningSettings defaults() {
        return new BloodMoonSpawningSettings(
            2.0, 100, 24, 56, 20, 200,
            List.of("ZOMBIE", "SKELETON", "SPIDER", "CREEPER", "HUSK", "STRAY")
        );
    }

    static BloodMoonSpawningSettings disabled() {
        return new BloodMoonSpawningSettings(1.0, 100, 24, 56, 0, 0, List.of());
    }
}

record BloodMoonSoundSettings(
    boolean enabled,
    String startSound,
    String stopSound,
    float volume,
    float pitch
) {
    BloodMoonSoundSettings {
        startSound = startSound == null || startSound.isBlank() ? "ENTITY_WITHER_SPAWN" : startSound;
        stopSound = stopSound == null || stopSound.isBlank() ? "BLOCK_BEACON_DEACTIVATE" : stopSound;
        volume = Float.isFinite(volume) ? Math.max(0.0f, Math.min(2.0f, volume)) : 1.0f;
        pitch = Float.isFinite(pitch) ? Math.max(0.0f, Math.min(2.0f, pitch)) : 1.0f;
    }

    static BloodMoonSoundSettings defaults() {
        return new BloodMoonSoundSettings(true, "ENTITY_WITHER_SPAWN", "BLOCK_BEACON_DEACTIVATE", 0.8f, 0.8f);
    }
}
