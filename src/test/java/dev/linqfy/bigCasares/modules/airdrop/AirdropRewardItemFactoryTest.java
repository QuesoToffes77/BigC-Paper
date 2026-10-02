package dev.linqfy.bigCasares.modules.airdrop;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class AirdropRewardItemFactoryTest {

    @Test
    void rareItemsAirdropUsesPluginItemFactoriesWithoutRebuildingTheStack() {
        AtomicReference<String> requestedId = new AtomicReference<>();
        AtomicInteger requestedAmount = new AtomicInteger();
        ItemStack canonicalFactoryResult = new ItemStack(Material.FISHING_ROD);
        AirdropRewardItemFactory factory = new AirdropRewardItemFactory((id, amount) -> {
            requestedId.set(id);
            requestedAmount.set(amount);
            return canonicalFactoryResult;
        }, Map.of());

        ItemStack result = factory.create(AirdropReward.customItem(
            "grappling_hook_6", 1, AirdropQuality.GHISTIC));

        assertSame(canonicalFactoryResult, result);
        assertEquals("grappling_hook_6", requestedId.get());
        assertEquals(1, requestedAmount.get());
    }
}
