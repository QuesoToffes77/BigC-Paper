package dev.linqfy.bigCasares.modules.mythicdungeons;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MythicMobCatalogTest {

    private static final Set<String> ELITES = Set.of("cerberus", "nidhogg", "hel", "treasure_dragon", "ammit",
        "apophis", "ereshkigal");

    private final MythicMobCatalog catalog = MythicMobCatalog.defaults();

    @Test
    void everyCultureHasExactlyItsMobs() {
        assertEquals(16, catalog.all().size());
        for (MythicCulture culture : MythicCulture.values()) {
            List<MythicMobType> mobs = catalog.ofCulture(culture);
            assertEquals(culture.mobIds(), mobs.stream().map(MythicMobType::id).toList());
            mobs.forEach(mob -> assertEquals(culture, mob.culture()));
        }
    }

    @Test
    void statsWeightsAndModelsAreSane() {
        for (MythicMobType mob : catalog.all()) {
            assertTrue(mob.weight() > 0.0, mob.id());
            assertTrue(mob.health() > 0.0 && mob.speed() > 0.0 && mob.scale() > 0.0, mob.id());
            assertTrue(mob.fragmentChance() > 0.0 && mob.fragmentChance() <= 1.0, mob.id());
            assertEquals("bigcasares_myth_" + mob.id(), mob.modelKey());
            assertTrue(mob.baseEntity().isAlive(), mob.id());
            assertFalse(mob.displayName().isBlank(), mob.id());
            if (mob.behaviour() == MythicBehaviour.SUMMONER) {
                MythicMobType summoned = catalog.require(mob.summonId());
                assertEquals(mob.culture(), summoned.culture(), mob.id());
            }
            if (mob.behaviour() == MythicBehaviour.RANGED) {
                assertTrue(mob.projectile() != MythicProjectile.NONE, mob.id());
            }
        }
    }

    @Test
    void elitesAreTougherAndRarerThanTheCommonsOfTheirDungeon() {
        for (MythicMobType mob : catalog.all()) {
            assertEquals(ELITES.contains(mob.id()), mob.elite(), mob.id());
            if (mob.elite()) {
                assertTrue(mob.health() >= 80 && mob.health() <= 200, mob.id());
            } else {
                assertTrue(mob.health() >= 20 && mob.health() <= 40, mob.id());
            }
        }
        for (MythicCulture culture : MythicCulture.values()) {
            double maxEliteWeight = catalog.ofCulture(culture).stream().filter(MythicMobType::elite)
                .mapToDouble(MythicMobType::weight).max().orElse(0.0);
            double minCommonWeight = catalog.ofCulture(culture).stream().filter(mob -> !mob.elite())
                .mapToDouble(MythicMobType::weight).min().orElse(Double.MAX_VALUE);
            assertTrue(maxEliteWeight < minCommonWeight, culture.id());
        }
    }

    @Test
    void ymlOverridesReplaceOnlyTheTunableNumbers() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("mobs.minotaur.health", 55.0);
        yaml.set("mobs.minotaur.damage", 9.0);
        yaml.set("mobs.minotaur.speed", 0.3);
        yaml.set("mobs.minotaur.fragment-chance", 0.5);
        yaml.set("mobs.minotaur.weight", 3.0);
        yaml.set("mobs.ghost.health", -4.0);
        yaml.set("mobs.ghost.fragment-chance", 2.0);
        yaml.set("mobs.unicorn.health", 10.0);
        List<String> unknown = new ArrayList<>();

        MythicMobCatalog tuned = catalog.withOverrides(yaml.getConfigurationSection("mobs"), unknown::add);

        MythicMobType minotaur = tuned.require("minotaur");
        assertEquals(55.0, minotaur.health());
        assertEquals(9.0, minotaur.damage());
        assertEquals(0.3, minotaur.speed());
        assertEquals(0.5, minotaur.fragmentChance());
        assertEquals(3.0, minotaur.weight());
        assertEquals(catalog.require("minotaur").baseEntity(), minotaur.baseEntity());
        MythicMobType ghost = tuned.require("ghost");
        assertEquals(catalog.require("ghost").health(), ghost.health());
        assertEquals(catalog.require("ghost").fragmentChance(), ghost.fragmentChance());
        assertEquals(List.of("unicorn"), unknown);
        assertEquals(catalog.ids(), tuned.ids());
    }
}
