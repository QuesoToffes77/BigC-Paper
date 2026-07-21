package dev.linqfy.bigCasares.modules.mobscaling;

import org.bukkit.configuration.file.FileConfiguration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public record MobScalingSettings(
        boolean enabled,
        LocalDate startDate,
        double formulaA,
        double formulaB,
        double formulaC,
        double baseMutationChance,
        double mutationChancePerDay,
        double maxMutationChance
) {
    private static final String PREFIX = "mob-scaling.";

    public static MobScalingSettings fromConfig(FileConfiguration config) {
        boolean enabled = config.getBoolean(PREFIX + "enabled", true);
        String dateStr = config.getString(PREFIX + "start-date", "2026-07-17");
        LocalDate startDate;
        try {
            startDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            startDate = LocalDate.now();
        }
        
        double a = config.getDouble(PREFIX + "formula.a", 0.05);
        double b = config.getDouble(PREFIX + "formula.b", 1.1);
        double c = config.getDouble(PREFIX + "formula.c", 1.0);
        
        double baseChance = config.getDouble(PREFIX + "mutations.base-chance", 0.05);
        double perDay = config.getDouble(PREFIX + "mutations.chance-per-day", 0.02);
        double maxChance = config.getDouble(PREFIX + "mutations.max-chance", 0.35);

        return new MobScalingSettings(enabled, startDate, a, b, c, baseChance, perDay, maxChance);
    }
}
