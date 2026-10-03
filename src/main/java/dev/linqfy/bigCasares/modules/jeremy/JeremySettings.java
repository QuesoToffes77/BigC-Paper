package dev.linqfy.bigCasares.modules.jeremy;

import java.util.List;
import java.util.Set;

record JeremySettings(
    boolean enabled,
    String displayName,
    JeremyTimingSettings timing,
    JeremyTargetingSettings targeting,
    JeremySpawnSettings spawn,
    JeremyMovementSettings movement,
    JeremyHealthSettings health,
    JeremyDamageSettings damage,
    JeremyGoldenHelmetSettings goldenHelmet,
    JeremyPathfindingSettings pathfinding,
    JeremyUltrasoundSettings ultrasound,
    JeremyCelebrationSettings celebration,
    JeremyRepositionSettings reposition,
    JeremyLootSettings loot,
    JeremySoundSettings sounds,
    boolean debug
) {
    JeremySettings {
        displayName = displayName == null || displayName.isBlank() ? "Jeremy" : displayName;
        loot = loot == null ? JeremyLootSettings.defaults() : loot;
    }

    static JeremySettings defaults() {
        return new JeremySettings(
            true,
            "Jeremy",
            JeremyTimingSettings.defaults(),
            JeremyTargetingSettings.defaults(),
            JeremySpawnSettings.defaults(),
            JeremyMovementSettings.defaults(),
            JeremyHealthSettings.defaults(),
            JeremyDamageSettings.defaults(),
            JeremyGoldenHelmetSettings.defaults(),
            JeremyPathfindingSettings.defaults(),
            JeremyUltrasoundSettings.defaults(),
            JeremyCelebrationSettings.defaults(),
            JeremyRepositionSettings.defaults(),
            JeremyLootSettings.defaults(),
            JeremySoundSettings.defaults(),
            false
        );
    }

    static JeremySettings disabled() {
        JeremySettings defaults = defaults();
        return new JeremySettings(
            false, defaults.displayName(), defaults.timing(), defaults.targeting(), defaults.spawn(),
            defaults.movement(), defaults.health(), defaults.damage(), defaults.goldenHelmet(),
            defaults.pathfinding(), JeremyUltrasoundSettings.disabled(), defaults.celebration(),
            defaults.reposition(), JeremyLootSettings.disabled(), defaults.sounds(), defaults.debug()
        );
    }
}

record JeremyTimingSettings(
    long huntMillis,
    long restMillis,
    long noPlayerRetryMillis,
    long killCreditMillis,
    long celebrationMillis
) {
    static JeremyTimingSettings defaults() {
        return new JeremyTimingSettings(420_000L, 3_600_000L, 60_000L, 5_000L, 4_000L);
    }
}

record JeremyTargetingSettings(
    boolean survival,
    boolean adventure,
    boolean creative,
    boolean spectator,
    boolean avoidLastTarget,
    Set<String> enabledWorlds,
    Set<String> excludedWorlds
) {
    JeremyTargetingSettings {
        enabledWorlds = enabledWorlds == null ? Set.of("world") : Set.copyOf(enabledWorlds);
        excludedWorlds = excludedWorlds == null ? Set.of() : Set.copyOf(excludedWorlds);
    }

    static JeremyTargetingSettings defaults() {
        return new JeremyTargetingSettings(true, true, false, false, true, Set.of("world"), Set.of());
    }

    boolean allowsWorld(String worldName) {
        if (worldName == null || excludedWorlds.stream().anyMatch(worldName::equalsIgnoreCase)) {
            return false;
        }
        return enabledWorlds.isEmpty() || enabledWorlds.stream().anyMatch(worldName::equalsIgnoreCase);
    }
}

record JeremySpawnSettings(int minDistance, int maxDistance, int attempts) {
    static JeremySpawnSettings defaults() {
        return new JeremySpawnSettings(24, 40, 24);
    }
}

record JeremyMovementSettings(boolean useBabyZombieSpeed, double configuredSpeed) {
    static final double BABY_ZOMBIE_EQUIVALENT_SPEED = 0.345D;

    static JeremyMovementSettings defaults() {
        return new JeremyMovementSettings(true, BABY_ZOMBIE_EQUIVALENT_SPEED);
    }

