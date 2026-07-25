package dev.linqfy.bigCasares.modules.endevent;

import org.bukkit.configuration.ConfigurationSection;

import java.time.LocalDate;
import java.time.ZoneId;

public record EndEventSettings(
    LocalDate eventDate,
    ZoneId zone,
    String overworldName,
    int minimumPlayers,
    int strongholdSearchRadiusChunks,
    int finalBanDelaySeconds
) {
    private static final String PREFIX = "end-event-system.";

    public EndEventSettings {
        if (eventDate == null || zone == null) {
            throw new IllegalArgumentException("Event date and timezone are required");
        }
        if (overworldName == null || overworldName.isBlank()) {
            throw new IllegalArgumentException("Overworld name is required");
        }
        if (minimumPlayers <= 0) {
            throw new IllegalArgumentException("minimum-players must be greater than zero");
        }
        if (strongholdSearchRadiusChunks <= 0) {
            throw new IllegalArgumentException("stronghold-search-radius-chunks must be greater than zero");
        }
        if (finalBanDelaySeconds < 0) {
            throw new IllegalArgumentException("final-ban-delay-seconds cannot be negative");
        }
    }

    public static EndEventSettings load(ConfigurationSection config) {
        return new EndEventSettings(
            LocalDate.parse(config.getString(PREFIX + "event-date", "2026-07-25")),
            ZoneId.of(config.getString(PREFIX + "timezone", "America/Argentina/Buenos_Aires")),
            config.getString(PREFIX + "overworld", "world"),
            config.getInt(PREFIX + "minimum-players", 5),
            config.getInt(PREFIX + "stronghold-search-radius-chunks", 512),
            config.getInt(PREFIX + "final-ban-delay-seconds", 60)
        );
    }

    public EndEventSchedule schedule() {
        return new EndEventSchedule(eventDate, zone);
    }
}
