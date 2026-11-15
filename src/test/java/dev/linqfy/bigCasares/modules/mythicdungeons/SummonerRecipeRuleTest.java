package dev.linqfy.bigCasares.modules.mythicdungeons;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SummonerRecipeRuleTest {

    private static List<String> fragments(MythicCulture culture, int count) {
        return Collections.nCopies(count, culture.fragmentItemId().orElseThrow());
    }

    @Test
    void fourFragmentsOfTheSameBossMakeItsSummoner() {
        for (MythicCulture culture : MythicCulture.developedCultures()) {
            assertEquals(Optional.of(culture), SummonerRecipeRule.result(fragments(culture, 4)));
        }
    }

    @Test
    void mixesAndWrongCountsAreRefused() {
        assertTrue(SummonerRecipeRule.result(fragments(MythicCulture.GREEK, 3)).isEmpty());
        assertTrue(SummonerRecipeRule.result(fragments(MythicCulture.GREEK, 5)).isEmpty());
        assertTrue(SummonerRecipeRule.result(List.of("hate_fragment_greek", "hate_fragment_greek",
            "hate_fragment_greek", "hate_fragment_norse")).isEmpty());
        assertTrue(SummonerRecipeRule.result(List.of()).isEmpty());
        assertTrue(SummonerRecipeRule.result(null).isEmpty());
    }

    @Test
    void plainVanillaItemsNeverCount() {
        // a plain echo shard (the fragments' base material) has no catalog id
        assertTrue(SummonerRecipeRule.result(Arrays.asList(null, null, null, null)).isEmpty());
        assertTrue(SummonerRecipeRule.result(Arrays.asList("hate_fragment_egyptian", "hate_fragment_egyptian",
            "hate_fragment_egyptian", null)).isEmpty());
        assertTrue(SummonerRecipeRule.result(List.of("echo_shard", "echo_shard", "echo_shard", "echo_shard"))
            .isEmpty());
        assertTrue(SummonerRecipeRule.result(List.of("boss_summoner_medieval", "hate_fragment_medieval",
            "hate_fragment_medieval", "hate_fragment_medieval")).isEmpty());
        // the postponed cultures have no fragments at all
        assertTrue(SummonerRecipeRule.result(Collections.nCopies(4, "hate_fragment_japanese")).isEmpty());
    }

    @Test
    void recognisesTheModuleItems() {
        assertTrue(SummonerRecipeRule.isMythicItem("hate_fragment_greek"));
        assertTrue(SummonerRecipeRule.isMythicItem("boss_summoner_mesopotamian"));
        assertFalse(SummonerRecipeRule.isMythicItem("hate_fragment_japanese"));
        assertFalse(SummonerRecipeRule.isMythicItem("keep_inventory_totem"));
        assertFalse(SummonerRecipeRule.isMythicItem(null));
    }
}