    double effectiveSpeed() {
        return useBabyZombieSpeed ? BABY_ZOMBIE_EQUIVALENT_SPEED : configuredSpeed;
    }
}

record JeremyHealthSettings(double base, double maximumPowerMultiplier) {
    static JeremyHealthSettings defaults() {
        return new JeremyHealthSettings(40.0, 1.25);
    }

    double scaledHealth(double powerScore) {
        double normalized = Math.max(0.0, Math.min(100.0, powerScore)) / 100.0;
        return base * (1.0 + normalized * (maximumPowerMultiplier - 1.0));
    }
}

record JeremyDamageSettings(
    double minimum,
    double maximum,
    int powerRecalculationTicks,
    List<Double> scoreThresholds,
    List<Double> tierDamage
) {
    JeremyDamageSettings {
        scoreThresholds = scoreThresholds == null ? List.of(15.0, 35.0, 60.0, 90.0) : List.copyOf(scoreThresholds);
        tierDamage = tierDamage == null ? List.of(4.0, 5.0, 7.0, 9.0, 11.0) : List.copyOf(tierDamage);
    }

    static JeremyDamageSettings defaults() {
        return new JeremyDamageSettings(
            4.0, 11.0, 200,
            List.of(15.0, 35.0, 60.0, 90.0),
            List.of(4.0, 5.0, 7.0, 9.0, 11.0)
        );
    }

    double damageFor(double powerScore) {
        int index = 0;
        while (index < scoreThresholds.size() && powerScore >= scoreThresholds.get(index)) {
            index++;
        }
        double configured = tierDamage.get(Math.min(index, tierDamage.size() - 1));
        return Math.max(minimum, Math.min(maximum, configured));
    }
}

record JeremyGoldenHelmetSettings(double damageMultiplier) {
    static JeremyGoldenHelmetSettings defaults() {
        return new JeremyGoldenHelmetSettings(1.5);
    }
}

record JeremyPathfindingSettings(int stuckDetectionTicks, double minimumProgress, int sampleIntervalTicks) {
    static JeremyPathfindingSettings defaults() {
        return new JeremyPathfindingSettings(80, 1.5, 20);
    }
}

record JeremyWallPenetrationSettings(boolean enabled, int maxBlocks) {
    static JeremyWallPenetrationSettings defaults() {
        return new JeremyWallPenetrationSettings(true, 2);
    }
}

record JeremyUltrasoundSettings(
    boolean enabled,
    double range,
    int chargeTicks,
    int cooldownTicks,
    double damageMultiplierVsMelee,
    double knockback,
    JeremyWallPenetrationSettings wallPenetration
) {
    static JeremyUltrasoundSettings defaults() {
        return new JeremyUltrasoundSettings(
            true, 32.0, 30, 100, 0.8, 0.4, JeremyWallPenetrationSettings.defaults()
        );
    }

    static JeremyUltrasoundSettings disabled() {
        JeremyUltrasoundSettings defaults = defaults();
        return new JeremyUltrasoundSettings(
            false, defaults.range(), defaults.chargeTicks(), defaults.cooldownTicks(),
            defaults.damageMultiplierVsMelee(), defaults.knockback(), defaults.wallPenetration()
        );
    }
}

record JeremyCelebrationSettings(boolean enabled, int durationTicks, float rotationDegreesPerTick) {
    static JeremyCelebrationSettings defaults() {
        return new JeremyCelebrationSettings(true, 80, 25.0f);
    }
}

record JeremyRepositionSettings(double distance, int delayTicks) {
    static JeremyRepositionSettings defaults() {
        return new JeremyRepositionSettings(128.0, 100);
    }
}

record JeremySoundSettings(
    boolean enabled,
    String spawn,
    String ultrasoundCharge,
    String ultrasoundFire,
    String celebration,
    float volume,
    float pitch
) {
    static JeremySoundSettings defaults() {
        return new JeremySoundSettings(
            true,
            "entity.zombie.converted_to_drowned",
            "entity.warden.sonic_charge",
            "entity.warden.sonic_boom",
            "entity.zombie_villager.cure",
            1.0f,
            0.9f
        );
    }
}
