package dev.linqfy.bigCasares.modules.missions;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MissionCatalogLoaderTest {

    @Test
    void loadsEnabledMissionDefinitionsFromConfig() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("mission-system.missions.daily-bed.scope", "daily");
        config.set("mission-system.missions.daily-bed.title", "Salto de cama");
        config.set("mission-system.missions.daily-bed.description", "Agachate 5 veces arriba de una cama ocupada.");
        config.set("mission-system.missions.daily-bed.type", "CROUCH_ON_SLEEPING_BED");
        config.set("mission-system.missions.daily-bed.goal", 5);
        config.set("mission-system.missions.daily-bed.reward", 250.0);
        config.set("mission-system.missions.daily-bed.weight", 1);
        config.set("mission-system.missions.daily-bed.enabled", true);

        MissionCatalog catalog = new MissionCatalogLoader().load(config);

        assertEquals(1, catalog.byScope(MissionScope.DAILY).size());
        assertEquals("Salto de cama", catalog.byScope(MissionScope.DAILY).getFirst().title());
    }
}
