package dev.linqfy.bigCasares.modules.mythicdungeons;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La tabla fija de culturas: otros trabajos (datapack, modelos, encuentros) dependen de estos ids. */
class MythicCultureTest {

    /** Las desarrolladas: id|jefe|id corto|nombre del jefe -> mobs. */
    private static final Map<String, List<String>> DEVELOPED = new LinkedHashMap<>();
    /** Solo mazmorra por ahora. */
    private static final List<String> DUNGEON_ONLY = List.of("japanese", "mesoamerican", "arabian", "andean");

    static {
        DEVELOPED.put("greek|myth-zeus|zeus|Zeus", List.of("minotaur", "cerberus", "erinys"));
        DEVELOPED.put("norse|myth-odin|odin|Odín", List.of("draugr", "dwarf", "nidhogg", "hel"));
        DEVELOPED.put("medieval|myth-diablo|diablo|Diablo", List.of("treasure_dragon", "kobold", "goblin", "ghost"));
        DEVELOPED.put("egyptian|myth-horus|horus|Horus", List.of("ammit", "apophis", "mummy"));
        DEVELOPED.put("mesopotamian|myth-enlil|enlil|Enlil", List.of("ereshkigal", "galla"));
    }

    @Test
    void theDevelopedCulturesMatchTheFixedIds() {
        assertEquals(DEVELOPED.size(), MythicCulture.developedCultures().size());
        int index = 0;
        for (Map.Entry<String, List<String>> row : DEVELOPED.entrySet()) {
            String[] columns = row.getKey().split("\\|");
            MythicCulture culture = MythicCulture.values()[index++];
            assertTrue(culture.developed());
            assertEquals(columns[0], culture.id());
            assertEquals(columns[1], culture.requireBoss().id());
            assertEquals("bigcasares_myth_" + columns[2], culture.requireBoss().modelKey());
            assertEquals(columns[3], culture.requireBoss().displayName());
            assertEquals(row.getValue(), culture.mobIds());
            assertEquals("hate_fragment_" + columns[0], culture.fragmentItemId().orElseThrow());
            assertEquals("boss_summoner_" + columns[0], culture.summonerItemId().orElseThrow());
            assertEquals("bigc_myth:" + columns[0], culture.structureKey());
            assertEquals(culture, MythicCulture.byBossId(columns[1]).orElseThrow());
            assertEquals(culture, MythicCulture.byFragmentItemId("hate_fragment_" + columns[0]).orElseThrow());
            assertEquals(culture, MythicCulture.bySummonerItemId("boss_summoner_" + columns[0]).orElseThrow());
        }
    }

    @Test
    void theOtherFourCulturesOnlyHaveTheirDungeon() {
        assertEquals(9, MythicCulture.values().length);
        for (String id : DUNGEON_ONLY) {
            MythicCulture culture = MythicCulture.byId(id).orElseThrow();
            assertFalse(culture.developed(), id);
            assertTrue(culture.boss().isEmpty(), id);
            assertTrue(culture.mobIds().isEmpty(), id);
            assertTrue(culture.fragmentItemId().isEmpty(), id);
            assertTrue(culture.summonerItemId().isEmpty(), id);
            assertEquals("bigc_myth:" + id, culture.structureKey());
            assertEquals(culture, MythicCulture.byStructureKey(culture.structureKey()).orElseThrow());
            assertThrows(IllegalStateException.class, culture::requireBoss);
            assertTrue(culture.notAvailableMessage().contains("todavía no está disponible"));
        }
        assertTrue(MythicCulture.byFragmentItemId("hate_fragment_japanese").isEmpty());
        assertTrue(MythicCulture.bySummonerItemId("boss_summoner_andean").isEmpty());
    }

    @Test
    void everyCultureHasAUniqueDungeonAndEveryIdIsUnique() {
        Set<String> seen = new HashSet<>();
        for (MythicCulture culture : MythicCulture.values()) {
            assertTrue(seen.add(culture.id()));
            assertTrue(seen.add(culture.structureKey()));
            assertTrue(culture.dungeonName().startsWith("la mazmorra "));
            culture.boss().ifPresent(boss -> {
                assertTrue(seen.add(boss.id()));
                assertTrue(seen.add(boss.modelKey()));
            });
            culture.fragmentItemId().ifPresent(id -> assertTrue(seen.add(id)));
            culture.summonerItemId().ifPresent(id -> assertTrue(seen.add(id)));
            for (String mob : culture.mobIds()) {
                assertTrue(seen.add(mob), "duplicated mob " + mob);
                assertTrue(seen.add(MythicCulture.mobModelKey(mob)), "duplicated model " + mob);
            }
        }
        assertEquals(16, MythicCulture.developedCultures().stream().mapToInt(c -> c.mobIds().size()).sum());
    }

    @Test
    void unknownLookupsAreEmpty() {
        assertTrue(MythicCulture.byId("roman").isEmpty());
        assertTrue(MythicCulture.byId(null).isEmpty());
        assertTrue(MythicCulture.byStructureKey("minecraft:greek").isEmpty());
        assertTrue(MythicCulture.byFragmentItemId("echo_shard").isEmpty());
        assertTrue(MythicCulture.byFragmentItemId(null).isEmpty());
        assertEquals(MythicCulture.GREEK, MythicCulture.byId(" GREEK ").orElseThrow());
    }
}
