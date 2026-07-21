package dev.linqfy.bigCasares.modules.pveboss;

import dev.linqfy.bigCasares.items.CustomItemRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaperSahurRewardDeliveryTest {

    @Test
    void resolvesVanillaPlacementRewardsWithTheirExactAmounts() {
        var apples = PaperSahurRewardDelivery.plan(new BossReward("ENCHANTED_GOLDEN_APPLE", 3));
        var shards = PaperSahurRewardDelivery.plan(new BossReward("ECHO_SHARD", 3));
        assertEquals(false, apples.customItem());
        assertEquals("ENCHANTED_GOLDEN_APPLE", apples.itemId());
        assertEquals(3, apples.amount());
        assertEquals(false, shards.customItem());
        assertEquals("ECHO_SHARD", shards.itemId());
        assertEquals(3, shards.amount());
    }

    @Test
    void resolvesFirstPlaceThroughTheActiveCustomItemCatalog() {
        var plan = PaperSahurRewardDelivery.plan(new BossReward("sahurs_bat", 1));
        assertEquals(true, plan.customItem());
        assertEquals("sahurs_bat", plan.itemId());
        assertEquals(1, plan.amount());
    }
}
