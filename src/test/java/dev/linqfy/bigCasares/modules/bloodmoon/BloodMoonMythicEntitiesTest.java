package dev.linqfy.bigCasares.modules.bloodmoon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BloodMoonMythicEntitiesTest {

    @Test
    void mythicMobsMinionsAndBossesAreLeftAlone() {
        assertTrue(BukkitBloodMoonMobModifier.isMythicDungeonEntity("mythic-dungeons"));
        assertTrue(BukkitBloodMoonMobModifier.isMythicDungeonEntity("myth-zeus"));
        assertFalse(BukkitBloodMoonMobModifier.isMythicDungeonEntity(null));
        assertFalse(BukkitBloodMoonMobModifier.isMythicDungeonEntity("mutango"));
    }
}
