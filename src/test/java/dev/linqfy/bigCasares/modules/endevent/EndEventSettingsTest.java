package dev.linqfy.bigCasares.modules.endevent;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EndEventSettingsTest {

    @Test
    void loadsOneShotDefaultsFromEmptyConfiguration() {
        EndEventSettings settings = EndEventSettings.load(new YamlConfiguration());

        assertEquals(LocalDate.of(2026, 7, 25), settings.eventDate());
        assertEquals(ZoneId.of("America/Argentina/Buenos_Aires"), settings.zone());
        assertEquals("world", settings.overworldName());
        assertEquals(5, settings.minimumPlayers());
        assertEquals(512, settings.strongholdSearchRadiusChunks());
    }

    @Test
    void rejectsInvalidMinimumPlayers() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("end-event-system.minimum-players", 0);

        assertThrows(IllegalArgumentException.class, () -> EndEventSettings.load(config));
    }
}
